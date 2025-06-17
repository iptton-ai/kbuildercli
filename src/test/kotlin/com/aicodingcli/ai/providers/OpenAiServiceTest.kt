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

class OpenAiServiceTest {

    private lateinit var mockHttpClient: AiHttpClient
    private lateinit var openAiService: OpenAiService
    private lateinit var config: AiServiceConfig

    @BeforeEach
    fun setUp() {
        mockHttpClient = mockk()
        config = AiServiceConfig(
            provider = AiProvider.OPENAI,
            apiKey = "test-api-key",
            model = "gpt-3.5-turbo",
            baseUrl = "https://api.openai.com/v1",
            temperature = 0.7f,
            maxTokens = 1000
        )
        openAiService = OpenAiService(config, mockHttpClient)
    }

    @Test
    fun `should make successful chat request`() = runTest {
        // Arrange
        val request = AiRequest(
            messages = listOf(
                AiMessage(role = MessageRole.USER, content = "Hello, AI!")
            ),
            model = "gpt-3.5-turbo",
            temperature = 0.7f,
            maxTokens = 1000
        )

        val mockResponse = HttpResponse(
            status = HttpStatusCode.OK,
            body = """
            {
                "id": "chatcmpl-123",
                "object": "chat.completion",
                "created": 1677652288,
                "model": "gpt-3.5-turbo",
                "choices": [
                    {
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": "Hello! How can I help you today?"
                        },
                        "finish_reason": "stop"
                    }
                ],
                "usage": {
                    "prompt_tokens": 9,
                    "completion_tokens": 12,
                    "total_tokens": 21
                }
            }
            """.trimIndent(),
            headers = mapOf("Content-Type" to "application/json")
        )

        coEvery { 
            mockHttpClient.post(
                url = "https://api.openai.com/v1/chat/completions",
                body = any(),
                headers = any()
            )
        } returns mockResponse

        // Act
        val response = openAiService.chat(request)

        // Assert
        assertEquals("Hello! How can I help you today?", response.content)
        assertEquals("gpt-3.5-turbo", response.model)
        assertEquals(9, response.usage.promptTokens)
        assertEquals(12, response.usage.completionTokens)
        assertEquals(21, response.usage.totalTokens)
        assertEquals(FinishReason.STOP, response.finishReason)

        // Verify HTTP call
        coVerify {
            mockHttpClient.post(
                url = "https://api.openai.com/v1/chat/completions",
                body = match { body ->
                    body.contains("\"model\":\"gpt-3.5-turbo\"") &&
                    body.contains("\"temperature\":0.7") &&
                    body.contains("\"max_tokens\":1000") &&
                    body.contains("\"messages\":[{\"role\":\"user\",\"content\":\"Hello, AI!\"}]")
                },
                headers = match { headers ->
                    headers["Authorization"] == "Bearer test-api-key" &&
                    headers["Content-Type"] == "application/json"
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
            model = "gpt-3.5-turbo"
        )

        coEvery { 
            mockHttpClient.post(any(), any(), any())
        } throws com.aicodingcli.http.HttpException(
            statusCode = HttpStatusCode.Unauthorized,
            responseBody = """{"error": {"message": "Invalid API key", "type": "invalid_request_error"}}"""
        )

        // Act & Assert
        assertThrows<OpenAiException> {
            openAiService.chat(request)
        }
    }

    @Test
    fun `should test connection successfully`() = runTest {
        // Arrange
        val mockResponse = HttpResponse(
            status = HttpStatusCode.OK,
            body = """{"object": "list", "data": [{"id": "gpt-3.5-turbo", "object": "model", "created": 1677610602, "owned_by": "openai"}]}""",
            headers = mapOf("Content-Type" to "application/json")
        )

        coEvery { 
            mockHttpClient.get(
                url = "https://api.openai.com/v1/models",
                headers = any()
            )
        } returns mockResponse

        // Act
        val result = openAiService.testConnection()

        // Assert
        assertTrue(result)
    }

    @Test
    fun `should fail connection test on error`() = runTest {
        // Arrange
        coEvery { 
            mockHttpClient.get(any(), any())
        } throws com.aicodingcli.http.HttpException(
            statusCode = HttpStatusCode.Unauthorized,
            responseBody = """{"error": {"message": "Invalid API key"}}"""
        )

        // Act
        val result = openAiService.testConnection()

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
            model = "gpt-3.5-turbo",
            stream = true
        )

        // For now, we'll test that the method exists and returns a flow
        // In a real implementation, we would mock the streaming response
        
        // Act
        val flow = openAiService.streamChat(request)

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
                model = "gpt-3.5-turbo"
            )
        }
    }

    @Test
    fun `should use custom base URL if provided`() = runTest {
        // Arrange
        val customConfig = config.copy(baseUrl = "https://custom.openai.com/v1")
        val customService = OpenAiService(customConfig, mockHttpClient)
        
        val request = AiRequest(
            messages = listOf(AiMessage(role = MessageRole.USER, content = "Hello")),
            model = "gpt-3.5-turbo"
        )

        val mockResponse = HttpResponse(
            status = HttpStatusCode.OK,
            body = """
            {
                "id": "chatcmpl-test",
                "object": "chat.completion",
                "created": 1677652288,
                "model": "gpt-3.5-turbo",
                "choices": [
                    {
                        "index": 0,
                        "message": {
                            "role": "assistant",
                            "content": "Hi"
                        },
                        "finish_reason": "stop"
                    }
                ],
                "usage": {
                    "prompt_tokens": 1,
                    "completion_tokens": 1,
                    "total_tokens": 2
                }
            }
            """.trimIndent(),
            headers = mapOf()
        )

        coEvery { 
            mockHttpClient.post(
                url = "https://custom.openai.com/v1/chat/completions",
                body = any(),
                headers = any()
            )
        } returns mockResponse

        // Act
        customService.chat(request)

        // Assert
        coVerify {
            mockHttpClient.post(
                url = "https://custom.openai.com/v1/chat/completions",
                body = any(),
                headers = any()
            )
        }
    }
}
