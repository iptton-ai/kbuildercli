package com.aicodingcli.plugins

import com.aicodingcli.ai.AiServiceFactory
import com.aicodingcli.config.ConfigManager
import com.aicodingcli.history.HistoryManager


/**
 * Base interface for all plugins
 */
interface Plugin {
    /**
     * Plugin metadata containing information about the plugin
     */
    val metadata: PluginMetadata
    
    /**
     * Initialize the plugin with the given context
     */
    fun initialize(context: PluginContext)
    
    /**
     * Shutdown the plugin and clean up resources
     */
    fun shutdown()
}

/**
 * Plugin metadata containing information about the plugin
 */
data class PluginMetadata(
    /**
     * Unique identifier for the plugin
     */
    val id: String,
    
    /**
     * Human-readable name of the plugin
     */
    val name: String,
    
    /**
     * Version of the plugin (semantic versioning)
     */
    val version: String,
    
    /**
     * Description of what the plugin does
     */
    val description: String,
    
    /**
     * Author or organization that created the plugin
     */
    val author: String,
    
    /**
     * Main class name for the plugin
     */
    val mainClass: String,
    
    /**
     * List of dependencies required by this plugin
     */
    val dependencies: List<PluginDependency> = emptyList(),
    
    /**
     * List of permissions required by this plugin
     */
    val permissions: List<PluginPermission> = emptyList(),
    
    /**
     * Minimum version of the CLI tool required
     */
    val minCliVersion: String? = null,
    
    /**
     * Website or repository URL for the plugin
     */
    val website: String? = null
)

/**
 * Plugin dependency specification
 */
data class PluginDependency(
    /**
     * ID of the required plugin
     */
    val id: String,
    
    /**
     * Version requirement (e.g., ">=1.0.0", "~1.2.0")
     */
    val version: String,
    
    /**
     * Whether this dependency is optional
     */
    val optional: Boolean = false
)

/**
 * Base class for plugin permissions
 */
sealed class PluginPermission {
    /**
     * Permission to access file system
     */
    data class FileSystemPermission(
        val allowedPaths: List<String>,
        val readOnly: Boolean = false
    ) : PluginPermission()
    
    /**
     * Permission to make network requests
     */
    data class NetworkPermission(
        val allowedHosts: List<String>
    ) : PluginPermission()
    
    /**
     * Permission to execute system commands
     */
    data class SystemPermission(
        val allowedCommands: List<String>
    ) : PluginPermission()
    
    /**
     * Permission to access configuration
     */
    object ConfigPermission : PluginPermission()
    
    /**
     * Permission to access conversation history
     */
    object HistoryPermission : PluginPermission()
}

/**
 * Plugin context providing access to CLI services
 */
interface PluginContext {
    /**
     * Configuration manager for accessing and modifying settings
     */
    val configManager: ConfigManager
    
    /**
     * History manager for accessing conversation history
     */
    val historyManager: HistoryManager
    
    /**
     * AI service factory for creating AI services
     */
    val aiServiceFactory: AiServiceFactory
    
    /**
     * Logger for plugin-specific logging
     */
    val logger: PluginLogger
    
    /**
     * Register a command that this plugin provides
     */
    fun registerCommand(command: Any) // Will be PluginCommand

    /**
     * Register an event handler for system events
     */
    fun registerEventHandler(handler: Any) // Will be PluginEventHandler
    
    /**
     * Get shared data between plugins
     */
    fun getSharedData(key: String): Any?
    
    /**
     * Set shared data for other plugins to access
     */
    fun setSharedData(key: String, value: Any)
    
    /**
     * Check if the plugin has a specific permission
     */
    fun hasPermission(permission: PluginPermission): Boolean
}

/**
 * Plugin-specific logger interface
 */
interface PluginLogger {
    fun debug(message: String)
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String, throwable: Throwable? = null)
}

/**
 * Plugin lifecycle states
 */
enum class PluginState {
    UNLOADED,
    LOADED,
    INITIALIZED,
    RUNNING,
    STOPPED,
    ERROR
}

/**
 * Plugin validation result
 */
data class PluginValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

/**
 * Plugin load exception
 */
class PluginLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Plugin execution exception
 */
class PluginExecutionException(message: String, cause: Throwable? = null) : Exception(message, cause)
