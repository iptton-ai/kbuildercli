package com.aicodingcli.http

import io.ktor.http.*

/**
 * HTTP response wrapper
 */
data class HttpResponse(
    val status: HttpStatusCode,
    val body: String,
    val headers: Map<String, String>
)

/**
 * HTTP exception for error responses
 */
class HttpException(
    val statusCode: HttpStatusCode,
    val responseBody: String,
    message: String = "HTTP error ${statusCode.value}: ${statusCode.description}"
) : Exception(message)

/**
 * Retry configuration for HTTP requests
 */
data class RetryConfig(
    val maxRetries: Int = 3,
    val delayMs: Long = 1000,
    val backoffMultiplier: Double = 2.0,
    val maxDelayMs: Long = 30000
) {
    init {
        require(maxRetries >= 0) { "Max retries must be non-negative" }
        require(delayMs > 0) { "Delay must be positive" }
        require(backoffMultiplier >= 1.0) { "Backoff multiplier must be >= 1.0" }
        require(maxDelayMs > 0) { "Max delay must be positive" }
    }
}

/**
 * HTTP request configuration
 */
data class RequestConfig(
    val timeoutMs: Long = 30000,
    val retryConfig: RetryConfig = RetryConfig(),
    val followRedirects: Boolean = true,
    val userAgent: String = "AiCodingCli/1.0"
) {
    init {
        require(timeoutMs > 0) { "Timeout must be positive" }
    }
}
