package com.aicodingcli.ai.providers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI API request models
 */
@Serializable
data class OpenAiChatRequest(
    val model: String,
    val messages: List<OpenAiMessage>,
    val temperature: Float? = null,
    @SerialName("max_tokens")
    val maxTokens: Int? = null,
    val stream: Boolean = false
)

@Serializable
data class OpenAiMessage(
    val role: String,
    val content: String
)

/**
 * OpenAI API response models
 */
@Serializable
data class OpenAiChatResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAiChoice>,
    val usage: OpenAiUsage
)

@Serializable
data class OpenAiChoice(
    val index: Int,
    val message: OpenAiMessage,
    @SerialName("finish_reason")
    val finishReason: String?
)

@Serializable
data class OpenAiUsage(
    @SerialName("prompt_tokens")
    val promptTokens: Int,
    @SerialName("completion_tokens")
    val completionTokens: Int,
    @SerialName("total_tokens")
    val totalTokens: Int
)

/**
 * OpenAI streaming response models
 */
@Serializable
data class OpenAiStreamResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAiStreamChoice>
)

@Serializable
data class OpenAiStreamChoice(
    val index: Int,
    val delta: OpenAiStreamDelta,
    @SerialName("finish_reason")
    val finishReason: String? = null
)

@Serializable
data class OpenAiStreamDelta(
    val role: String? = null,
    val content: String? = null
)

/**
 * OpenAI error response models
 */
@Serializable
data class OpenAiErrorResponse(
    val error: OpenAiError
)

@Serializable
data class OpenAiError(
    val message: String,
    val type: String,
    val param: String? = null,
    val code: String? = null
)

/**
 * OpenAI models list response
 */
@Serializable
data class OpenAiModelsResponse(
    val `object`: String,
    val data: List<OpenAiModel>
)

@Serializable
data class OpenAiModel(
    val id: String,
    val `object`: String,
    val created: Long? = null,
    val owned_by: String? = null
)

/**
 * OpenAI specific exception
 */
class OpenAiException(
    message: String,
    val errorType: String? = null,
    val errorCode: String? = null,
    cause: Throwable? = null
) : Exception(message, cause)
