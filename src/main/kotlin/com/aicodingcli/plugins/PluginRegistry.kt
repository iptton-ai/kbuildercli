package com.aicodingcli.plugins

import com.aicodingcli.ai.AiProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.ConcurrentHashMap

/**
 * Registry for managing loaded plugins and their capabilities
 */
class PluginRegistry {
    private val plugins = ConcurrentHashMap<String, Plugin>()
    private val pluginContexts = ConcurrentHashMap<String, DefaultPluginContext>()
    private val commandPlugins = ConcurrentHashMap<String, CommandPlugin>()
    private val aiServicePlugins = ConcurrentHashMap<AiProvider, AiServicePlugin>()
    private val commandMappings = ConcurrentHashMap<String, String>() // command name -> plugin id
    
    /**
     * Register a plugin with the registry
     */
    fun registerPlugin(plugin: Plugin, context: DefaultPluginContext) {
        val pluginId = plugin.metadata.id
        
        // Store plugin and context
        plugins[pluginId] = plugin
        pluginContexts[pluginId] = context
        
        // Register specific plugin types
        when (plugin) {
            is CommandPlugin -> registerCommandPlugin(plugin)
            is AiServicePlugin -> registerAiServicePlugin(plugin)
        }
    }
    
    /**
     * Unregister a plugin from the registry
     */
    fun unregisterPlugin(pluginId: String) {
        val plugin = plugins[pluginId] ?: return
        
        // Unregister specific plugin types
        when (plugin) {
            is CommandPlugin -> unregisterCommandPlugin(plugin)
            is AiServicePlugin -> unregisterAiServicePlugin(plugin)
        }
        
        // Remove from main registry
        plugins.remove(pluginId)
        pluginContexts.remove(pluginId)
    }
    
    /**
     * Register a command plugin
     */
    private fun registerCommandPlugin(plugin: CommandPlugin) {
        val pluginId = plugin.metadata.id
        commandPlugins[pluginId] = plugin
        
        // Register command mappings
        plugin.commands.forEach { command ->
            if (commandMappings.containsKey(command.name)) {
                val existingPluginId = commandMappings[command.name]
                throw IllegalStateException("Command '${command.name}' is already registered by plugin $existingPluginId")
            }
            commandMappings[command.name] = pluginId
        }
    }
    
    /**
     * Unregister a command plugin
     */
    private fun unregisterCommandPlugin(plugin: CommandPlugin) {
        val pluginId = plugin.metadata.id
        commandPlugins.remove(pluginId)
        
        // Remove command mappings
        plugin.commands.forEach { command ->
            commandMappings.remove(command.name)
        }
    }
    
    /**
     * Register an AI service plugin
     */
    private fun registerAiServicePlugin(plugin: AiServicePlugin) {
        val provider = plugin.supportedProvider
        
        if (aiServicePlugins.containsKey(provider)) {
            val existingPlugin = aiServicePlugins[provider]
            throw IllegalStateException("AI provider $provider is already registered by plugin ${existingPlugin?.metadata?.id}")
        }
        
        aiServicePlugins[provider] = plugin
        AiServicePluginRegistry.register(plugin)
    }
    
    /**
     * Unregister an AI service plugin
     */
    private fun unregisterAiServicePlugin(plugin: AiServicePlugin) {
        aiServicePlugins.remove(plugin.supportedProvider)
        AiServicePluginRegistry.unregister(plugin.supportedProvider)
    }
    
    /**
     * Get a plugin by ID
     */
    fun getPlugin(pluginId: String): Plugin? {
        return plugins[pluginId]
    }
    
    /**
     * Get all registered plugins
     */
    fun getAllPlugins(): Map<String, Plugin> {
        return plugins.toMap()
    }
    
    /**
     * Get a command plugin that provides a specific command
     */
    fun getCommandPlugin(commandName: String): CommandPlugin? {
        val pluginId = commandMappings[commandName] ?: return null
        return commandPlugins[pluginId]
    }
    
    /**
     * Get a specific command by name
     */
    fun getCommand(commandName: String): PluginCommand? {
        val plugin = getCommandPlugin(commandName) ?: return null
        return plugin.getCommand(commandName)
    }
    
    /**
     * Get all available commands from all plugins
     */
    fun getAllCommands(): Map<String, PluginCommand> {
        val commands = mutableMapOf<String, PluginCommand>()
        
        commandPlugins.values.forEach { plugin ->
            plugin.commands.forEach { command ->
                commands[command.name] = command
            }
        }
        
        return commands
    }
    
    /**
     * Get all command plugins
     */
    fun getCommandPlugins(): Map<String, CommandPlugin> {
        return commandPlugins.toMap()
    }
    
    /**
     * Get an AI service plugin for a specific provider
     */
    fun getAiServicePlugin(provider: AiProvider): AiServicePlugin? {
        return aiServicePlugins[provider]
    }
    
