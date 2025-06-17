package com.aicodingcli.config

import com.aicodingcli.ai.AiProvider
import com.aicodingcli.ai.AiServiceConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.io.IOException

/**
 * Configuration manager for handling application configuration
 */
class ConfigManager(private val configDir: String = System.getProperty("user.home") + "/.aicodingcli") {
    
    private val configFile = File(configDir, "config.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private var currentConfig: AppConfig? = null

    /**
     * Load configuration from file or create default if not exists
     */
    suspend fun loadConfig(): AppConfig {
        return try {
            if (configFile.exists()) {
                val configContent = configFile.readText()
                val config = json.decodeFromString<AppConfig>(configContent)
                currentConfig = config
                config
            } else {
                val defaultConfig = createDefaultConfig()
                saveConfig(defaultConfig)
                currentConfig = defaultConfig
                defaultConfig
            }
        } catch (e: Exception) {
            throw IOException("Failed to load configuration: ${e.message}", e)
        }
    }

    /**
     * Save configuration to file
     */
    suspend fun saveConfig(config: AppConfig) {
        try {
            // Ensure config directory exists
            if (!File(configDir).exists()) {
                File(configDir).mkdirs()
            }
            
            val configContent = json.encodeToString(config)
            configFile.writeText(configContent)
            currentConfig = config
        } catch (e: Exception) {
            throw IOException("Failed to save configuration: ${e.message}", e)
        }
    }

    /**
     * Get current provider configuration
     */
    suspend fun getCurrentProviderConfig(): AiServiceConfig {
        val config = currentConfig ?: loadConfig()
        return config.getDefaultProviderConfig()
            ?: throw IllegalStateException("No configuration found for default provider: ${config.defaultProvider}")
    }

    /**
     * Update provider configuration
     */
    suspend fun updateProviderConfig(provider: AiProvider, serviceConfig: AiServiceConfig) {
        val config = currentConfig ?: loadConfig()
        val updatedProviders = config.providers.toMutableMap()
        updatedProviders[provider] = serviceConfig
        
        val updatedConfig = config.copy(providers = updatedProviders)
        saveConfig(updatedConfig)
    }

    /**
     * Set default provider
     */
    suspend fun setDefaultProvider(provider: AiProvider) {
        val config = currentConfig ?: loadConfig()
        
        // Ensure the provider is configured
        if (!config.hasProvider(provider)) {
            throw IllegalArgumentException("Provider $provider is not configured")
        }
        
        val updatedConfig = config.copy(defaultProvider = provider)
        saveConfig(updatedConfig)
    }

    /**
     * Remove provider configuration
     */
    suspend fun removeProvider(provider: AiProvider) {
        val config = currentConfig ?: loadConfig()
        
        if (config.defaultProvider == provider) {
            throw IllegalArgumentException("Cannot remove default provider. Set another provider as default first.")
        }
        
        val updatedProviders = config.providers.toMutableMap()
        updatedProviders.remove(provider)
        
        val updatedConfig = config.copy(providers = updatedProviders)
        saveConfig(updatedConfig)
    }

    /**
     * Get all configured providers
     */
    suspend fun getConfiguredProviders(): Set<AiProvider> {
        val config = currentConfig ?: loadConfig()
        return config.getConfiguredProviders()
    }

    /**
     * Check if provider is configured
     */
    suspend fun hasProvider(provider: AiProvider): Boolean {
        val config = currentConfig ?: loadConfig()
        return config.hasProvider(provider)
    }

    /**
     * Create default configuration
     */
    private fun createDefaultConfig(): AppConfig {
        val defaultProviders = mapOf(
            AiProvider.OPENAI to AiServiceConfig(
                provider = AiProvider.OPENAI,
                apiKey = "your-openai-api-key",
                model = "gpt-3.5-turbo",
                baseUrl = "https://api.openai.com/v1",
                temperature = 0.7f,
                maxTokens = 1000,
                timeout = 30000L
            )
        )
        
        return AppConfig(
            defaultProvider = AiProvider.OPENAI,
            providers = defaultProviders
        )
    }
}
