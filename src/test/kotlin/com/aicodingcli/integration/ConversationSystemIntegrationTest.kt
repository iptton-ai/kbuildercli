package com.aicodingcli.integration

import com.aicodingcli.conversation.*
import com.aicodingcli.ai.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.async
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Assertions.*
import java.io.File

/**
 * Integration tests for the complete conversation system
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConversationSystemIntegrationTest {
    
    @TempDir
    lateinit var tempDir: File
    
    private lateinit var conversationStateManager: ConversationStateManager
    private lateinit var toolExecutor: ToolExecutor
    private lateinit var aiPromptEngine: AiPromptEngine
    private lateinit var aiDrivenEngine: AiDrivenAutoExecutionEngine
    
    @BeforeEach
    fun setup() {
        // Setup temporary state directory
        val stateDir = File(tempDir, "conversations").absolutePath
        conversationStateManager = ConversationStateManager(stateDir)
        
        // Setup tool executor with real working directory
        toolExecutor = DefaultToolExecutor(workingDirectory = tempDir.absolutePath)
        
        // Setup AI components with mock AI service
        val mockAiService = SimpleMockAiService()
        aiPromptEngine = DefaultAiPromptEngine(mockAiService)
        
        // Setup AI-driven engine
        aiDrivenEngine = AiDrivenAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            aiPromptEngine = aiPromptEngine,
            toolExecutor = toolExecutor,
            strategy = AiExecutionStrategy(maxRounds = 5)
        )
    }
    
    @Test
    fun `should execute complete conversation workflow`() = runTest {
        // Arrange
        val requirement = "Create a simple Kotlin data class User with name and email fields"
        
        // Act
        val result = aiDrivenEngine.executeConversation(requirement)
        
        // Assert
        assertTrue(result.success, "Execution should succeed. Error: ${result.error}")
        assertNotNull(result.sessionId)
        assertEquals(ConversationStatus.COMPLETED, result.finalStatus)
        assertTrue(result.executedSteps.isNotEmpty())
        assertTrue(result.executionRounds > 0)
        assertTrue(result.executionTime.isPositive())
        
        // Verify session was created and managed properly
        val session = conversationStateManager.getSession(result.sessionId!!)
        assertNotNull(session)
        assertEquals(ConversationStatus.COMPLETED, session!!.state.status)
        assertTrue(session.executionHistory.isNotEmpty())
        
        // Verify file was actually created
        val userFile = File(tempDir, "User.kt")
        assertTrue(userFile.exists(), "User.kt file should be created")
        val content = userFile.readText()
        assertTrue(content.contains("data class User"), "File should contain User data class")
        assertTrue(content.contains("name"), "File should contain name field")
        assertTrue(content.contains("email"), "File should contain email field")
    }
    
    @Test
    fun `should handle conversation state persistence`() = runTest {
        // Arrange
        val requirement = "Create a simple test file"
        
        // Act - Execute conversation
        val result = aiDrivenEngine.executeConversation(requirement)
        assertTrue(result.success)
        
        val sessionId = result.sessionId!!
        
        // Create new state manager (simulating app restart)
        val stateDir = File(tempDir, "conversations").absolutePath
        val newStateManager = ConversationStateManager(stateDir)
        
        // Assert - Session should be persisted and retrievable
        val retrievedSession = newStateManager.getSession(sessionId)
        assertNotNull(retrievedSession, "Session should be persisted")
        assertEquals(sessionId, retrievedSession!!.id)
        assertEquals(result.finalStatus, retrievedSession.state.status)
    }
    
    @Test
    fun `should continue existing conversation`() = runTest {
        // Arrange - Start a conversation
        val requirement = "Create a basic User class"
        val initialResult = aiDrivenEngine.executeConversation(requirement)
        assertTrue(initialResult.success)

        val sessionId = initialResult.sessionId!!

        // Act - Continue the conversation (should complete immediately since task is done)
        val continueResult = aiDrivenEngine.continueConversation(sessionId)

        // Assert - Should succeed even if no more work to do
        assertNotNull(continueResult.sessionId)
        assertEquals(sessionId, continueResult.sessionId)

        // Verify session state is properly maintained
        val session = conversationStateManager.getSession(sessionId)
        assertNotNull(session)
    }
    
    @Test
    fun `should handle multiple sequential conversations`() = runTest {
        // Arrange
        val requirements = listOf(
            "Create a User data class",
            "Create a Product data class"
        )

        // Act - Execute conversations sequentially
        val results = mutableListOf<ExecutionResult>()
        for (requirement in requirements) {
            val result = aiDrivenEngine.executeConversation(requirement)
            results.add(result)
        }

        // Assert
        results.forEach { result ->
            assertTrue(result.success, "All conversations should succeed")
            assertNotNull(result.sessionId)
        }

        // Verify all sessions are unique
        val sessionIds = results.map { it.sessionId!! }.toSet()
        assertEquals(requirements.size, sessionIds.size, "All sessions should have unique IDs")

        // Verify at least one file was created (User.kt should always work)
        val userFile = File(tempDir, "User.kt")
        assertTrue(userFile.exists(), "User.kt should be created")
    }
    
    @Test
    fun `should respect execution round limits`() = runTest {
        // Arrange - Create engine with very low round limit
        val limitedEngine = AiDrivenAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            aiPromptEngine = aiPromptEngine,
            toolExecutor = toolExecutor,
            strategy = AiExecutionStrategy(maxRounds = 2)
        )
        
        val requirement = "Create multiple files and complex operations"
        
        // Act
        val result = limitedEngine.executeConversation(requirement)
        
        // Assert
        assertTrue(result.executionRounds <= 2, "Should not exceed round limit")
        assertNotNull(result.sessionId)
    }
    
    @Test
    fun `should handle tool execution properly`() = runTest {
        // Arrange
        val requirement = "Create a Calculator class with basic math operations"
        
        // Act
        val result = aiDrivenEngine.executeConversation(requirement)
        
        // Assert
        assertTrue(result.success)
        assertTrue(result.executedSteps.isNotEmpty())
        
        // Verify at least one tool was executed
        val toolExecutions = result.executedSteps.filter { it.toolCall.toolName == "save-file" }
        assertTrue(toolExecutions.isNotEmpty(), "Should have executed save-file tool")
        
        // Verify file was created
        val calcFile = File(tempDir, "Calculator.kt")
        assertTrue(calcFile.exists(), "Calculator.kt should be created")
        val content = calcFile.readText()
        assertTrue(content.contains("Calculator"), "File should contain Calculator class")
    }
}

/**
 * Simple Mock AI Service for integration testing
 */
