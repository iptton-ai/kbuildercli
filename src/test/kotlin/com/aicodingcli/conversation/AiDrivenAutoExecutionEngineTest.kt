package com.aicodingcli.conversation

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AiDrivenAutoExecutionEngineTest {
    
    @TempDir
    lateinit var tempDir: File
    
    private lateinit var conversationStateManager: ConversationStateManager
    private lateinit var toolExecutor: ToolExecutor
    private lateinit var aiPromptEngine: AiPromptEngine
    private lateinit var autoExecutionEngine: AiDrivenAutoExecutionEngine
    
    @BeforeEach
    fun setUp() {
        conversationStateManager = ConversationStateManager(tempDir.absolutePath)
        toolExecutor = DefaultToolExecutor(tempDir.absolutePath)
        
        // Use scenario-based mock for predictable testing
        val mockAiService = ScenarioMockAiService(TestScenario.SIMPLE_SUCCESS)
        aiPromptEngine = DefaultAiPromptEngine(mockAiService)
        
        autoExecutionEngine = AiDrivenAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            aiPromptEngine = aiPromptEngine,
            toolExecutor = toolExecutor,
            strategy = AiExecutionStrategy(
                maxRounds = 5,
                confidenceThreshold = 0.7,
                enableSafetyChecks = true
            )
        )
    }
    
    @Test
    fun `should execute simple requirement successfully`() = runTest {
        // Arrange
        val requirement = "Create a User class with name and email properties"
        
        // Act
        val result = autoExecutionEngine.executeConversation(requirement)
        
        // Assert
        assertTrue(result.success, "Execution should be successful. Error: ${result.error}")
        assertNotNull(result.sessionId)
        assertEquals(ConversationStatus.COMPLETED, result.finalStatus)
        assertTrue(result.executedSteps.isNotEmpty())
        assertTrue(result.executionRounds > 0)
        assertTrue(result.executionTime.isPositive())
        assertNotNull(result.summary)
        
        // Verify file was created
        val userFile = File(tempDir, "User.kt")
        assertTrue(userFile.exists())
        assertTrue(userFile.readText().contains("User"))
    }
    
    @Test
    fun `should handle empty requirement gracefully`() = runTest {
        // Arrange
        val requirement = ""
        
        // Act
        val result = autoExecutionEngine.executeConversation(requirement)
        
        // Assert
        assertTrue(result.success)
        assertEquals("empty-session", result.sessionId)
        assertEquals(ConversationStatus.COMPLETED, result.finalStatus)
        assertTrue(result.executedSteps.isEmpty())
        assertEquals(0, result.executionRounds)
        assertEquals("Empty requirement - no action taken", result.summary)
    }
    
    @Test
    fun `should execute multi-step requirement`() = runTest {
        // Arrange
        val mockAiService = ScenarioMockAiService(TestScenario.MULTI_STEP)
        val engine = AiDrivenAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            aiPromptEngine = DefaultAiPromptEngine(mockAiService),
            toolExecutor = toolExecutor,
            strategy = AiExecutionStrategy(maxRounds = 10)
        )
        
        val requirement = "Create a User class with validation"
        
        // Act
        val result = engine.executeConversation(requirement)
        
        // Assert
        assertTrue(result.success)
        assertEquals(ConversationStatus.COMPLETED, result.finalStatus)
        assertTrue(result.executedSteps.size >= 2) // Should have multiple steps
        
        // Verify file was created and modified
        val userFile = File(tempDir, "User.kt")
        assertTrue(userFile.exists())
        val content = userFile.readText()
        assertTrue(content.contains("User"))
    }
    
    @Test
    fun `should require user confirmation for dangerous operations`() = runTest {
        // Arrange
        val mockAiService = ScenarioMockAiService(TestScenario.REQUIRES_CONFIRMATION)
        val engine = AiDrivenAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            aiPromptEngine = DefaultAiPromptEngine(mockAiService),
            toolExecutor = toolExecutor,
            strategy = AiExecutionStrategy(
                maxRounds = 5,
                requireUserConfirmation = listOf("remove-files")
            )
        )
        
        val requirement = "Clean up old files"
        
        // Act
        val result = engine.executeConversation(requirement)
        
        // Assert
        assertTrue(result.success)
        assertEquals(ConversationStatus.WAITING_USER, result.finalStatus)
        assertTrue(result.summary!!.contains("confirmation required"))
    }
    
    @Test
    fun `should stop at maximum execution rounds`() = runTest {
        // Arrange
        val mockAiService = ScenarioMockAiService(TestScenario.MAX_ROUNDS)
        val engine = AiDrivenAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            aiPromptEngine = DefaultAiPromptEngine(mockAiService),
            toolExecutor = toolExecutor,
            strategy = AiExecutionStrategy(maxRounds = 3)
        )
        
        val requirement = "Perform many operations"
        
        // Act
        val result = engine.executeConversation(requirement)
        
        // Assert
        assertTrue(result.success)
        assertEquals(ConversationStatus.WAITING_USER, result.finalStatus)
        assertEquals(3, result.executionRounds)
        assertTrue(result.summary!!.contains("maximum execution rounds"))
    }
    
    @Test
    fun `should handle AI service failures gracefully`() = runTest {
        // Arrange
        val mockAiService = ScenarioMockAiService(TestScenario.AI_FAILURE)
        val engine = AiDrivenAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            aiPromptEngine = DefaultAiPromptEngine(mockAiService),
            toolExecutor = toolExecutor
        )
        
        val requirement = "Create a User class"
        
        // Act
        val result = engine.executeConversation(requirement)
        
        // Assert
        assertFalse(result.success)
        assertEquals(ConversationStatus.FAILED, result.finalStatus)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("AI decision failed"))
    }
    
    @Test
    fun `should continue existing conversation`() = runTest {
        // Arrange
        // Create a session that will require user confirmation (so it stops and can be continued)
        val mockAiService = ScenarioMockAiService(TestScenario.REQUIRES_CONFIRMATION)
        val engine = AiDrivenAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            aiPromptEngine = DefaultAiPromptEngine(mockAiService),
            toolExecutor = toolExecutor,
            strategy = AiExecutionStrategy(
                maxRounds = 5,
                requireUserConfirmation = listOf("remove-files")
            )
        )

        val requirement = "Clean up old files"
        val initialResult = engine.executeConversation(requirement)
        assertTrue(initialResult.success)
        assertEquals(ConversationStatus.WAITING_USER, initialResult.finalStatus)

        val sessionId = initialResult.sessionId!!

        // Act - Continue the conversation (should still be waiting for user)
        val continueResult = engine.continueConversation(sessionId)

        // Assert
        assertTrue(continueResult.success)
        assertEquals(sessionId, continueResult.sessionId)
        assertEquals(ConversationStatus.WAITING_USER, continueResult.finalStatus)
    }
    
    @Test
    fun `should fail to continue non-existent conversation`() = runTest {
        // Arrange
        val nonExistentSessionId = "non-existent-session"
        
        // Act
        val result = autoExecutionEngine.continueConversation(nonExistentSessionId)
        
        // Assert
        assertFalse(result.success)
        assertEquals(ConversationStatus.FAILED, result.finalStatus)
        assertTrue(result.error!!.contains("Session not found"))
    }
    
    @Test
    fun `should execute single step successfully`() = runTest {
        // Arrange
        val toolCall = ToolCall(
            toolName = "save-file",
            parameters = mapOf(
                "path" to "test.kt",
                "file_content" to "class Test"
            )
        )
        val executionStep = ExecutionStep(
            taskId = "test-task",
            toolCall = toolCall,
            result = ToolResult.success("placeholder"),
            duration = kotlin.time.Duration.parse("100ms")
        )
        
        // Act
        val result = autoExecutionEngine.executeStep(executionStep)
        
        // Assert
        assertTrue(result.success)
        assertNotNull(result.toolResult)
        assertTrue(result.toolResult!!.success)
        
        // Verify file was created
        val testFile = File(tempDir, "test.kt")
        assertTrue(testFile.exists())
        assertEquals("class Test", testFile.readText())
    }
    
    @Test
    fun `should validate safety checks for dangerous operations`() = runTest {
        // Arrange
        val mockAiService = MockAiService.withActionResponse("""
            {
                "action": "EXECUTE_TOOL",
                "toolName": "launch-process",
                "parameters": {
                    "command": "rm -rf /"
                },
                "reasoning": "Dangerous command",
                "confidence": 0.9
            }
        """.trimIndent())
        
        val engine = AiDrivenAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            aiPromptEngine = DefaultAiPromptEngine(mockAiService),
            toolExecutor = toolExecutor,
            strategy = AiExecutionStrategy(enableSafetyChecks = true)
        )
        
        val requirement = "Clean everything"
        
        // Act
        val result = engine.executeConversation(requirement)
        
        // Assert
        assertFalse(result.success)
        assertEquals(ConversationStatus.FAILED, result.finalStatus)
        assertTrue(result.error!!.contains("Safety check failed"))
    }
    
    @Test
    fun `should respect tool restrictions in strategy`() = runTest {
        // Arrange
        val mockAiService = MockAiService.withActionResponse("""
            {
                "action": "EXECUTE_TOOL",
                "toolName": "web-search",
                "parameters": {
                    "query": "test query"
                },
                "reasoning": "Search for information",
                "confidence": 0.9
            }
        """.trimIndent())
        
        val engine = AiDrivenAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            aiPromptEngine = DefaultAiPromptEngine(mockAiService),
            toolExecutor = toolExecutor,
            strategy = AiExecutionStrategy(allowNetworkOperations = false)
        )
        
        val requirement = "Search for information"
        
        // Act
        val result = engine.executeConversation(requirement)
        
        // Assert
        assertFalse(result.success)
        assertEquals(ConversationStatus.FAILED, result.finalStatus)
        assertTrue(result.error!!.contains("not allowed by current strategy"))
    }
    
    @Test
    fun `should update session state during execution`() = runTest {
        // Arrange
        val requirement = "Create a User class"
        
        // Act
        val result = autoExecutionEngine.executeConversation(requirement)
        
        // Assert
        assertTrue(result.success)
        
        // Verify session was created and updated
        val session = conversationStateManager.getSession(result.sessionId!!)
        assertNotNull(session)
        assertEquals(ConversationStatus.COMPLETED, session!!.state.status)
        assertTrue(session.executionHistory.isNotEmpty())
        assertTrue(session.state.executionRound > 0)
    }
}
