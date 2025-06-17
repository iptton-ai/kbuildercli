package com.aicodingcli.ai

import kotlinx.serialization.Serializable

/**
 * Supported AI providers
 */
enum class AiProvider {
    OPENAI,
    CLAUDE,
    OLLAMA
}

/**
 * Message roles in AI conversation
 */
enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}

/**
 * Finish reasons for AI responses
 */
enum class FinishReason {
    STOP,
    LENGTH,
    CONTENT_FILTER,
    FUNCTION_CALL
}

/**
 * Configuration for AI service
 */
@Serializable
data class AiServiceConfig(
    val provider: AiProvider,
    val apiKey: String,
    val model: String,
    val baseUrl: String? = null,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val timeout: Long = 30000L
) {
    init {
        require(apiKey.isNotBlank()) { "API key cannot be empty" }
        require(model.isNotBlank()) { "Model cannot be empty" }
        require(temperature in 0.0f..1.0f) { "Temperature must be between 0.0 and 1.0" }
        require(maxTokens > 0) { "Max tokens must be positive" }
        require(timeout > 0) { "Timeout must be positive" }
    }
}

/**
 * AI message in conversation
 */
@Serializable
data class AiMessage(
    val role: MessageRole,
    val content: String
) {
    init {
        require(content.isNotBlank()) { "Message content cannot be empty" }
    }
}

/**
 * AI request
 */
@Serializable
data class AiRequest(
    val messages: List<AiMessage>,
    val model: String,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val stream: Boolean = false
) {
    init {
        require(messages.isNotEmpty()) { "Messages cannot be empty" }
        require(model.isNotBlank()) { "Model cannot be empty" }
        require(temperature in 0.0f..1.0f) { "Temperature must be between 0.0 and 1.0" }
        require(maxTokens > 0) { "Max tokens must be positive" }
    }
}

/**
 * Token usage information
 */
@Serializable
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/**
 * AI response
 */
@Serializable
data class AiResponse(
    val content: String,
    val model: String,
    val usage: TokenUsage,
    val finishReason: FinishReason
)

/**
 * AI streaming chunk
 */
@Serializable
data class AiStreamChunk(
    val content: String,
    val finishReason: FinishReason? = null
)