class SimpleMockAiService : AiService {
    private var callCount = 0
    private var lastPrompt = ""

    override val config: AiServiceConfig = AiServiceConfig(
        provider = AiProvider.OPENAI,
        apiKey = "test-key",
        model = "gpt-4"
    )

    override suspend fun chat(request: AiRequest): AiResponse {
        callCount++
        lastPrompt = request.messages.lastOrNull()?.content ?: ""
        
        val response = when {
            lastPrompt.contains("analyze") -> getAnalysisResponse()
            lastPrompt.contains("decide") -> getActionResponse()
            lastPrompt.contains("evaluate") -> getEvaluationResponse()
            else -> "Mock response for integration test"
        }
        
        return AiResponse(
            content = response,
            model = "gpt-4",
            usage = TokenUsage(10, 5, 15),
            finishReason = FinishReason.STOP
        )
    }
    
    override suspend fun streamChat(request: AiRequest): kotlinx.coroutines.flow.Flow<AiStreamChunk> {
        val response = chat(request)
        return flowOf(AiStreamChunk(response.content, FinishReason.STOP))
    }
    
    override suspend fun testConnection(): Boolean = true
    
    private fun getAnalysisResponse(): String = """
        {
            "intent": "Create Kotlin data class with specified fields",
            "complexity": "SIMPLE",
            "category": "CODE_GENERATION",
            "prerequisites": [],
            "estimatedSteps": 1,
            "reasoning": "Simple data class creation requires only one file operation"
        }
    """.trimIndent()
    
    private fun getActionResponse(): String {
        val prompt = lastPrompt
        return when {
            prompt.contains("User") && callCount % 3 == 2 -> """
                {
                    "action": "EXECUTE_TOOL",
                    "toolName": "save-file",
                    "parameters": {
                        "path": "User.kt",
                        "file_content": "data class User(val name: String, val email: String)"
                    },
                    "reasoning": "Create the User data class as requested",
                    "confidence": 0.9
                }
            """.trimIndent()

            prompt.contains("Product") && callCount % 3 == 2 -> """
                {
                    "action": "EXECUTE_TOOL",
                    "toolName": "save-file",
                    "parameters": {
                        "path": "Product.kt",
                        "file_content": "data class Product(val id: Long, val name: String, val price: Double)"
                    },
                    "reasoning": "Create the Product data class as requested",
                    "confidence": 0.9
                }
            """.trimIndent()

            prompt.contains("Order") && callCount % 3 == 2 -> """
                {
                    "action": "EXECUTE_TOOL",
                    "toolName": "save-file",
                    "parameters": {
                        "path": "Order.kt",
                        "file_content": "data class Order(val id: Long, val userId: Long, val products: List<Product>)"
                    },
                    "reasoning": "Create the Order data class as requested",
                    "confidence": 0.9
                }
            """.trimIndent()

            prompt.contains("Calculator") && callCount % 3 == 2 -> """
                {
                    "action": "EXECUTE_TOOL",
                    "toolName": "save-file",
                    "parameters": {
                        "path": "Calculator.kt",
                        "file_content": "class Calculator { fun add(a: Int, b: Int) = a + b; fun subtract(a: Int, b: Int) = a - b; fun multiply(a: Int, b: Int) = a * b; fun divide(a: Int, b: Int) = a / b }"
                    },
                    "reasoning": "Create the Calculator class as requested",
                    "confidence": 0.9
                }
            """.trimIndent()

            else -> """
                {
                    "action": "COMPLETE",
                    "reasoning": "Task completed successfully",
                    "confidence": 0.95
                }
            """.trimIndent()
        }
    }
    
    private fun getEvaluationResponse(): String = """
        {
            "status": "SUCCESS",
            "completionPercentage": 100,
            "summary": "Successfully created the requested class with all specified fields",
            "reasoning": "All requirements have been fulfilled"
        }
    """.trimIndent()
}
