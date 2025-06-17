package com.aicodingcli.ai.providers

import com.aicodingcli.ai.*
import com.aicodingcli.http.AiHttpClient
import com.aicodingcli.http.HttpResponse
import io.ktor.http.*
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows

class ClaudeServiceTest {

    private lateinit var mockHttpClient: AiHttpClient
    private lateinit var claudeService: ClaudeService
    private lateinit var config: AiServiceConfig

    @BeforeEach
    fun setUp() {
        mockHttpClient = mockk()
        config = AiServiceConfig(
            provider = AiProvider.CLAUDE,
            apiKey = "test-api-key",
            model = "claude-3-sonnet-20240229",
            baseUrl = "https://api.anthropic.com/v1",
            temperature = 0.7f,
            maxTokens = 1000
        )
        claudeService = ClaudeService(config, mockHttpClient)
    }

    @Test
    fun `should make successful chat request`() = runTest {
        // Arrange
        val request = AiRequest(
            messages = listOf(
                AiMessage(role = MessageRole.USER, content = "Hello, Claude!")
            ),
            model = "claude-3-sonnet-20240229",
            temperature = 0.7f,
            maxTokens = 1000
        )

        val mockResponse = HttpResponse(
            status = HttpStatusCode.OK,
            body = """
            {
                "id": "msg_01XFDUDYJgAACzvnptvVoYEL",
                "type": "message",
                "role": "assistant",
                "content": [
                    {
                        "type": "text",
                        "text": "Hello! How can I help you today?"
                    }
                ],
                "model": "claude-3-sonnet-20240229",
                "stop_reason": "end_turn",
                "stop_sequence": null,
                "usage": {
                    "input_tokens": 10,
                    "output_tokens": 15
                }
            }
            """.trimIndent(),
            headers = mapOf("Content-Type" to "application/json")
        )

        coEvery { 
            mockHttpClient.post(
                url = "https://api.anthropic.com/v1/messages",
                body = any(),
                headers = any()
            )
        } returns mockResponse

        // Act
        val response = claudeService.chat(request)

        // Assert
        assertEquals("Hello! How can I help you today?", response.content)
        assertEquals("claude-3-sonnet-20240229", response.model)
        assertEquals(10, response.usage.promptTokens)
        assertEquals(15, response.usage.completionTokens)
        assertEquals(25, response.usage.totalTokens)
        assertEquals(FinishReason.STOP, response.finishReason)

        // Verify HTTP call
        coVerify {
            mockHttpClient.post(
                url = "https://api.anthropic.com/v1/messages",
                body = match { body ->
                    body.contains("\"model\":\"claude-3-sonnet-20240229\"") &&
                    body.contains("\"temperature\":0.7") &&
                    body.contains("\"max_tokens\":1000") &&
                    body.contains("\"messages\":[{\"role\":\"user\",\"content\":\"Hello, Claude!\"}]")
                },
                headers = match { headers ->
                    headers["x-api-key"] == "test-api-key" &&
                    headers["Content-Type"] == "application/json" &&
                    headers["anthropic-version"] == "2023-06-01"
                }
            )
        }
    }

    @Test
    fun `should handle API error response`() = runTest {
        // Arrange
        val request = AiRequest(
            messages = listOf(
                AiMessage(role = MessageRole.USER, content = "Hello")
            ),
            model = "claude-3-sonnet-20240229"
        )

        coEvery { 
            mockHttpClient.post(any(), any(), any())
        } throws com.aicodingcli.http.HttpException(
            statusCode = HttpStatusCode.Unauthorized,
            responseBody = """{"type": "error", "error": {"type": "authentication_error", "message": "Invalid API key"}}"""
        )

        // Act & Assert
        assertThrows<ClaudeException> {
            claudeService.chat(request)
        }
    }

    @Test
    fun `should test connection successfully`() = runTest {
        // Arrange
        val mockResponse = HttpResponse(
            status = HttpStatusCode.OK,
            body = """
            {
                "id": "msg_test",
                "type": "message",
                "role": "assistant",
                "content": [
                    {
                        "type": "text",
                        "text": "Connection test successful"
                    }
                ],
                "model": "claude-3-sonnet-20240229",
                "stop_reason": "end_turn",
                "stop_sequence": null,
                "usage": {
                    "input_tokens": 5,
                    "output_tokens": 5
                }
            }
            """.trimIndent(),
            headers = mapOf("Content-Type" to "application/json")
        )

        coEvery { 
            mockHttpClient.post(
                url = "https://api.anthropic.com/v1/messages",
                body = any(),
                headers = any()
            )
        } returns mockResponse

        // Act
        val result = claudeService.testConnection()

        // Assert
        assertTrue(result)
    }

    @Test
    fun `should fail connection test on error`() = runTest {
        // Arrange
        coEvery { 
            mockHttpClient.post(any(), any(), any())
        } throws com.aicodingcli.http.HttpException(
            statusCode = HttpStatusCode.Unauthorized,
            responseBody = """{"type": "error", "error": {"type": "authentication_error", "message": "Invalid API key"}}"""
        )

        // Act
        val result = claudeService.testConnection()

        // Assert
        assertFalse(result)
    }

    @Test
    fun `should handle streaming chat request`() = runTest {
        // Arrange
        val request = AiRequest(
            messages = listOf(
                AiMessage(role = MessageRole.USER, content = "Tell me a story")
            ),
            model = "claude-3-sonnet-20240229",
            stream = true
        )

        // For now, we'll test that the method exists and returns a flow
        // In a real implementation, we would mock the streaming response
        
        // Act
        val flow = claudeService.streamChat(request)

        // Assert
        assertNotNull(flow)
        // Note: Full streaming implementation would require more complex mocking
        // This is a placeholder test to ensure the interface is implemented
    }

    @Test
    fun `should validate request before sending`() = runTest {
        // Act & Assert
        assertThrows<IllegalArgumentException> {
            // This should fail during AiRequest construction due to empty messages
            AiRequest(
                messages = emptyList(),
                model = "claude-3-sonnet-20240229"
            )
        }
    }

    @Test
    fun `should use custom base URL if provided`() = runTest {
        // Arrange
        val customConfig = config.copy(baseUrl = "https://custom.anthropic.com/v1")
        val customService = ClaudeService(customConfig, mockHttpClient)
        
        val request = AiRequest(
            messages = listOf(AiMessage(role = MessageRole.USER, content = "Hello")),
            model = "claude-3-sonnet-20240229"
        )

        val mockResponse = HttpResponse(
            status = HttpStatusCode.OK,
            body = """
            {
                "id": "msg_test",
                "type": "message",
                "role": "assistant",
                "content": [
                    {
                        "type": "text",
                        "text": "Hi"
                    }
                ],
                "model": "claude-3-sonnet-20240229",
                "stop_reason": "end_turn",
                "stop_sequence": null,
                "usage": {
                    "input_tokens": 1,
                    "output_tokens": 1
                }
            }
            """.trimIndent(),
            headers = mapOf()
        )

        coEvery { 
            mockHttpClient.post(
                url = "https://custom.anthropic.com/v1/messages",
                body = any(),
                headers = any()
            )
        } returns mockResponse

        // Act
        customService.chat(request)

        // Assert
        coVerify {
            mockHttpClient.post(
                url = "https://custom.anthropic.com/v1/messages",
                body = any(),
                headers = any()
            )
        }
    }
}
