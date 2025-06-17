package com.aicodingcli.ai

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import io.mockk.mockk
import io.mockk.coEvery

class AiRequestResponseTest {

    @Test
    fun `should create AI request with required fields`() {
        // Arrange & Act
        val request = AiRequest(
            messages = listOf(
                AiMessage(role = MessageRole.USER, content = "Hello, AI!")
            ),
            model = "gpt-3.5-turbo",
            temperature = 0.7f,
            maxTokens = 1000
        )

        // Assert
        assertEquals(1, request.messages.size)
        assertEquals("Hello, AI!", request.messages[0].content)
        assertEquals(MessageRole.USER, request.messages[0].role)
        assertEquals("gpt-3.5-turbo", request.model)
        assertEquals(0.7f, request.temperature)
        assertEquals(1000, request.maxTokens)
    }

    @Test
    fun `should create AI response with content`() {
        // Arrange & Act
        val response = AiResponse(
            content = "Hello, human!",
            model = "gpt-3.5-turbo",
            usage = TokenUsage(
                promptTokens = 10,
                completionTokens = 5,
                totalTokens = 15
            ),
            finishReason = FinishReason.STOP
        )

        // Assert
        assertEquals("Hello, human!", response.content)
        assertEquals("gpt-3.5-turbo", response.model)
        assertEquals(10, response.usage.promptTokens)
        assertEquals(5, response.usage.completionTokens)
        assertEquals(15, response.usage.totalTokens)
        assertEquals(FinishReason.STOP, response.finishReason)
    }

    @Test
    fun `should handle conversation with multiple messages`() {
        // Arrange
        val messages = listOf(
            AiMessage(role = MessageRole.SYSTEM, content = "You are a helpful assistant."),
            AiMessage(role = MessageRole.USER, content = "What is 2+2?"),
            AiMessage(role = MessageRole.ASSISTANT, content = "2+2 equals 4."),
            AiMessage(role = MessageRole.USER, content = "Thank you!")
        )

        // Act
        val request = AiRequest(
            messages = messages,
            model = "gpt-3.5-turbo"
        )

        // Assert
        assertEquals(4, request.messages.size)
        assertEquals(MessageRole.SYSTEM, request.messages[0].role)
        assertEquals(MessageRole.USER, request.messages[1].role)
        assertEquals(MessageRole.ASSISTANT, request.messages[2].role)
        assertEquals(MessageRole.USER, request.messages[3].role)
    }

    @Test
    fun `should validate message content is not empty`() {
        // Act & Assert
        assertThrows<IllegalArgumentException> {
            AiMessage(role = MessageRole.USER, content = "")
        }
    }

    @Test
    fun `should handle streaming response`() = runTest {
        // Arrange
        val mockService = mockk<AiService>()
        val request = AiRequest(
            messages = listOf(AiMessage(role = MessageRole.USER, content = "Hello")),
            model = "gpt-3.5-turbo"
        )

        // Mock streaming response
        coEvery { mockService.streamChat(request) } returns kotlinx.coroutines.flow.flowOf(
            AiStreamChunk(content = "Hello", finishReason = null),
            AiStreamChunk(content = " there", finishReason = null),
            AiStreamChunk(content = "!", finishReason = FinishReason.STOP)
        )

        // Act
        val chunks = mutableListOf<AiStreamChunk>()
        mockService.streamChat(request).collect { chunk ->
            chunks.add(chunk)
        }

        // Assert
        assertEquals(3, chunks.size)
        assertEquals("Hello", chunks[0].content)
        assertEquals(" there", chunks[1].content)
        assertEquals("!", chunks[2].content)
        assertEquals(FinishReason.STOP, chunks[2].finishReason)
    }
}
