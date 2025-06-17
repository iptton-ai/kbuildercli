package com.aicodingcli.plugins

import com.aicodingcli.ai.AiServiceFactory
import com.aicodingcli.config.ConfigManager
import com.aicodingcli.history.HistoryManager
import java.util.concurrent.ConcurrentHashMap

/**
 * Default implementation of PluginContext
 */
class DefaultPluginContext(
    override val configManager: ConfigManager,
    override val historyManager: HistoryManager,
    override val aiServiceFactory: AiServiceFactory,
    override val logger: PluginLogger,
    private val pluginMetadata: PluginMetadata
) : PluginContext {
    
    private val registeredCommands = mutableListOf<PluginCommand>()
    private val registeredEventHandlers = mutableListOf<PluginEventHandler>()
    private val sharedData = ConcurrentHashMap<String, Any>()
    
    override fun registerCommand(command: Any) {
        val pluginCommand = command as PluginCommand
        registeredCommands.add(pluginCommand)
        logger.debug("Registered command '${pluginCommand.name}' from plugin ${pluginMetadata.id}")
    }

    override fun registerEventHandler(handler: Any) {
        val eventHandler = handler as PluginEventHandler
        registeredEventHandlers.add(eventHandler)
        logger.debug("Registered event handler for events ${eventHandler.eventTypes} from plugin ${pluginMetadata.id}")
    }
    
    override fun getSharedData(key: String): Any? {
        return sharedData[key]
    }
    
    override fun setSharedData(key: String, value: Any) {
        sharedData[key] = value
        logger.debug("Set shared data '$key' from plugin ${pluginMetadata.id}")
    }
    
    override fun hasPermission(permission: PluginPermission): Boolean {
        return pluginMetadata.permissions.contains(permission)
    }
    
    /**
     * Get all registered commands from this context
     */
    fun getRegisteredCommands(): List<PluginCommand> = registeredCommands.toList()
    
    /**
     * Get all registered event handlers from this context
     */
    fun getRegisteredEventHandlers(): List<PluginEventHandler> = registeredEventHandlers.toList()
    
    /**
     * Clear all registered commands and handlers (used during plugin unload)
     */
    fun clearRegistrations() {
        registeredCommands.clear()
        registeredEventHandlers.clear()
        logger.debug("Cleared all registrations for plugin ${pluginMetadata.id}")
    }
}

/**
 * Default implementation of PluginLogger
 */
class DefaultPluginLogger(
    private val pluginId: String,
    private val enableDebug: Boolean = false
) : PluginLogger {
    
    private fun formatMessage(level: String, message: String): String {
        val timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        )
        return "[$timestamp] [$level] [Plugin:$pluginId] $message"
    }
    
    override fun debug(message: String) {
        if (enableDebug) {
            println(formatMessage("DEBUG", message))
        }
    }
    
    override fun info(message: String) {
        println(formatMessage("INFO", message))
    }
    
    override fun warn(message: String) {
        println(formatMessage("WARN", message))
    }
    
    override fun error(message: String, throwable: Throwable?) {
        val errorMessage = if (throwable != null) {
            "$message: ${throwable.message}"
        } else {
            message
        }
        System.err.println(formatMessage("ERROR", errorMessage))
        
        if (throwable != null && enableDebug) {
            throwable.printStackTrace()
        }
    }
}

/**
 * Plugin context factory
 */
object PluginContextFactory {
    /**
     * Create a plugin context for a specific plugin
     */
    fun createContext(
        pluginMetadata: PluginMetadata,
        configManager: ConfigManager,
        historyManager: HistoryManager,
        aiServiceFactory: AiServiceFactory,
        enableDebugLogging: Boolean = false
    ): DefaultPluginContext {
        val logger = DefaultPluginLogger(pluginMetadata.id, enableDebugLogging)
        
        return DefaultPluginContext(
            configManager = configManager,
            historyManager = historyManager,
            aiServiceFactory = aiServiceFactory,
            logger = logger,
            pluginMetadata = pluginMetadata
        )
    }
}

/**
 * Plugin event dispatcher
 */
class PluginEventDispatcher {
    private val eventHandlers = mutableMapOf<PluginEventType, MutableList<PluginEventHandler>>()
    
    /**
     * Register an event handler
     */
    fun registerHandler(handler: PluginEventHandler) {
        handler.eventTypes.forEach { eventType ->
            eventHandlers.getOrPut(eventType) { mutableListOf() }.add(handler)
        }
    }
    
    /**
     * Unregister an event handler
     */
    fun unregisterHandler(handler: PluginEventHandler) {
        handler.eventTypes.forEach { eventType ->
            eventHandlers[eventType]?.remove(handler)
        }
    }
    
    /**
     * Dispatch an event to all registered handlers
     */
    suspend fun dispatchEvent(event: PluginEvent) {
        val handlers = eventHandlers[event.type] ?: return
        
        handlers.forEach { handler ->
            try {
                handler.handleEvent(event)
            } catch (e: Exception) {
                // Log error but don't stop other handlers
                System.err.println("Error in event handler for ${event.type}: ${e.message}")
            }
        }
    }
    
    /**
     * Get all registered handlers for an event type
     */
    fun getHandlers(eventType: PluginEventType): List<PluginEventHandler> {
        return eventHandlers[eventType]?.toList() ?: emptyList()
    }
    
    /**
     * Clear all event handlers
     */
    fun clearAllHandlers() {
        eventHandlers.clear()
    }
}
