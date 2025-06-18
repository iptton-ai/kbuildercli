package com.aicodingcli.conversation

import com.aicodingcli.ai.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Mock AI Service for testing AI Prompt Engine
 */
class MockAiService(
    private val responses: Map<String, String> = defaultResponses
) : AiService {
    
    override val config: AiServiceConfig = AiServiceConfig(
        provider = AiProvider.OPENAI,
        apiKey = "mock-key",
        model = "gpt-4"
    )
    
    private val requestHistory = mutableListOf<AiRequest>()
    
    override suspend fun chat(request: AiRequest): AiResponse {
        requestHistory.add(request)
        
        val prompt = request.messages.lastOrNull()?.content ?: ""
        val response = findMatchingResponse(prompt)
        
        return AiResponse(
            content = response,
            model = request.model,
            usage = TokenUsage(promptTokens = 100, completionTokens = 50, totalTokens = 150),
            finishReason = FinishReason.STOP
        )
    }
    
    override suspend fun streamChat(request: AiRequest): Flow<AiStreamChunk> {
        val response = chat(request)
        return flowOf(
            AiStreamChunk(response.content, null),
            AiStreamChunk("", FinishReason.STOP)
        )
    }
    
    override suspend fun testConnection(): Boolean = true
    
    fun getRequestHistory(): List<AiRequest> = requestHistory.toList()
    
    fun clearHistory() = requestHistory.clear()
    
    private fun findMatchingResponse(prompt: String): String {
        return when {
            prompt.contains("analyze") && prompt.contains("requirement") -> responses["analysis"] ?: defaultAnalysisResponse
            prompt.contains("decide") && prompt.contains("next action") -> responses["action"] ?: defaultActionResponse
            prompt.contains("evaluate") && prompt.contains("completion") -> responses["completion"] ?: defaultCompletionResponse
            else -> responses["default"] ?: "Mock AI response"
        }
    }
    
    companion object {
        val defaultAnalysisResponse = """
            {
                "intent": "Create a simple class with basic properties",
                "complexity": "SIMPLE",
                "category": "CODE_GENERATION",
                "prerequisites": [],
                "estimatedSteps": 2,
                "reasoning": "This is a straightforward class creation task"
            }
        """.trimIndent()
        
        val defaultActionResponse = """
            {
                "action": "EXECUTE_TOOL",
                "toolName": "save-file",
                "parameters": {
                    "path": "User.kt",
                    "file_content": "data class User(val name: String, val email: String)"
                },
                "reasoning": "Creating the User class as requested",
                "confidence": 0.9
            }
        """.trimIndent()
        
        val defaultCompletionResponse = """
            {
                "completed": true,
                "completionPercentage": 100,
                "missingItems": [],
                "summary": "Successfully created the User class with required properties",
                "nextSteps": []
            }
        """.trimIndent()
        
        val completeActionResponse = """
            {
                "action": "COMPLETE",
                "reasoning": "The requirement has been successfully fulfilled",
                "confidence": 0.95
            }
        """.trimIndent()
        
        val waitUserActionResponse = """
            {
                "action": "WAIT_USER",
                "reasoning": "Need user clarification on the specific requirements",
                "confidence": 0.8
            }
        """.trimIndent()
        
        val failActionResponse = """
            {
                "action": "FAIL",
                "reasoning": "Cannot proceed due to missing dependencies",
                "confidence": 0.9
            }
        """.trimIndent()
        
        val defaultResponses = mapOf(
            "analysis" to defaultAnalysisResponse,
            "action" to defaultActionResponse,
            "completion" to defaultCompletionResponse,
            "default" to "Mock AI response"
        )
        
        fun withCustomResponses(customResponses: Map<String, String>): MockAiService {
            return MockAiService(defaultResponses + customResponses)
        }
        
        fun withAnalysisResponse(response: String): MockAiService {
            return MockAiService(defaultResponses + ("analysis" to response))
        }
        
        fun withActionResponse(response: String): MockAiService {
            return MockAiService(defaultResponses + ("action" to response))
        }
        
        fun withCompletionResponse(response: String): MockAiService {
            return MockAiService(defaultResponses + ("completion" to response))
        }
    }
}

/**
 * Mock AI Service that simulates different scenarios
 */
class ScenarioMockAiService(private val scenario: TestScenario) : AiService {
    
    override val config: AiServiceConfig = AiServiceConfig(
        provider = AiProvider.OPENAI,
        apiKey = "mock-key",
        model = "gpt-4"
    )
    
    private var callCount = 0
    