    /**
     * Get all AI service plugins
     */
    fun getAiServicePlugins(): Map<AiProvider, AiServicePlugin> {
        return aiServicePlugins.toMap()
    }
    
    /**
     * Check if a command is available
     */
    fun hasCommand(commandName: String): Boolean {
        return commandMappings.containsKey(commandName)
    }
    
    /**
     * Check if an AI provider is supported
     */
    fun hasAiProvider(provider: AiProvider): Boolean {
        return aiServicePlugins.containsKey(provider)
    }
    
    /**
     * Get plugin context for a specific plugin
     */
    fun getPluginContext(pluginId: String): DefaultPluginContext? {
        return pluginContexts[pluginId]
    }
    
    /**
     * Get plugins by type
     */
    fun <T : Plugin> getPluginsByType(clazz: Class<T>): List<T> {
        return plugins.values.filterIsInstance(clazz)
    }
    
    /**
     * Get plugin statistics
     */
    fun getStatistics(): PluginRegistryStatistics {
        return PluginRegistryStatistics(
            totalPlugins = plugins.size,
            commandPlugins = commandPlugins.size,
            aiServicePlugins = aiServicePlugins.size,
            totalCommands = commandMappings.size,
            supportedAiProviders = aiServicePlugins.keys.toList()
        )
    }
    
    /**
     * Clear all registrations (used for testing or shutdown)
     */
    fun clear() {
        plugins.clear()
        pluginContexts.clear()
        commandPlugins.clear()
        aiServicePlugins.clear()
        commandMappings.clear()
    }
}

/**
 * Statistics about the plugin registry
 */
data class PluginRegistryStatistics(
    val totalPlugins: Int,
    val commandPlugins: Int,
    val aiServicePlugins: Int,
    val totalCommands: Int,
    val supportedAiProviders: List<AiProvider>
)

/**
 * Plugin discovery service for finding available plugins
 */
class PluginDiscoveryService(private val pluginDir: String) {
    
    /**
     * Discover all plugin files in the plugin directory
     */
    fun discoverPlugins(): List<PluginInfo> {
        val pluginDirectory = java.io.File(pluginDir)
        if (!pluginDirectory.exists() || !pluginDirectory.isDirectory) {
            return emptyList()
        }
        
        return pluginDirectory.listFiles { file ->
            file.isFile && file.name.endsWith(".jar")
        }?.mapNotNull { file ->
            try {
                val classLoader = java.net.URLClassLoader(arrayOf(file.toURI().toURL()))
                val metadataStream = classLoader.getResourceAsStream("plugin.json")
                
                if (metadataStream != null) {
                    val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                    val jsonText = metadataStream.bufferedReader().use { it.readText() }
                    val jsonElement = Json.parseToJsonElement(jsonText).jsonObject
                    val metadata = PluginMetadata(
                        id = jsonElement["id"]?.jsonPrimitive?.content ?: "",
                        name = jsonElement["name"]?.jsonPrimitive?.content ?: "",
                        version = jsonElement["version"]?.jsonPrimitive?.content ?: "",
                        description = jsonElement["description"]?.jsonPrimitive?.content ?: "",
                        author = jsonElement["author"]?.jsonPrimitive?.content ?: "",
                        mainClass = jsonElement["mainClass"]?.jsonPrimitive?.content ?: ""
                    )
                    
                    PluginInfo(
                        metadata = metadata,
                        filePath = file.absolutePath,
                        fileSize = file.length(),
                        lastModified = file.lastModified()
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null // Skip invalid plugins
            }
        } ?: emptyList()
    }
    
    /**
     * Get plugin info for a specific file
     */
    fun getPluginInfo(pluginPath: String): PluginInfo? {
        return try {
            val file = java.io.File(pluginPath)
            val classLoader = java.net.URLClassLoader(arrayOf(file.toURI().toURL()))
            val metadataStream = classLoader.getResourceAsStream("plugin.json")
            
            if (metadataStream != null) {
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val jsonText = metadataStream.bufferedReader().use { it.readText() }
                val jsonElement = Json.parseToJsonElement(jsonText).jsonObject
                val metadata = PluginMetadata(
                    id = jsonElement["id"]?.jsonPrimitive?.content ?: "",
                    name = jsonElement["name"]?.jsonPrimitive?.content ?: "",
                    version = jsonElement["version"]?.jsonPrimitive?.content ?: "",
                    description = jsonElement["description"]?.jsonPrimitive?.content ?: "",
                    author = jsonElement["author"]?.jsonPrimitive?.content ?: "",
                    mainClass = jsonElement["mainClass"]?.jsonPrimitive?.content ?: ""
                )
                
                PluginInfo(
                    metadata = metadata,
                    filePath = file.absolutePath,
                    fileSize = file.length(),
                    lastModified = file.lastModified()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Information about a discovered plugin
 */
data class PluginInfo(
    val metadata: PluginMetadata,
    val filePath: String,
    val fileSize: Long,
    val lastModified: Long
)
