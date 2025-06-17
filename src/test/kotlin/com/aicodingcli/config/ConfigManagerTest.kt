package com.aicodingcli.config

import com.aicodingcli.ai.AiProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ConfigManagerTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var configManager: ConfigManager

    @BeforeEach
    fun setUp() {
        configManager = ConfigManager(tempDir.absolutePath)
    }

    @Test
    fun `should create default configuration file if not exists`() = runTest {
        // Act
        val config = configManager.loadConfig()

        // Assert
        assertNotNull(config)
        assertEquals(AiProvider.OPENAI, config.defaultProvider)
        assertTrue(config.providers.isNotEmpty())
        assertTrue(File(tempDir, "config.json").exists())
    }

    @Test
    fun `should load existing configuration file`() = runTest {
        // Arrange
        val configFile = File(tempDir, "config.json")
        val configContent = """
        {
            "defaultProvider": "CLAUDE",
            "providers": {
                "OPENAI": {
                    "provider": "OPENAI",
                    "apiKey": "test-openai-key",
                    "model": "gpt-4",
                    "baseUrl": "https://api.openai.com/v1",
                    "temperature": 0.8,
                    "maxTokens": 2000,
                    "timeout": 60000
                },
                "CLAUDE": {
                    "provider": "CLAUDE",
                    "apiKey": "test-claude-key",
                    "model": "claude-3-sonnet",
                    "baseUrl": "https://api.anthropic.com",
                    "temperature": 0.7,
                    "maxTokens": 1500,
                    "timeout": 45000
                }
            }
        }
        """.trimIndent()
        configFile.writeText(configContent)

        // Act
        val config = configManager.loadConfig()

        // Assert
        assertEquals(AiProvider.CLAUDE, config.defaultProvider)
        assertEquals(2, config.providers.size)
        
        val openaiConfig = config.providers[AiProvider.OPENAI]
        assertNotNull(openaiConfig)
        assertEquals("test-openai-key", openaiConfig!!.apiKey)
        assertEquals("gpt-4", openaiConfig.model)
        assertEquals(0.8f, openaiConfig.temperature)
        
        val claudeConfig = config.providers[AiProvider.CLAUDE]
        assertNotNull(claudeConfig)
        assertEquals("test-claude-key", claudeConfig!!.apiKey)
        assertEquals("claude-3-sonnet", claudeConfig.model)
    }

    @Test
    fun `should save configuration to file`() = runTest {
        // Arrange
        val config = AppConfig(
            defaultProvider = AiProvider.GEMINI,
            providers = mapOf(
                AiProvider.GEMINI to com.aicodingcli.ai.AiServiceConfig(
                    provider = AiProvider.GEMINI,
                    apiKey = "test-gemini-key",
                    model = "gemini-pro",
                    temperature = 0.5f,
                    maxTokens = 1000
                )
            )
        )

        // Act
        configManager.saveConfig(config)

        // Assert
        val configFile = File(tempDir, "config.json")
        assertTrue(configFile.exists())
        
        val savedConfig = configManager.loadConfig()
        assertEquals(AiProvider.GEMINI, savedConfig.defaultProvider)
        assertEquals(1, savedConfig.providers.size)
        
        val geminiConfig = savedConfig.providers[AiProvider.GEMINI]
        assertNotNull(geminiConfig)
        assertEquals("test-gemini-key", geminiConfig!!.apiKey)
        assertEquals("gemini-pro", geminiConfig.model)
        assertEquals(0.5f, geminiConfig.temperature)
    }

    @Test
    fun `should get current provider configuration`() = runTest {
        // Arrange
        val config = AppConfig(
            defaultProvider = AiProvider.OPENAI,
            providers = mapOf(
                AiProvider.OPENAI to com.aicodingcli.ai.AiServiceConfig(
                    provider = AiProvider.OPENAI,
                    apiKey = "test-key",
                    model = "gpt-3.5-turbo"
                )
            )
        )
        configManager.saveConfig(config)

        // Act
        val currentConfig = configManager.getCurrentProviderConfig()

        // Assert
        assertNotNull(currentConfig)
        assertEquals(AiProvider.OPENAI, currentConfig.provider)
        assertEquals("test-key", currentConfig.apiKey)
        assertEquals("gpt-3.5-turbo", currentConfig.model)
    }

    @Test
    fun `should update provider configuration`() = runTest {
        // Arrange
        configManager.loadConfig() // Create default config
        val newConfig = com.aicodingcli.ai.AiServiceConfig(
            provider = AiProvider.OPENAI,
            apiKey = "updated-key",
            model = "gpt-4",
            temperature = 0.9f
        )

        // Act
        configManager.updateProviderConfig(AiProvider.OPENAI, newConfig)

        // Assert
        val updatedConfig = configManager.getCurrentProviderConfig()
        assertEquals("updated-key", updatedConfig.apiKey)
        assertEquals("gpt-4", updatedConfig.model)
        assertEquals(0.9f, updatedConfig.temperature)
    }

    @Test
    fun `should switch default provider`() = runTest {
        // Arrange
        configManager.loadConfig() // Create default config
        configManager.updateProviderConfig(
            AiProvider.CLAUDE,
            com.aicodingcli.ai.AiServiceConfig(
                provider = AiProvider.CLAUDE,
                apiKey = "claude-key",
                model = "claude-3-sonnet"
            )
        )

        // Act
        configManager.setDefaultProvider(AiProvider.CLAUDE)

        // Assert
        val config = configManager.loadConfig()
        assertEquals(AiProvider.CLAUDE, config.defaultProvider)
        
        val currentConfig = configManager.getCurrentProviderConfig()
        assertEquals(AiProvider.CLAUDE, currentConfig.provider)
        assertEquals("claude-key", currentConfig.apiKey)
    }
}
