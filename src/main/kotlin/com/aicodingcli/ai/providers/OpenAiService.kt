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
 * OpenAI service implementation
 */
class OpenAiService(
    override val config: AiServiceConfig,
    private val httpClient: AiHttpClient = AiHttpClient()
) : BaseAiService(config) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val baseUrl = config.baseUrl ?: "https://api.openai.com/v1"

    override suspend fun chat(request: AiRequest): AiResponse {
        validateRequest(request)
        
        try {
            val openAiRequest = convertToOpenAiRequest(request)
            val requestBody = json.encodeToString(openAiRequest)
            
            val response = httpClient.post(
                url = "$baseUrl/chat/completions",
                body = requestBody,
                headers = createHeaders()
            )
            
            val openAiResponse = json.decodeFromString<OpenAiChatResponse>(response.body)
            return convertToAiResponse(openAiResponse)
            
        } catch (e: HttpException) {
            throw handleHttpException(e)
        } catch (e: Exception) {
            throw OpenAiException("Failed to process chat request: ${e.message}", cause = e)
        }
    }

    override suspend fun streamChat(request: AiRequest): Flow<AiStreamChunk> {
        validateRequest(request)

        return flow {
            try {
                val openAiRequest = convertToOpenAiRequest(request.copy(stream = true))
                val requestBody = json.encodeToString(openAiRequest)

                httpClient.postStream(
                    url = "$baseUrl/chat/completions",
                    body = requestBody,
                    headers = createHeaders()
                ).collect { data ->
                    if (data.isNotBlank()) {
                        try {
                            val streamResponse = json.decodeFromString<OpenAiStreamResponse>(data)
                            val choice = streamResponse.choices.firstOrNull()

                            if (choice != null) {
                                val content = choice.delta.content ?: ""
                                val finishReason = when (choice.finishReason) {
                                    "stop" -> FinishReason.STOP
                                    "length" -> FinishReason.LENGTH
                                    "content_filter" -> FinishReason.CONTENT_FILTER
                                    "function_call" -> FinishReason.FUNCTION_CALL
                                    null -> null
                                    else -> FinishReason.STOP
                                }

                                emit(AiStreamChunk(content, finishReason))
                            }
                        } catch (e: Exception) {
                            // Skip malformed JSON chunks
                        }
                    }
                }

            } catch (e: HttpException) {
                throw handleHttpException(e)
            } catch (e: Exception) {
                throw OpenAiException("Failed to process streaming chat request: ${e.message}", cause = e)
            }
        }
    }

    override suspend fun testConnection(): Boolean {
        return try {
            val response = httpClient.get(
                url = "$baseUrl/models",
                headers = createHeaders()
            )
            
            // Try to parse the response to ensure it's valid
            json.decodeFromString<OpenAiModelsResponse>(response.body)
            true
            
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Convert AI request to OpenAI format
     */
    private fun convertToOpenAiRequest(request: AiRequest): OpenAiChatRequest {
        val openAiMessages = request.messages.map { message ->
            OpenAiMessage(
                role = when (message.role) {
                    MessageRole.SYSTEM -> "system"
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                },
                content = message.content
            )
        }
        
        return OpenAiChatRequest(
            model = request.model,
            messages = openAiMessages,
            temperature = request.temperature,
            maxTokens = request.maxTokens,
            stream = request.stream
        )
    }

    /**
     * Convert OpenAI response to AI format
     */
    private fun convertToAiResponse(openAiResponse: OpenAiChatResponse): AiResponse {
        val choice = openAiResponse.choices.firstOrNull()
            ?: throw OpenAiException("No choices in OpenAI response")
        
        val finishReason = when (choice.finishReason) {
            "stop" -> FinishReason.STOP
            "length" -> FinishReason.LENGTH
            "content_filter" -> FinishReason.CONTENT_FILTER
            "function_call" -> FinishReason.FUNCTION_CALL
            else -> FinishReason.STOP
        }
        
        return AiResponse(
            content = choice.message.content,
            model = openAiResponse.model,
            usage = TokenUsage(
                promptTokens = openAiResponse.usage.promptTokens,
                completionTokens = openAiResponse.usage.completionTokens,
                totalTokens = openAiResponse.usage.totalTokens
            ),
            finishReason = finishReason
        )
    }

    /**
     * Create headers for OpenAI API requests
     */
    private fun createHeaders(): Map<String, String> {
        return mapOf(
            "Authorization" to "Bearer ${config.apiKey}",
            "Content-Type" to "application/json"
        )
    }

    /**
     * Handle HTTP exceptions and convert to OpenAI exceptions
     */
    private fun handleHttpException(e: HttpException): OpenAiException {
        return try {
            val errorResponse = json.decodeFromString<OpenAiErrorResponse>(e.responseBody)
            OpenAiException(
                message = errorResponse.error.message,
                errorType = errorResponse.error.type,
                errorCode = errorResponse.error.code,
                cause = e
            )
        } catch (parseException: Exception) {
            OpenAiException(
                message = "OpenAI API error: ${e.statusCode.value} - ${e.responseBody}",
                cause = e
            )
        }
    }
}
