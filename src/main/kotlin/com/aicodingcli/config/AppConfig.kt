package com.aicodingcli.config

import com.aicodingcli.ai.AiProvider
import com.aicodingcli.ai.AiServiceConfig
import kotlinx.serialization.Serializable

/**
 * Application configuration
 */
@Serializable
data class AppConfig(
    val defaultProvider: AiProvider = AiProvider.OPENAI,
    val providers: Map<AiProvider, AiServiceConfig> = emptyMap()
) {
    /**
     * Get configuration for the default provider
     */
    fun getDefaultProviderConfig(): AiServiceConfig? {
        return providers[defaultProvider]
    }

    /**
     * Get configuration for a specific provider
     */
    fun getProviderConfig(provider: AiProvider): AiServiceConfig? {
        return providers[provider]
    }

    /**
     * Check if a provider is configured
     */
    fun hasProvider(provider: AiProvider): Boolean {
        return providers.containsKey(provider)
    }

    /**
     * Get all configured providers
     */
    fun getConfiguredProviders(): Set<AiProvider> {
        return providers.keys
    }
}
