package com.aicodingcli.http

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json

/**
 * HTTP client wrapper for AI service calls
 */
class AiHttpClient(
    private val engine: HttpClientEngine? = null,
    private val timeoutMs: Long = 30000,
    private val retryConfig: RetryConfig = RetryConfig()
) {
    
    private val client = HttpClient(engine ?: CIO.create()) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }

        install(HttpTimeout) {
            requestTimeoutMillis = timeoutMs
            connectTimeoutMillis = timeoutMs
            socketTimeoutMillis = timeoutMs
        }

        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 0) // We handle retries manually
            retryOnException(maxRetries = 0, retryOnTimeout = false)
        }

        defaultRequest {
            headers.append(HttpHeaders.UserAgent, "AiCodingCli/1.0")
        }
    }

    /**
     * Make GET request
     */
    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): HttpResponse {
        return executeWithRetry {
            val response = client.get(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            handleResponse(response)
        }
    }

    /**
     * Make POST request
     */
    suspend fun post(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap()
    ): HttpResponse {
        return executeWithRetry {
            val response = client.post(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(body)
            }
            handleResponse(response)
        }
    }

    /**
     * Make streaming POST request for Server-Sent Events or JSONL
     */
    suspend fun postStream(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap()
    ): Flow<String> {
        return flow {
            val response = client.post(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(body)
            }

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                throw HttpException(response.status, errorBody)
            }

            val channel = response.bodyAsChannel()

            while (!channel.isClosedForRead) {
                val chunk = channel.readUTF8Line()
                if (chunk != null) {
                    if (chunk.startsWith("data: ")) {
                        // Server-Sent Events format (OpenAI, Claude)
                        val data = chunk.substring(6)
                        if (data == "[DONE]") {
                            break
                        }
                        emit(data)
                    } else if (chunk.trim().isNotEmpty()) {
                        // JSONL format (Ollama)
                        emit(chunk.trim())
                    }
                }
            }
        }
    }

    /**
     * Make PUT request
     */
    suspend fun put(
        url: String,
        body: String,
        headers: Map<String, String> = emptyMap()
    ): HttpResponse {
        return executeWithRetry {
            val response = client.put(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
                header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                setBody(body)
            }
            handleResponse(response)
        }
    }

    /**
     * Make DELETE request
     */
    suspend fun delete(
        url: String,
        headers: Map<String, String> = emptyMap()
    ): HttpResponse {
        return executeWithRetry {
            val response = client.delete(url) {
                headers.forEach { (key, value) ->
                    header(key, value)
                }
            }
            handleResponse(response)
        }
    }

    /**
     * Execute request with retry logic
     */
    private suspend fun <T> executeWithRetry(request: suspend () -> T): T {
        var lastException: Exception? = null
        var currentDelay = retryConfig.delayMs
        
        repeat(retryConfig.maxRetries + 1) { attempt ->
            try {
                return request()
            } catch (e: HttpException) {
                // Don't retry on client errors (4xx), only on server errors (5xx) and network issues
                if (e.statusCode.value in 400..499 && e.statusCode != HttpStatusCode.TooManyRequests) {
                    throw e
                }
                lastException = e
                
                // Handle rate limiting with Retry-After header
                if (e.statusCode == HttpStatusCode.TooManyRequests) {
                    // In a real implementation, we would parse the Retry-After header
                    // For now, we'll use the configured delay
                }
            } catch (e: Exception) {
                lastException = e
            }
            
            // Don't delay after the last attempt
            if (attempt < retryConfig.maxRetries) {
                delay(currentDelay)
                currentDelay = minOf(
                    (currentDelay * retryConfig.backoffMultiplier).toLong(),
                    retryConfig.maxDelayMs
                )
            }
        }
        
        throw lastException ?: Exception("Request failed after ${retryConfig.maxRetries} retries")
    }

    /**
     * Handle HTTP response and convert to our wrapper
     */
    private suspend fun handleResponse(response: io.ktor.client.statement.HttpResponse): HttpResponse {
        val body = response.bodyAsText()
        val headers = mutableMapOf<String, String>()
        response.headers.forEach { key, values ->
            headers[key] = values.firstOrNull() ?: ""
        }

        val httpResponse = HttpResponse(
            status = response.status,
            body = body,
            headers = headers
        )

        // Throw exception for error status codes
        if (!response.status.isSuccess()) {
            throw HttpException(response.status, body)
        }

        return httpResponse
    }

    /**
     * Close the HTTP client
     */
    fun close() {
        client.close()
    }
}
