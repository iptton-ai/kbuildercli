package com.aicodingcli.ai.providers

import com.aicodingcli.ai.*
import com.aicodingcli.http.AiHttpClient
import com.aicodingcli.http.HttpException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * Ollama service implementation
 */
class OllamaService(
    override val config: AiServiceConfig,
    private val httpClient: AiHttpClient = AiHttpClient()
) : BaseAiService(config) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val baseUrl = config.baseUrl ?: "http://localhost:11434"

    override suspend fun chat(request: AiRequest): AiResponse {
        validateRequest(request)
        
        try {
            val ollamaRequest = convertToOllamaRequest(request)
            val requestBody = json.encodeToString(ollamaRequest)
            
            val response = httpClient.post(
                url = "$baseUrl/api/chat",
                body = requestBody,
                headers = createHeaders()
            )
            
            val ollamaResponse = json.decodeFromString<OllamaResponse>(response.body)
            return convertToAiResponse(ollamaResponse)
            
        } catch (e: HttpException) {
            throw handleHttpException(e)
        } catch (e: Exception) {
            throw OllamaException("Failed to process chat request: ${e.message}", cause = e)
        }
    }

    override suspend fun streamChat(request: AiRequest): Flow<AiStreamChunk> {
        validateRequest(request)

        return flow {
            try {
                val ollamaRequest = convertToOllamaRequest(request.copy(stream = true))
                val requestBody = json.encodeToString(ollamaRequest)

                httpClient.postStream(
                    url = "$baseUrl/api/chat",
                    body = requestBody,
                    headers = createHeaders()
                ).collect { data ->
                    if (data.isNotBlank()) {
                        try {
                            val streamResponse = json.decodeFromString<OllamaResponse>(data)
                            val content = streamResponse.message.content

                            val finishReason = if (streamResponse.done) {
                                when (streamResponse.doneReason) {
                                    "stop" -> FinishReason.STOP
                                    "length" -> FinishReason.LENGTH
                                    null -> FinishReason.STOP
                                    else -> FinishReason.STOP
                                }
                            } else {
                                null
                            }

                            emit(AiStreamChunk(content, finishReason))
                        } catch (e: Exception) {
                            // Skip malformed JSON chunks
                        }
                    }
                }

            } catch (e: HttpException) {
                throw handleHttpException(e)
            } catch (e: Exception) {
                throw OllamaException("Failed to process streaming chat request: ${e.message}", cause = e)
            }
        }
    }

    override suspend fun testConnection(): Boolean {
        return try {
            val response = httpClient.get(
                url = "$baseUrl/api/tags",
                headers = createHeaders()
            )
            
            // Try to parse the response to ensure it's valid
            json.decodeFromString<OllamaModelsResponse>(response.body)
            true
            
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Convert AI request to Ollama format
     */
    private fun convertToOllamaRequest(request: AiRequest): OllamaRequest {
        val ollamaMessages = request.messages.map { message ->
            OllamaMessage(
                role = when (message.role) {
                    MessageRole.SYSTEM -> "system"
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                },
                content = message.content
            )
        }
        
        val options = OllamaOptions(
            temperature = request.temperature,
            numPredict = request.maxTokens
        )
        
        return OllamaRequest(
            model = request.model,
            messages = ollamaMessages,
            stream = request.stream,
            options = options
        )
    }

    /**
     * Convert Ollama response to AI format
     */
    private fun convertToAiResponse(ollamaResponse: OllamaResponse): AiResponse {
        val content = ollamaResponse.message.content
        
        val finishReason = when (ollamaResponse.doneReason) {
            "stop" -> FinishReason.STOP
            "length" -> FinishReason.LENGTH
            null -> if (ollamaResponse.done) FinishReason.STOP else FinishReason.STOP
            else -> FinishReason.STOP
        }
        
        // Calculate token usage from Ollama's metrics
        val promptTokens = ollamaResponse.promptEvalCount ?: 0
        val completionTokens = ollamaResponse.evalCount ?: 0
        val totalTokens = promptTokens + completionTokens
        
        return AiResponse(
            content = content,
            model = ollamaResponse.model,
            usage = TokenUsage(
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                totalTokens = totalTokens
            ),
            finishReason = finishReason
        )
    }

    /**
     * Create headers for Ollama API requests
     */
    private fun createHeaders(): Map<String, String> {
        return mapOf(
            "Content-Type" to "application/json"
        )
        // Note: Ollama doesn't require authentication headers
    }

    /**
     * Handle HTTP exceptions and convert to Ollama exceptions
     */
    private fun handleHttpException(e: HttpException): OllamaException {
        return try {
            val errorResponse = json.decodeFromString<OllamaErrorResponse>(e.responseBody)
            OllamaException(
                message = errorResponse.error,
                cause = e
            )
        } catch (parseException: Exception) {
            OllamaException(
                message = "Ollama API error: ${e.statusCode.value} - ${e.responseBody}",
                cause = e
            )
        }
    }
}
