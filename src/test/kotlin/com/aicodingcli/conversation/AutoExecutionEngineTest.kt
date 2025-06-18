package com.aicodingcli.conversation

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AutoExecutionEngineTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var autoExecutionEngine: AutoExecutionEngine
    private lateinit var conversationStateManager: ConversationStateManager
    private lateinit var taskDecomposer: TaskDecomposer
    private lateinit var requirementParser: RequirementParser
    private lateinit var toolExecutor: ToolExecutor

    @BeforeEach
    fun setUp() {
        conversationStateManager = ConversationStateManager(tempDir.absolutePath)
        taskDecomposer = DefaultTaskDecomposer()
        requirementParser = DefaultRequirementParser()
        toolExecutor = DefaultToolExecutor(tempDir.absolutePath)
        
        autoExecutionEngine = DefaultAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            taskDecomposer = taskDecomposer,
            requirementParser = requirementParser,
            toolExecutor = toolExecutor,
            maxExecutionRounds = 25
        )
    }

    @Test
    fun `should execute simple conversation successfully`() = runTest {
        // Arrange
        val requirement = "Create a simple User data class with name and email properties"

        // Act
        val result = autoExecutionEngine.executeConversation(requirement)

        // Assert
        assertTrue(result.success, "Execution should be successful")
        assertEquals(ConversationStatus.COMPLETED, result.finalStatus)
        assertTrue(result.executedSteps.isNotEmpty(), "Should have executed steps")
        assertTrue(result.executionRounds <= 25, "Should not exceed max rounds")
        assertNull(result.error)
        
        // Verify file was created
        val userFile = File(tempDir, "User.kt")
        assertTrue(userFile.exists())
        val content = userFile.readText()
        assertTrue(content.contains("data class User"))
        assertTrue(content.contains("name"))
        assertTrue(content.contains("email"))
    }

    @Test
    fun `should handle complex conversation with multiple tasks`() = runTest {
        // Arrange
        val requirement = "Create a REST API for user management with CRUD operations"

        // Act
        val result = autoExecutionEngine.executeConversation(requirement)

        // Assert
        assertTrue(result.success, "Execution should be successful")
        assertEquals(ConversationStatus.COMPLETED, result.finalStatus)
        assertTrue(result.executedSteps.size >= 3, "Should have model, service, controller tasks")
        assertTrue(result.executionRounds <= 25, "Should not exceed max rounds")
        
        // Verify multiple files were created
        val userFile = File(tempDir, "User.kt")
        val serviceFile = File(tempDir, "UserService.kt")
        val controllerFile = File(tempDir, "UserController.kt")
        
        assertTrue(userFile.exists())
        assertTrue(serviceFile.exists())
        assertTrue(controllerFile.exists())
    }

    @Test
    fun `should stop execution after max rounds`() = runTest {
        // Arrange
        val requirement = "Create a very complex system" // This might generate many tasks
        val engineWithLowLimit = DefaultAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            taskDecomposer = taskDecomposer,
            requirementParser = requirementParser,
            toolExecutor = toolExecutor,
            maxExecutionRounds = 3 // Very low limit
        )

        // Act
        val result = engineWithLowLimit.executeConversation(requirement)

        // Assert
        assertEquals(3, result.executionRounds)
        assertEquals(ConversationStatus.WAITING_USER, result.finalStatus)
        assertTrue(result.success, "Should still be successful, just paused")
        assertNotNull(result.sessionId) // Should have a session to continue
    }

    @Test
    fun `should handle execution errors gracefully`() = runTest {
        // Arrange
        val requirement = "Create a file with invalid path characters"
        
        // Create a mock tool executor that always fails
        val failingToolExecutor = object : ToolExecutor {
            override suspend fun execute(tool: ToolCall): ToolResult {
                return ToolResult.failure("Mock failure for testing")
            }
            override fun getSupportedTools(): List<ToolMetadata> = emptyList()
            override suspend fun validateParameters(tool: ToolCall): ValidationResult = ValidationResult.valid()
        }
        
        val engineWithFailingExecutor = DefaultAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            taskDecomposer = taskDecomposer,
            requirementParser = requirementParser,
            toolExecutor = failingToolExecutor,
            maxExecutionRounds = 25
        )

        // Act
        val result = engineWithFailingExecutor.executeConversation(requirement)

        // Assert
        assertFalse(result.success, "Should fail due to mock failure")
        assertEquals(ConversationStatus.FAILED, result.finalStatus)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("Mock failure"), "Error should contain mock failure message")
    }

    @Test
    fun `should execute individual step correctly`() = runTest {
        // Arrange
        val task = ExecutableTask(
            description = "Create test file",
            toolCalls = listOf(
                ToolCall(
                    toolName = "save-file",
                    parameters = mapOf(
                        "path" to "test.kt",
                        "file_content" to "class Test"
                    )
                )
            )
        )
        val step = ExecutionStep(
            taskId = task.id,
            toolCall = task.toolCalls.first(),
            result = ToolResult.success("placeholder"),
            duration = kotlin.time.Duration.ZERO
        )

        // Act
        val result = autoExecutionEngine.executeStep(step)

        // Assert
        assertTrue(result.success, "Step execution should be successful")
        assertNotNull(result.toolResult)
        assertTrue(result.toolResult!!.success, "Tool result should be successful")
        
        // Verify file was created
        val testFile = File(tempDir, "test.kt")
        assertTrue(testFile.exists())
        assertEquals("class Test", testFile.readText())
    }

    @Test
    fun `should validate max execution rounds setting`() = runTest {
        // Arrange & Act
        autoExecutionEngine.setMaxExecutionRounds(50)

        // Assert - Should not throw exception and should accept valid value
        // We can't directly test the internal state, but we can test behavior
        val requirement = "Create a simple class"
        val result = autoExecutionEngine.executeConversation(requirement)
        
        assertTrue(result.executionRounds <= 50)
    }

    @Test
    fun `should reject invalid max execution rounds`() = runTest {
        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            autoExecutionEngine.setMaxExecutionRounds(0)
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            autoExecutionEngine.setMaxExecutionRounds(-1)
        }
    }

    @Test
    fun `should continue existing conversation`() = runTest {
        // Arrange - Create a conversation that will be paused
        val requirement = "Create a complex system"
        val engineWithLowLimit = DefaultAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            taskDecomposer = taskDecomposer,
            requirementParser = requirementParser,
            toolExecutor = toolExecutor,
            maxExecutionRounds = 2
        )
        
        val firstResult = engineWithLowLimit.executeConversation(requirement)
        assertEquals(ConversationStatus.WAITING_USER, firstResult.finalStatus)

        // Act - Continue the conversation
        val continueResult = autoExecutionEngine.continueConversation(firstResult.sessionId!!)

        // Assert
        assertTrue(continueResult.success, "Continue should be successful")
        assertTrue(continueResult.executionRounds > firstResult.executionRounds, "Should have more execution rounds")
    }

    @Test
    fun `should handle empty requirement gracefully`() = runTest {
        // Arrange
        val requirement = ""

        // Act
        val result = autoExecutionEngine.executeConversation(requirement)

        // Assert
        assertTrue(result.success, "Empty requirement should be successful")
        assertEquals(ConversationStatus.COMPLETED, result.finalStatus)
        assertTrue(result.executedSteps.isEmpty(), "Should have no executed steps")
        assertEquals(0, result.executionRounds)
    }

    @Test
    fun `should provide detailed execution summary`() = runTest {
        // Arrange
        val requirement = "Create a User class with validation"

        // Act
        val result = autoExecutionEngine.executeConversation(requirement)

        // Assert
        assertTrue(result.success, "Execution should be successful")
        assertNotNull(result.sessionId)
        assertNotNull(result.summary)
        assertTrue(result.summary!!.contains("User"), "Summary should mention User")
        assertTrue(result.executionTime.isPositive(), "Execution time should be positive")
        assertTrue(result.executedSteps.isNotEmpty(), "Should have executed steps")
        
        // Check execution steps have proper metadata
        result.executedSteps.forEach { step ->
            assertNotNull(step.toolCall)
            assertNotNull(step.result)
            assertTrue(step.duration > kotlin.time.Duration.ZERO, "Step duration should be positive")
        }
    }

    @Test
    fun `should handle tool validation failures`() = runTest {
        // Arrange
        val requirement = "Create a file" // This will generate a save-file task
        
        // Create a mock tool executor that fails validation
        val validationFailingExecutor = object : ToolExecutor {
            override suspend fun execute(tool: ToolCall): ToolResult {
                return ToolResult.failure("Validation failed")
            }
            override fun getSupportedTools(): List<ToolMetadata> = emptyList()
            override suspend fun validateParameters(tool: ToolCall): ValidationResult {
                return ValidationResult.invalid("Missing required parameters")
            }
        }
        
        val engineWithValidationFailure = DefaultAutoExecutionEngine(
            conversationStateManager = conversationStateManager,
            taskDecomposer = taskDecomposer,
            requirementParser = requirementParser,
            toolExecutor = validationFailingExecutor,
            maxExecutionRounds = 25
        )

        // Act
        val result = engineWithValidationFailure.executeConversation(requirement)

        // Assert
        assertFalse(result.success, "Should fail due to validation failure")
        assertEquals(ConversationStatus.FAILED, result.finalStatus)
        assertNotNull(result.error)
        assertTrue(result.error?.contains("Validation failed") == true || result.error?.contains("Missing required") == true,
                  "Error should contain validation failure message")
    }
}