    override suspend fun chat(request: AiRequest): AiResponse {
        callCount++
        val prompt = request.messages.lastOrNull()?.content ?: ""
        
        val response = when (scenario) {
            TestScenario.SIMPLE_SUCCESS -> getSimpleSuccessResponse(prompt, callCount)
            TestScenario.MULTI_STEP -> getMultiStepResponse(prompt, callCount)
            TestScenario.REQUIRES_CONFIRMATION -> getConfirmationRequiredResponse(prompt, callCount)
            TestScenario.AI_FAILURE -> throw RuntimeException("Simulated AI service failure")
            TestScenario.INVALID_JSON -> "Invalid JSON response"
            TestScenario.MAX_ROUNDS -> getMaxRoundsResponse(prompt, callCount)
        }
        
        return AiResponse(
            content = response,
            model = request.model,
            usage = TokenUsage(promptTokens = 100, completionTokens = 50, totalTokens = 150),
            finishReason = FinishReason.STOP
        )
    }
    
    override suspend fun streamChat(request: AiRequest): Flow<AiStreamChunk> {
        val response = chat(request)
        return flowOf(AiStreamChunk(response.content, FinishReason.STOP))
    }
    
    override suspend fun testConnection(): Boolean = true
    
    private fun getSimpleSuccessResponse(prompt: String, callCount: Int): String {
        return when {
            prompt.contains("analyze") -> MockAiService.defaultAnalysisResponse
            prompt.contains("decide") && callCount == 2 -> MockAiService.defaultActionResponse  // First decision call
            prompt.contains("decide") && callCount == 3 -> MockAiService.completeActionResponse  // Second decision call
            prompt.contains("evaluate") -> MockAiService.defaultCompletionResponse
            else -> "Mock response"
        }
    }
    
    private fun getMultiStepResponse(prompt: String, callCount: Int): String {
        return when {
            prompt.contains("analyze") -> """
                {
                    "intent": "Create a User class with validation",
                    "complexity": "MODERATE",
                    "category": "CODE_GENERATION",
                    "prerequisites": [],
                    "estimatedSteps": 3,
                    "reasoning": "Need to create class, add validation, and create tests"
                }
            """.trimIndent()
            prompt.contains("decide") && callCount == 2 -> """
                {
                    "action": "EXECUTE_TOOL",
                    "toolName": "save-file",
                    "parameters": {
                        "path": "User.kt",
                        "file_content": "data class User(val name: String, val email: String)"
                    },
                    "reasoning": "First create the basic User class",
                    "confidence": 0.9
                }
            """.trimIndent()
            prompt.contains("decide") && callCount == 3 -> """
                {
                    "action": "EXECUTE_TOOL",
                    "toolName": "str-replace-editor",
                    "parameters": {
                        "path": "User.kt",
                        "old_str": "data class User(val name: String, val email: String)",
                        "new_str": "data class User(val name: String, val email: String) { init { require(name.isNotBlank()) { \"Name cannot be blank\" } } }"
                    },
                    "reasoning": "Add validation to the User class",
                    "confidence": 0.85
                }
            """.trimIndent()
            prompt.contains("decide") && callCount >= 4 -> MockAiService.completeActionResponse
            prompt.contains("evaluate") -> MockAiService.defaultCompletionResponse
            else -> "Mock response"
        }
    }
    
    private fun getConfirmationRequiredResponse(prompt: String, callCount: Int): String {
        return when {
            prompt.contains("analyze") -> MockAiService.defaultAnalysisResponse
            prompt.contains("decide") -> """
                {
                    "action": "EXECUTE_TOOL",
                    "toolName": "remove-files",
                    "parameters": {
                        "file_paths": "old-file.kt"
                    },
                    "reasoning": "Need to remove old file before creating new one",
                    "confidence": 0.8
                }
            """.trimIndent()
            else -> "Mock response"
        }
    }
    
    private fun getMaxRoundsResponse(prompt: String, callCount: Int): String {
        return when {
            prompt.contains("analyze") -> MockAiService.defaultAnalysisResponse
            prompt.contains("decide") -> """
                {
                    "action": "EXECUTE_TOOL",
                    "toolName": "view",
                    "parameters": {
                        "path": "file$callCount.kt",
                        "type": "file"
                    },
                    "reasoning": "Checking file $callCount",
                    "confidence": 0.7
                }
            """.trimIndent()
            else -> "Mock response"
        }
    }
}

enum class TestScenario {
    SIMPLE_SUCCESS,
    MULTI_STEP,
    REQUIRES_CONFIRMATION,
    AI_FAILURE,
    INVALID_JSON,
    MAX_ROUNDS
}
