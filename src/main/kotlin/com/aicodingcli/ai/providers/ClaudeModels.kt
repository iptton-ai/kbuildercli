package com.aicodingcli.ai.providers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Claude API request models
 */
@Serializable
data class ClaudeRequest(
    val model: String,
    val messages: List<ClaudeMessage>,
    @SerialName("max_tokens")
    val maxTokens: Int,
    val temperature: Float? = null,
    val stream: Boolean = false,
    val system: String? = null,
    @SerialName("stop_sequences")
    val stopSequences: List<String>? = null
)

@Serializable
data class ClaudeMessage(
    val role: String,
    val content: String
)

/**
 * Claude API response models
 */
@Serializable
data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContent>,
    val model: String,
    @SerialName("stop_reason")
    val stopReason: String?,
    @SerialName("stop_sequence")
    val stopSequence: String?,
    val usage: ClaudeUsage
)

@Serializable
data class ClaudeContent(
    val type: String,
    val text: String
)

@Serializable
data class ClaudeUsage(
    @SerialName("input_tokens")
    val inputTokens: Int,
    @SerialName("output_tokens")
    val outputTokens: Int
)

/**
 * Claude streaming response models
 */
@Serializable
data class ClaudeStreamEvent(
    val type: String,
    val message: ClaudeStreamMessage? = null,
    val delta: ClaudeStreamDelta? = null,
    val usage: ClaudeUsage? = null
)

@Serializable
data class ClaudeStreamMessage(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContent>,
    val model: String,
    @SerialName("stop_reason")
    val stopReason: String?,
    @SerialName("stop_sequence")
    val stopSequence: String?,
    val usage: ClaudeUsage
)

@Serializable
data class ClaudeStreamDelta(
    val type: String,
    val text: String? = null,
    @SerialName("stop_reason")
    val stopReason: String? = null,
    @SerialName("stop_sequence")
    val stopSequence: String? = null
)

/**
 * Claude error response models
 */
@Serializable
data class ClaudeErrorResponse(
    val type: String,
    val error: ClaudeError
)

@Serializable
data class ClaudeError(
    val type: String,
    val message: String
)

/**
 * Claude specific exception
 */
class ClaudeException(
    message: String,
    val errorType: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)
