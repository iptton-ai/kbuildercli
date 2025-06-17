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
 * Claude service implementation
 */
class ClaudeService(
    override val config: AiServiceConfig,
    private val httpClient: AiHttpClient = AiHttpClient()
) : BaseAiService(config) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val baseUrl = config.baseUrl ?: "https://api.anthropic.com/v1"

    override suspend fun chat(request: AiRequest): AiResponse {
        validateRequest(request)
        
        try {
            val claudeRequest = convertToClaudeRequest(request)
            val requestBody = json.encodeToString(claudeRequest)
            
            val response = httpClient.post(
                url = "$baseUrl/messages",
                body = requestBody,
                headers = createHeaders()
            )
            
            val claudeResponse = json.decodeFromString<ClaudeResponse>(response.body)
            return convertToAiResponse(claudeResponse)
            
        } catch (e: HttpException) {
            throw handleHttpException(e)
        } catch (e: Exception) {
            throw ClaudeException("Failed to process chat request: ${e.message}", cause = e)
        }
    }

    override suspend fun streamChat(request: AiRequest): Flow<AiStreamChunk> {
        validateRequest(request)

        return flow {
            try {
                val claudeRequest = convertToClaudeRequest(request.copy(stream = true))
                val requestBody = json.encodeToString(claudeRequest)

                httpClient.postStream(
                    url = "$baseUrl/messages",
                    body = requestBody,
                    headers = createHeaders()
                ).collect { data ->
                    if (data.isNotBlank()) {
                        try {
                            val streamEvent = json.decodeFromString<ClaudeStreamEvent>(data)

                            when (streamEvent.type) {
                                "content_block_delta" -> {
                                    val content = streamEvent.delta?.text ?: ""
                                    emit(AiStreamChunk(content, null))
                                }
                                "message_stop" -> {
                                    val finishReason = when (streamEvent.message?.stopReason) {
                                        "end_turn" -> FinishReason.STOP
                                        "max_tokens" -> FinishReason.LENGTH
                                        "stop_sequence" -> FinishReason.STOP
                                        else -> FinishReason.STOP
                                    }
                                    emit(AiStreamChunk("", finishReason))
                                }
                            }
                        } catch (e: Exception) {
                            // Skip malformed JSON chunks
                        }
                    }
                }

            } catch (e: HttpException) {
                throw handleHttpException(e)
            } catch (e: Exception) {
                throw ClaudeException("Failed to process streaming chat request: ${e.message}", cause = e)
            }
        }
    }

    override suspend fun testConnection(): Boolean {
        return try {
            val testRequest = ClaudeRequest(
                model = config.model,
                messages = listOf(ClaudeMessage("user", "Test connection")),
                maxTokens = 10
            )
            val requestBody = json.encodeToString(testRequest)
            
            val response = httpClient.post(
                url = "$baseUrl/messages",
                body = requestBody,
                headers = createHeaders()
            )
            
            // Try to parse the response to ensure it's valid
            json.decodeFromString<ClaudeResponse>(response.body)
            true
            
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Convert AI request to Claude format
     */
    private fun convertToClaudeRequest(request: AiRequest): ClaudeRequest {
        val claudeMessages = request.messages.map { message ->
            ClaudeMessage(
                role = when (message.role) {
                    MessageRole.SYSTEM -> "user" // Claude handles system messages differently
                    MessageRole.USER -> "user"
                    MessageRole.ASSISTANT -> "assistant"
                },
                content = message.content
            )
        }
        
        return ClaudeRequest(
            model = request.model,
            messages = claudeMessages,
            maxTokens = request.maxTokens,
            temperature = request.temperature,
            stream = request.stream
        )
    }

    /**
     * Convert Claude response to AI format
     */
    private fun convertToAiResponse(claudeResponse: ClaudeResponse): AiResponse {
        val content = claudeResponse.content.firstOrNull()?.text
            ?: throw ClaudeException("No content in Claude response")
        
        val finishReason = when (claudeResponse.stopReason) {
            "end_turn" -> FinishReason.STOP
            "max_tokens" -> FinishReason.LENGTH
            "stop_sequence" -> FinishReason.STOP
            else -> FinishReason.STOP
        }
        
        return AiResponse(
            content = content,
            model = claudeResponse.model,
            usage = TokenUsage(
                promptTokens = claudeResponse.usage.inputTokens,
                completionTokens = claudeResponse.usage.outputTokens,
                totalTokens = claudeResponse.usage.inputTokens + claudeResponse.usage.outputTokens
            ),
            finishReason = finishReason
        )
    }

    /**
     * Create headers for Claude API requests
     */
    private fun createHeaders(): Map<String, String> {
        return mapOf(
            "x-api-key" to config.apiKey,
            "Content-Type" to "application/json",
            "anthropic-version" to "2023-06-01"
        )
    }

    /**
     * Handle HTTP exceptions and convert to Claude exceptions
     */
    private fun handleHttpException(e: HttpException): ClaudeException {
        return try {
            val errorResponse = json.decodeFromString<ClaudeErrorResponse>(e.responseBody)
            ClaudeException(
                message = errorResponse.error.message,
                errorType = errorResponse.error.type,
                cause = e
            )
        } catch (parseException: Exception) {
            ClaudeException(
                message = "Claude API error: ${e.statusCode.value} - ${e.responseBody}",
                cause = e
            )
        }
    }
}
