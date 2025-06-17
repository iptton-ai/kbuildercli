package com.aicodingcli.plugins

import com.aicodingcli.ai.AiService
import com.aicodingcli.ai.AiServiceConfig
import com.aicodingcli.ai.AiProvider

/**
 * Plugin that provides AI service implementations
 */
interface AiServicePlugin : Plugin {
    /**
     * The AI provider this plugin supports
     */
    val supportedProvider: AiProvider
    
    /**
     * Create an AI service instance with the given configuration
     */
    fun createAiService(config: AiServiceConfig): AiService
    
    /**
     * Validate the configuration for this AI service
     */
    fun validateConfig(config: AiServiceConfig): PluginValidationResult {
        return try {
            // Basic validation - check if provider matches
            if (config.provider != supportedProvider) {
                return PluginValidationResult(
                    isValid = false,
                    errors = listOf("Provider mismatch: expected $supportedProvider, got ${config.provider}")
                )
            }
            
            // Perform provider-specific validation
            validateProviderConfig(config)
        } catch (e: Exception) {
            PluginValidationResult(
                isValid = false,
                errors = listOf("Configuration validation failed: ${e.message}")
            )
        }
    }
    
    /**
     * Provider-specific configuration validation
     */
    fun validateProviderConfig(config: AiServiceConfig): PluginValidationResult
    
    /**
     * Get the default configuration for this AI service
     */
    fun getDefaultConfig(): AiServiceConfig?
    
    /**
     * Get supported models for this AI service
     */
    fun getSupportedModels(): List<String>
}

/**
 * Base implementation for AI service plugins
 */
abstract class BaseAiServicePlugin : AiServicePlugin {
    private var isInitialized = false
    private lateinit var pluginContext: PluginContext
    
    override fun initialize(context: PluginContext) {
        if (isInitialized) {
            throw IllegalStateException("Plugin ${metadata.id} is already initialized")
        }
        
        this.pluginContext = context
        
        // Perform plugin-specific initialization
        onInitialize(context)
        
        isInitialized = true
        context.logger.info("AI service plugin ${metadata.name} initialized for provider ${supportedProvider}")
    }
    
    override fun shutdown() {
        if (!isInitialized) {
            return
        }
        
        // Perform plugin-specific cleanup
        onShutdown()
        
        isInitialized = false
        pluginContext.logger.info("AI service plugin ${metadata.name} shut down")
    }
    
    /**
     * Get the plugin context (only available after initialization)
     */
    protected fun getContext(): PluginContext {
        if (!isInitialized) {
            throw IllegalStateException("Plugin ${metadata.id} is not initialized")
        }
        return pluginContext
    }
    
    /**
     * Check if the plugin is initialized
     */
    protected fun isInitialized(): Boolean = isInitialized
    
    /**
     * Called when the plugin is being initialized
     * Override this method to perform plugin-specific initialization
     */
    protected open fun onInitialize(context: PluginContext) {
        // Default implementation does nothing
    }
    
    /**
     * Called when the plugin is being shut down
     * Override this method to perform plugin-specific cleanup
     */
    protected open fun onShutdown() {
        // Default implementation does nothing
    }
    
    override fun validateProviderConfig(config: AiServiceConfig): PluginValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // Basic validation
        if (config.baseUrl?.isBlank() != false) {
            errors.add("Base URL cannot be empty")
        }
        
        // Check if API key is required
        if (requiresApiKey() && config.apiKey.isBlank()) {
            errors.add("API key is required for $supportedProvider")
        }
        
        // Validate model
        val supportedModels = getSupportedModels()
        if (supportedModels.isNotEmpty() && config.model !in supportedModels) {
            warnings.add("Model '${config.model}' is not in the list of known supported models: ${supportedModels.joinToString(", ")}")
        }
        
        return PluginValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Whether this AI service requires an API key
     */
    protected open fun requiresApiKey(): Boolean = true
    
    /**
     * Get the default base URL for this AI service
     */
    protected abstract fun getDefaultBaseUrl(): String
    
    /**
     * Get the default model for this AI service
     */
    protected abstract fun getDefaultModel(): String
    
    override fun getDefaultConfig(): AiServiceConfig? {
        return try {
            AiServiceConfig(
                provider = supportedProvider,
                baseUrl = getDefaultBaseUrl(),
                apiKey = "", // Will need to be set by user
                model = getDefaultModel()
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Registry for AI service plugins
 */
object AiServicePluginRegistry {
    private val plugins = mutableMapOf<AiProvider, AiServicePlugin>()
    
    /**
     * Register an AI service plugin
     */
    fun register(plugin: AiServicePlugin) {
        plugins[plugin.supportedProvider] = plugin
    }
    
    /**
     * Unregister an AI service plugin
     */
    fun unregister(provider: AiProvider) {
        plugins.remove(provider)
    }
    
    /**
     * Get an AI service plugin for a provider
     */
    fun getPlugin(provider: AiProvider): AiServicePlugin? {
        return plugins[provider]
    }
    
    /**
     * Get all registered AI service plugins
     */
    fun getAllPlugins(): Map<AiProvider, AiServicePlugin> {
        return plugins.toMap()
    }
    
    /**
     * Check if a provider is supported by any plugin
     */
    fun isProviderSupported(provider: AiProvider): Boolean {
        return plugins.containsKey(provider)
    }
}
