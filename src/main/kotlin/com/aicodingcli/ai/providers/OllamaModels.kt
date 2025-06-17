package com.aicodingcli.ai.providers

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ollama API request models
 */
@Serializable
data class OllamaRequest(
    val model: String,
    val messages: List<OllamaMessage>,
    val stream: Boolean,
    val format: String? = null,
    val options: OllamaOptions? = null,
    @SerialName("keep_alive")
    val keepAlive: String? = null
)

@Serializable
data class OllamaMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null
)

@Serializable
data class OllamaOptions(
    val temperature: Float? = null,
    @SerialName("num_predict")
    val numPredict: Int? = null,
    @SerialName("top_k")
    val topK: Int? = null,
    @SerialName("top_p")
    val topP: Float? = null,
    val seed: Int? = null,
    val stop: List<String>? = null
)

/**
 * Ollama API response models
 */
@Serializable
data class OllamaResponse(
    val model: String,
    @SerialName("created_at")
    val createdAt: String,
    val message: OllamaResponseMessage,
    val done: Boolean,
    @SerialName("done_reason")
    val doneReason: String? = null,
    @SerialName("total_duration")
    val totalDuration: Long? = null,
    @SerialName("load_duration")
    val loadDuration: Long? = null,
    @SerialName("prompt_eval_count")
    val promptEvalCount: Int? = null,
    @SerialName("prompt_eval_duration")
    val promptEvalDuration: Long? = null,
    @SerialName("eval_count")
    val evalCount: Int? = null,
    @SerialName("eval_duration")
    val evalDuration: Long? = null
)

@Serializable
data class OllamaResponseMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null
)

/**
 * Ollama streaming response models
 */
@Serializable
data class OllamaStreamResponse(
    val model: String,
    @SerialName("created_at")
    val createdAt: String,
    val message: OllamaResponseMessage? = null,
    val done: Boolean,
    @SerialName("done_reason")
    val doneReason: String? = null,
    @SerialName("total_duration")
    val totalDuration: Long? = null,
    @SerialName("load_duration")
    val loadDuration: Long? = null,
    @SerialName("prompt_eval_count")
    val promptEvalCount: Int? = null,
    @SerialName("prompt_eval_duration")
    val promptEvalDuration: Long? = null,
    @SerialName("eval_count")
    val evalCount: Int? = null,
    @SerialName("eval_duration")
    val evalDuration: Long? = null
)

/**
 * Ollama error response models
 */
@Serializable
data class OllamaErrorResponse(
    val error: String
)

/**
 * Ollama models list response
 */
@Serializable
data class OllamaModelsResponse(
    val models: List<OllamaModel>
)

@Serializable
data class OllamaModel(
    val name: String,
    val model: String? = null,
    val size: Long,
    val digest: String,
    val details: OllamaModelDetails? = null,
    @SerialName("expires_at")
    val expiresAt: String? = null,
    @SerialName("size_vram")
    val sizeVram: Long? = null
)

@Serializable
data class OllamaModelDetails(
    @SerialName("parent_model")
    val parentModel: String? = null,
    val format: String,
    val family: String,
    val families: List<String>? = null,
    @SerialName("parameter_size")
    val parameterSize: String,
    @SerialName("quantization_level")
    val quantizationLevel: String
)

/**
 * Ollama specific exception
 */
class OllamaException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
