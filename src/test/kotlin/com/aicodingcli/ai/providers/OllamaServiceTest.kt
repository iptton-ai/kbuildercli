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

class OllamaServiceTest {

    private lateinit var mockHttpClient: AiHttpClient
    private lateinit var ollamaService: OllamaService
    private lateinit var config: AiServiceConfig

    @BeforeEach
    fun setUp() {
        mockHttpClient = mockk()
        config = AiServiceConfig(
            provider = AiProvider.OLLAMA,
            apiKey = "not-required", // Ollama doesn't require API key but config validation needs non-empty string
            model = "llama2",
            baseUrl = "http://localhost:11434",
            temperature = 0.7f,
            maxTokens = 1000
        )
        ollamaService = OllamaService(config, mockHttpClient)
    }

    @Test
    fun `should make successful chat request`() = runTest {
        // Arrange
        val request = AiRequest(
            messages = listOf(
                AiMessage(role = MessageRole.USER, content = "Hello, Ollama!")
            ),
            model = "llama2",
            temperature = 0.7f,
            maxTokens = 1000
        )

        val mockResponse = HttpResponse(
            status = HttpStatusCode.OK,
            body = """
            {
                "model": "llama2",
                "created_at": "2023-12-12T14:13:43.416799Z",
                "message": {
                    "role": "assistant",
                    "content": "Hello! How can I help you today?"
                },
                "done": true,
                "total_duration": 5191566416,
                "load_duration": 2154458,
                "prompt_eval_count": 26,
                "prompt_eval_duration": 383809000,
                "eval_count": 298,
                "eval_duration": 4799921000
            }
            """.trimIndent(),
            headers = mapOf("Content-Type" to "application/json")
        )

        coEvery { 
            mockHttpClient.post(
                url = "http://localhost:11434/api/chat",
                body = any(),
                headers = any()
            )
        } returns mockResponse

        // Act
        val response = ollamaService.chat(request)

        // Assert
        assertEquals("Hello! How can I help you today?", response.content)
        assertEquals("llama2", response.model)
        assertEquals(26, response.usage.promptTokens)
        assertEquals(298, response.usage.completionTokens)
        assertEquals(324, response.usage.totalTokens)
        assertEquals(FinishReason.STOP, response.finishReason)

        // Verify HTTP call
        coVerify {
            mockHttpClient.post(
                url = "http://localhost:11434/api/chat",
                body = match { body ->
                    body.contains("\"model\":\"llama2\"") &&
                    body.contains("\"stream\":false") &&
                    body.contains("\"messages\":[{\"role\":\"user\",\"content\":\"Hello, Ollama!\"}]")
                },
                headers = match { headers ->
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
            model = "llama2"
        )

        coEvery { 
            mockHttpClient.post(any(), any(), any())
        } throws com.aicodingcli.http.HttpException(
            statusCode = HttpStatusCode.NotFound,
            responseBody = """{"error": "model 'nonexistent' not found"}"""
        )

        // Act & Assert
        assertThrows<OllamaException> {
            ollamaService.chat(request)
        }
    }

    @Test
    fun `should test connection successfully`() = runTest {
        // Arrange
        val mockResponse = HttpResponse(
            status = HttpStatusCode.OK,
            body = """
            {
                "models": [
                    {
                        "name": "llama2:latest",
                        "model": "llama2:latest",
                        "size": 3825819519,
                        "digest": "fe938a131f40e6f6d40083c9f0f430a515233eb2edaa6d72eb85c50d64f2300e",
                        "details": {
                            "format": "gguf",
                            "family": "llama",
                            "parameter_size": "7B",
                            "quantization_level": "Q4_0"
                        }
                    }
                ]
            }
            """.trimIndent(),
            headers = mapOf("Content-Type" to "application/json")
        )

        coEvery { 
            mockHttpClient.get(
                url = "http://localhost:11434/api/tags",
                headers = any()
            )
        } returns mockResponse

        // Act
        val result = ollamaService.testConnection()

        // Assert
        assertTrue(result)
    }

    @Test
    fun `should fail connection test on error`() = runTest {
        // Arrange
        coEvery { 
            mockHttpClient.get(any(), any())
        } throws com.aicodingcli.http.HttpException(
            statusCode = HttpStatusCode.ServiceUnavailable,
            responseBody = """{"error": "Ollama server not available"}"""
        )

        // Act
        val result = ollamaService.testConnection()

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
            model = "llama2",
            stream = true
        )

        // For now, we'll test that the method exists and returns a flow
        // In a real implementation, we would mock the streaming response
        
        // Act
        val flow = ollamaService.streamChat(request)

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
                model = "llama2"
            )
        }
    }

    @Test
    fun `should use custom base URL if provided`() = runTest {
        // Arrange
        val customConfig = config.copy(baseUrl = "http://custom-ollama:11434")
        val customService = OllamaService(customConfig, mockHttpClient)
        
        val request = AiRequest(
            messages = listOf(AiMessage(role = MessageRole.USER, content = "Hello")),
            model = "llama2"
        )

        val mockResponse = HttpResponse(
            status = HttpStatusCode.OK,
            body = """
            {
                "model": "llama2",
                "created_at": "2023-12-12T14:13:43.416799Z",
                "message": {
                    "role": "assistant",
                    "content": "Hi"
                },
                "done": true,
                "total_duration": 1000000,
                "eval_count": 1,
                "prompt_eval_count": 1
            }
            """.trimIndent(),
            headers = mapOf()
        )

        coEvery { 
            mockHttpClient.post(
                url = "http://custom-ollama:11434/api/chat",
                body = any(),
                headers = any()
            )
        } returns mockResponse

        // Act
        customService.chat(request)

        // Assert
        coVerify {
            mockHttpClient.post(
                url = "http://custom-ollama:11434/api/chat",
                body = any(),
                headers = any()
            )
        }
    }

    @Test
    fun `should handle temperature and max tokens options`() = runTest {
        // Arrange
        val request = AiRequest(
            messages = listOf(AiMessage(role = MessageRole.USER, content = "Hello")),
            model = "llama2",
            temperature = 0.5f,
            maxTokens = 500
        )

        val mockResponse = HttpResponse(
            status = HttpStatusCode.OK,
            body = """
            {
                "model": "llama2",
                "created_at": "2023-12-12T14:13:43.416799Z",
                "message": {
                    "role": "assistant",
                    "content": "Hello there!"
                },
                "done": true,
                "eval_count": 2,
                "prompt_eval_count": 1
            }
            """.trimIndent(),
            headers = mapOf()
        )

        coEvery { 
            mockHttpClient.post(any(), any(), any())
        } returns mockResponse

        // Act
        ollamaService.chat(request)

        // Assert
        coVerify {
            mockHttpClient.post(
                url = any(),
                body = match { body ->
                    body.contains("\"temperature\":0.5") &&
                    body.contains("\"num_predict\":500")
                },
                headers = any()
            )
        }
    }
}
