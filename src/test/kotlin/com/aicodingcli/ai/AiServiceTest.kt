package com.aicodingcli.ai

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

class AiServiceTest {

    @Test
    fun `should create AI service with valid configuration`() = runTest {
        // Arrange
        val config = AiServiceConfig(
            provider = AiProvider.OPENAI,
            apiKey = "test-api-key",
            model = "gpt-3.5-turbo",
            baseUrl = "https://api.openai.com/v1",
            temperature = 0.7f,
            maxTokens = 1000
        )

        // Act & Assert
        assertDoesNotThrow {
            AiServiceFactory.createService(config)
        }
    }

    @Test
    fun `should throw exception for invalid API key`() = runTest {
        // Act & Assert
        assertThrows<IllegalArgumentException> {
            AiServiceConfig(
                provider = AiProvider.OPENAI,
                apiKey = "",
                model = "gpt-3.5-turbo"
            )
        }
    }

    @Test
    fun `should support multiple AI providers`() = runTest {
        // Arrange & Act & Assert
        val openaiConfig = AiServiceConfig(
            provider = AiProvider.OPENAI,
            apiKey = "test-key",
            model = "gpt-3.5-turbo"
        )
        val claudeConfig = AiServiceConfig(
            provider = AiProvider.CLAUDE,
            apiKey = "test-key",
            model = "claude-3-sonnet"
        )

        assertDoesNotThrow {
            AiServiceFactory.createService(openaiConfig)
            AiServiceFactory.createService(claudeConfig)
        }
    }

    @Test
    fun `should validate model parameters`() = runTest {
        // Act & Assert
        assertThrows<IllegalArgumentException> {
            AiServiceConfig(
                provider = AiProvider.OPENAI,
                apiKey = "test-key",
                model = "gpt-3.5-turbo",
                temperature = 2.0f // Invalid temperature > 1.0
            )
        }
    }
}
