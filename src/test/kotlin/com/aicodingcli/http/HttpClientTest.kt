package com.aicodingcli.http

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class HttpClientTest {

    @Test
    fun `should make successful GET request`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""{"message": "success"}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = AiHttpClient(mockEngine)

        // Act
        val response = httpClient.get("https://api.example.com/test")

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"message": "success"}""", response.body)
        assertEquals("application/json", response.headers["Content-Type"])
    }

    @Test
    fun `should make successful POST request with JSON body`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("application/json", request.body.contentType?.toString())
            respond(
                content = ByteReadChannel("""{"id": "123", "status": "created"}"""),
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = AiHttpClient(mockEngine)
        val requestBody = """{"name": "test", "value": "data"}"""

        // Act
        val response = httpClient.post(
            url = "https://api.example.com/create",
            body = requestBody,
            headers = mapOf("Authorization" to "Bearer token123")
        )

        // Assert
        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals("""{"id": "123", "status": "created"}""", response.body)
    }

    @Test
    fun `should handle HTTP error responses`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""{"error": "Not found"}"""),
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = AiHttpClient(mockEngine)

        // Act & Assert
        assertThrows<HttpException> {
            httpClient.get("https://api.example.com/notfound")
        }
    }

    @Test
    fun `should retry on network errors`() = runTest {
        // Arrange
        var callCount = 0
        val mockEngine = MockEngine { request ->
            callCount++
            if (callCount < 3) {
                throw Exception("Network error")
            }
            respond(
                content = ByteReadChannel("""{"message": "success after retry"}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = AiHttpClient(mockEngine, retryConfig = RetryConfig(maxRetries = 3, delayMs = 10))

        // Act
        val response = httpClient.get("https://api.example.com/test")

        // Assert
        assertEquals(3, callCount)
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("""{"message": "success after retry"}""", response.body)
    }

    @Test
    fun `should fail after max retries exceeded`() = runTest {
        // Arrange
        var callCount = 0
        val mockEngine = MockEngine { request ->
            callCount++
            throw Exception("Persistent network error")
        }
        val httpClient = AiHttpClient(mockEngine, retryConfig = RetryConfig(maxRetries = 2, delayMs = 10))

        // Act & Assert
        assertThrows<Exception> {
            httpClient.get("https://api.example.com/test")
        }
        assertEquals(3, callCount) // Initial call + 2 retries
    }

    @Test
    fun `should handle timeout`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            // Simulate slow response
            kotlinx.coroutines.delay(2000)
            respond(
                content = ByteReadChannel("""{"message": "slow response"}"""),
                status = HttpStatusCode.OK
            )
        }
        val httpClient = AiHttpClient(mockEngine, timeoutMs = 100)

        // Act & Assert
        assertThrows<Exception> {
            httpClient.get("https://api.example.com/slow")
        }
    }

    @Test
    fun `should add custom headers`() = runTest {
        // Arrange
        val mockEngine = MockEngine { request ->
            assertEquals("Bearer token123", request.headers["Authorization"])
            assertEquals("application/json", request.headers["Content-Type"])
            assertEquals("MyApp/1.0", request.headers["User-Agent"])
            respond(
                content = ByteReadChannel("""{"message": "success"}"""),
                status = HttpStatusCode.OK
            )
        }
        val httpClient = AiHttpClient(mockEngine)

        // Act
        val response = httpClient.get(
            url = "https://api.example.com/test",
            headers = mapOf(
                "Authorization" to "Bearer token123",
                "Content-Type" to "application/json",
                "User-Agent" to "MyApp/1.0"
            )
        )

        // Assert
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `should handle rate limiting with retry after`() = runTest {
        // Arrange
        var callCount = 0
        val mockEngine = MockEngine { request ->
            callCount++
            if (callCount == 1) {
                respond(
                    content = ByteReadChannel("""{"error": "Rate limited"}"""),
                    status = HttpStatusCode.TooManyRequests,
                    headers = headersOf("Retry-After", "1")
                )
            } else {
                respond(
                    content = ByteReadChannel("""{"message": "success"}"""),
                    status = HttpStatusCode.OK
                )
            }
        }
        val httpClient = AiHttpClient(mockEngine, retryConfig = RetryConfig(maxRetries = 2, delayMs = 100))

        // Act
        val response = httpClient.get("https://api.example.com/test")

        // Assert
        assertEquals(2, callCount)
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
