package com.aicodingcli.conversation

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class AiPromptEngineTest {
    
    private lateinit var mockAiService: MockAiService
    private lateinit var aiPromptEngine: AiPromptEngine
    private lateinit var context: ExecutionContext
    
    @BeforeEach
    fun setUp() {
        mockAiService = MockAiService()
        aiPromptEngine = DefaultAiPromptEngine(mockAiService)
        context = ExecutionContext(
            projectPath = "/test/project",
            language = "kotlin",
            framework = "spring-boot",
            buildTool = "gradle"
        )
    }
    
    @Test
    fun `should analyze simple requirement successfully`() = runTest {
        // Arrange
        val requirement = "Create a User class with name and email properties"
        
        // Act
        val result = aiPromptEngine.analyzeRequirement(requirement, context)
        
        // Assert
        assertTrue(result is AnalysisResult.Success)
        val success = result as AnalysisResult.Success
        assertEquals("Create a simple class with basic properties", success.intent)
        assertEquals(RequirementComplexity.SIMPLE, success.complexity)
        assertEquals(RequirementCategory.CODE_GENERATION, success.category)
        assertEquals(2, success.estimatedSteps)
        assertTrue(success.prerequisites.isEmpty())
    }
    
    @Test
    fun `should decide to execute tool for code generation`() = runTest {
        // Arrange
        val requirement = "Create a User class"
        val executionHistory = emptyList<ExecutionStep>()
        val availableTools = listOf(
            ToolMetadata("save-file", "Create a new file", emptyList(), "file"),
            ToolMetadata("view", "View file contents", emptyList(), "file")
        )
        
        // Act
        val result = aiPromptEngine.decideNextAction(requirement, executionHistory, availableTools, context)
        
        // Assert
        assertTrue(result is ActionDecision.ExecuteTool)
        val executeTool = result as ActionDecision.ExecuteTool
        assertEquals("save-file", executeTool.toolName)
        assertTrue(executeTool.parameters.containsKey("path"))
        assertTrue(executeTool.parameters.containsKey("file_content"))
        assertEquals(0.9, executeTool.confidence, 0.01)
    }
    
    @Test
    fun `should decide to complete when requirement is fulfilled`() = runTest {
        // Arrange
        val mockService = MockAiService.withActionResponse(MockAiService.completeActionResponse)
        val engine = DefaultAiPromptEngine(mockService)
        
        val requirement = "Create a User class"
        val executionHistory = listOf(
            ExecutionStep(
                taskId = "task1",
                toolCall = ToolCall("save-file", mapOf("path" to "User.kt", "file_content" to "class User")),
                result = ToolResult.success("File created successfully"),
                duration = kotlin.time.Duration.parse("100ms")
            )
        )
        val availableTools = listOf(
            ToolMetadata("save-file", "Create a new file", emptyList(), "file")
        )
        
        // Act
        val result = engine.decideNextAction(requirement, executionHistory, availableTools, context)
        
        // Assert
        assertTrue(result is ActionDecision.Complete)
        val complete = result as ActionDecision.Complete
        assertEquals("The requirement has been successfully fulfilled", complete.reasoning)
    }
    
    @Test
    fun `should decide to wait for user when clarification needed`() = runTest {
        // Arrange
        val mockService = MockAiService.withActionResponse(MockAiService.waitUserActionResponse)
        val engine = DefaultAiPromptEngine(mockService)
        
        val requirement = "Create something"
        val executionHistory = emptyList<ExecutionStep>()
        val availableTools = listOf(
            ToolMetadata("save-file", "Create a new file", emptyList(), "file")
        )
        
        // Act
        val result = engine.decideNextAction(requirement, executionHistory, availableTools, context)
        
        // Assert
        assertTrue(result is ActionDecision.WaitUser)
        val waitUser = result as ActionDecision.WaitUser
        assertEquals("Need user clarification on the specific requirements", waitUser.reasoning)
    }
    
    @Test
    fun `should decide to fail when cannot proceed`() = runTest {
        // Arrange
        val mockService = MockAiService.withActionResponse(MockAiService.failActionResponse)
        val engine = DefaultAiPromptEngine(mockService)
        
        val requirement = "Create a complex system"
        val executionHistory = emptyList<ExecutionStep>()
        val availableTools = emptyList<ToolMetadata>()
        
        // Act
        val result = engine.decideNextAction(requirement, executionHistory, availableTools, context)
        
        // Assert
        assertTrue(result is ActionDecision.Fail)
        val fail = result as ActionDecision.Fail
        assertEquals("Cannot proceed due to missing dependencies", fail.reasoning)
    }
    
    @Test
    fun `should evaluate completion successfully`() = runTest {
        // Arrange
        val requirement = "Create a User class"
        val executionHistory = listOf(
            ExecutionStep(
                taskId = "task1",
                toolCall = ToolCall("save-file", mapOf("path" to "User.kt", "file_content" to "class User")),
                result = ToolResult.success("File created successfully"),
                duration = kotlin.time.Duration.parse("100ms")
            )
        )
        
        // Act
        val result = aiPromptEngine.evaluateCompletion(requirement, executionHistory, context)
        
        // Assert
        assertTrue(result is CompletionEvaluation.Success)
        val success = result as CompletionEvaluation.Success
        assertTrue(success.completed)
        assertEquals(100, success.completionPercentage)
        assertTrue(success.missingItems.isEmpty())
        assertEquals("Successfully created the User class with required properties", success.summary)
        assertTrue(success.nextSteps.isEmpty())
    }
    
    @Test
    fun `should handle AI service failure gracefully`() = runTest {
        // Arrange
        val failingService = ScenarioMockAiService(TestScenario.AI_FAILURE)
        val engine = DefaultAiPromptEngine(failingService)
        val requirement = "Create a User class"
        
        // Act
        val result = engine.analyzeRequirement(requirement, context)
        
        // Assert
        assertTrue(result is AnalysisResult.Failure)
        val failure = result as AnalysisResult.Failure
        assertTrue(failure.error.contains("Failed to analyze requirement"))
    }
    
    @Test
    fun `should handle invalid JSON response gracefully`() = runTest {
        // Arrange
        val invalidJsonService = ScenarioMockAiService(TestScenario.INVALID_JSON)
        val engine = DefaultAiPromptEngine(invalidJsonService)
        val requirement = "Create a User class"
        
        // Act
        val result = engine.analyzeRequirement(requirement, context)
        
        // Assert
        assertTrue(result is AnalysisResult.Failure)
        val failure = result as AnalysisResult.Failure
        assertTrue(failure.error.contains("Failed to parse analysis response"))
    }
    
    @Test
    fun `should include execution history in action decision prompt`() = runTest {
        // Arrange
        val requirement = "Create and test a User class"
        val executionHistory = listOf(
            ExecutionStep(
                taskId = "task1",
                toolCall = ToolCall("save-file", mapOf("path" to "User.kt")),
                result = ToolResult.success("File created"),
                duration = kotlin.time.Duration.parse("100ms")
            ),
            ExecutionStep(
                taskId = "task2",
                toolCall = ToolCall("view", mapOf("path" to "User.kt")),
                result = ToolResult.success("class User"),
                duration = kotlin.time.Duration.parse("50ms")
            )
        )
        val availableTools = listOf(
            ToolMetadata("save-file", "Create a new file", emptyList(), "file")
        )
        
        // Act
        val result = aiPromptEngine.decideNextAction(requirement, executionHistory, availableTools, context)
        
        // Assert
        assertTrue(result is ActionDecision.ExecuteTool)
        
        // Verify that the AI service received the execution history
        val requests = mockAiService.getRequestHistory()
        assertTrue(requests.isNotEmpty())
        val lastRequest = requests.last()
        val prompt = lastRequest.messages.last().content
        assertTrue(prompt.contains("save-file"))
        assertTrue(prompt.contains("view"))
        assertTrue(prompt.contains("File created"))
    }
    
    @Test
    fun `should limit execution history in prompt to last 5 actions`() = runTest {
        // Arrange
        val requirement = "Create multiple files"
        val executionHistory = (1..10).map { i ->
            ExecutionStep(
                taskId = "task$i",
                toolCall = ToolCall("save-file", mapOf("path" to "File$i.kt")),
                result = ToolResult.success("File $i created"),
                duration = kotlin.time.Duration.parse("100ms")
            )
        }
        val availableTools = listOf(
            ToolMetadata("save-file", "Create a new file", emptyList(), "file")
        )

        // Act
        aiPromptEngine.decideNextAction(requirement, executionHistory, availableTools, context)

        // Assert
        val requests = mockAiService.getRequestHistory()
        val prompt = requests.last().messages.last().content

        // Check that the prompt contains references to the last 5 files
        // The prompt should contain "File 6 created" through "File 10 created"
        assertTrue(prompt.contains("File 6 created"), "Prompt should contain 'File 6 created'")
        assertTrue(prompt.contains("File 10 created"), "Prompt should contain 'File 10 created'")

        // Should not contain early files
        assertFalse(prompt.contains("File 1 created"), "Prompt should not contain 'File 1 created'")
        assertFalse(prompt.contains("File 5 created"), "Prompt should not contain 'File 5 created'")
    }
    
    @Test
    fun `should provide different temperature settings for different operations`() = runTest {
        // Arrange
        val requirement = "Create a User class"
        val executionHistory = emptyList<ExecutionStep>()
        val availableTools = listOf(
            ToolMetadata("save-file", "Create a new file", emptyList(), "file")
        )
        
        // Act - Analysis (should use temperature 0.3)
        aiPromptEngine.analyzeRequirement(requirement, context)
        
        // Act - Action decision (should use temperature 0.2)
        aiPromptEngine.decideNextAction(requirement, executionHistory, availableTools, context)
        
        // Act - Completion evaluation (should use temperature 0.1)
        aiPromptEngine.evaluateCompletion(requirement, executionHistory, context)
        
        // Assert
        val requests = mockAiService.getRequestHistory()
        assertEquals(3, requests.size)
        
        // Analysis request
        assertEquals(0.3f, requests[0].temperature)
        
        // Action decision request
        assertEquals(0.2f, requests[1].temperature)
        
        // Completion evaluation request
        assertEquals(0.1f, requests[2].temperature)
    }

    @Test
    fun `should handle complex requirements with multiple categories`() = runTest {
        // Arrange
        val complexAnalysisResponse = """
            {
                "intent": "Create a complete user management system with validation and tests",
                "complexity": "COMPLEX",
                "category": "CODE_GENERATION",
                "prerequisites": ["database setup", "validation framework"],
                "estimatedSteps": 8,
                "reasoning": "This requires multiple components including models, services, controllers, and tests"
            }
        """.trimIndent()

        val mockService = MockAiService.withAnalysisResponse(complexAnalysisResponse)
        val engine = DefaultAiPromptEngine(mockService)
        val requirement = "Create a complete user management system with CRUD operations, validation, and comprehensive tests"

        // Act
        val result = engine.analyzeRequirement(requirement, context)

        // Assert
        assertTrue(result is AnalysisResult.Success)
        val success = result as AnalysisResult.Success
        assertEquals("Create a complete user management system with validation and tests", success.intent)
        assertEquals(RequirementComplexity.COMPLEX, success.complexity)
        assertEquals(RequirementCategory.CODE_GENERATION, success.category)
        assertEquals(8, success.estimatedSteps)
        assertEquals(2, success.prerequisites.size)
        assertTrue(success.prerequisites.contains("database setup"))
        assertTrue(success.prerequisites.contains("validation framework"))
    }
}
