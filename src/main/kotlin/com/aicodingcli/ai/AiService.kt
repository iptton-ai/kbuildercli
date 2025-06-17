package com.aicodingcli.ai

import kotlinx.coroutines.flow.Flow
import com.aicodingcli.ai.providers.OpenAiService as RealOpenAiService
import com.aicodingcli.ai.providers.ClaudeService as RealClaudeService
import com.aicodingcli.ai.providers.OllamaService as RealOllamaService

/**
 * AI service interface for different providers
 */
interface AiService {
    /**
     * Send a chat request and get response
     */
    suspend fun chat(request: AiRequest): AiResponse

    /**
     * Send a chat request and get streaming response
     */
    suspend fun streamChat(request: AiRequest): Flow<AiStreamChunk>

    /**
     * Test connection to AI service
     */
    suspend fun testConnection(): Boolean

    /**
     * Get service configuration
     */
    val config: AiServiceConfig
}

/**
 * Factory for creating AI services
 */
object AiServiceFactory {
    /**
     * Create AI service based on configuration
     */
    fun createService(config: AiServiceConfig): AiService {
        // Configuration is already validated in AiServiceConfig.init
        return when (config.provider) {
            AiProvider.OPENAI -> RealOpenAiService(config)
            AiProvider.CLAUDE -> RealClaudeService(config)
            AiProvider.OLLAMA -> RealOllamaService(config)
        }
    }
}

/**
 * Base implementation for AI services
 */
abstract class BaseAiService(override val config: AiServiceConfig) : AiService {
    
    protected fun validateRequest(request: AiRequest) {
        require(request.messages.isNotEmpty()) { "Messages cannot be empty" }
        require(request.model.isNotBlank()) { "Model cannot be empty" }
    }
}

/**
 * OpenAI service implementation (placeholder)
 */
class OpenAiService(config: AiServiceConfig) : BaseAiService(config) {
    override suspend fun chat(request: AiRequest): AiResponse {
        validateRequest(request)
        // TODO: Implement actual OpenAI API call
        return AiResponse(
            content = "Mock response from OpenAI",
            model = request.model,
            usage = TokenUsage(10, 5, 15),
            finishReason = FinishReason.STOP
        )
    }

    override suspend fun streamChat(request: AiRequest): Flow<AiStreamChunk> {
        validateRequest(request)
        // TODO: Implement actual streaming
        return kotlinx.coroutines.flow.flowOf(
            AiStreamChunk("Mock", null),
            AiStreamChunk(" response", null),
            AiStreamChunk(" from OpenAI", FinishReason.STOP)
        )
    }

    override suspend fun testConnection(): Boolean {
        // TODO: Implement actual connection test
        return true
    }
}






