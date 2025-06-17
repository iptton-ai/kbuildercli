package com.aicodingcli.plugins

import com.aicodingcli.ai.AiServiceFactory
import com.aicodingcli.config.ConfigManager
import com.aicodingcli.history.HistoryManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.URLClassLoader
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

/**
 * Plugin manager responsible for loading, managing, and unloading plugins
 */
class PluginManager(
    private val pluginDir: String = System.getProperty("user.home") + "/.aicodingcli/plugins",
    private val configManager: ConfigManager,
    private val historyManager: HistoryManager,
    private val aiServiceFactory: AiServiceFactory,
    private val enableDebugLogging: Boolean = false
) {
    private val loadedPlugins = ConcurrentHashMap<String, LoadedPlugin>()
    private val pluginClassLoaders = ConcurrentHashMap<String, ClassLoader>()
    private val pluginRegistry = PluginRegistry()
    private val eventDispatcher = PluginEventDispatcher()
    private val json = Json { ignoreUnknownKeys = true }
    
    init {
        // Ensure plugin directory exists
        File(pluginDir).mkdirs()
    }
    
    /**
     * Load a plugin from a JAR file
     */
    suspend fun loadPlugin(pluginPath: String): Plugin {
        val pluginFile = File(pluginPath)
        if (!pluginFile.exists()) {
            throw PluginLoadException("Plugin file not found: $pluginPath")
        }
        
        // Validate plugin before loading
        val validationResult = validatePlugin(pluginPath)
        if (!validationResult.isValid) {
            throw PluginLoadException("Plugin validation failed: ${validationResult.errors.joinToString(", ")}")
        }
        
        try {
            // Create class loader for the plugin
            val classLoader = URLClassLoader(arrayOf(pluginFile.toURI().toURL()))
            
            // Read plugin metadata
            val metadata = readPluginMetadata(classLoader)
            
            // Check if plugin is already loaded
            if (loadedPlugins.containsKey(metadata.id)) {
                throw PluginLoadException("Plugin ${metadata.id} is already loaded")
            }
            
            // Validate dependencies
            validateDependencies(metadata.dependencies)
            
            // Load plugin main class
            val pluginClass = classLoader.loadClass(metadata.mainClass)
            val plugin = pluginClass.getDeclaredConstructor().newInstance() as Plugin
            
            // Verify metadata matches
            if (plugin.metadata.id != metadata.id) {
                throw PluginLoadException("Plugin metadata mismatch: expected ${metadata.id}, got ${plugin.metadata.id}")
            }
            
            // Create plugin context
            val context = PluginContextFactory.createContext(
                pluginMetadata = metadata,
                configManager = configManager,
                historyManager = historyManager,
                aiServiceFactory = aiServiceFactory,
                enableDebugLogging = enableDebugLogging
            )
            
            // Initialize plugin
            plugin.initialize(context)
            
            // Store loaded plugin info
            val loadedPlugin = LoadedPlugin(
                plugin = plugin,
                context = context,
                classLoader = classLoader,
                filePath = pluginPath,
                state = PluginState.RUNNING
            )
            
            loadedPlugins[metadata.id] = loadedPlugin
            pluginClassLoaders[metadata.id] = classLoader
            
            // Register plugin with registry
            pluginRegistry.registerPlugin(plugin, context)
            
            // Register event handlers
            context.getRegisteredEventHandlers().forEach { handler ->
                eventDispatcher.registerHandler(handler)
            }
            
            // Dispatch plugin loaded event
            eventDispatcher.dispatchEvent(PluginEvent(
                type = PluginEventType.PLUGIN_LOADED,
                source = metadata.id,
                data = mapOf("plugin" to plugin, "metadata" to metadata)
            ))
            
            return plugin
            
        } catch (e: Exception) {
            throw PluginLoadException("Failed to load plugin from $pluginPath", e)
        }
    }
    
    /**
     * Unload a plugin
     */
    suspend fun unloadPlugin(pluginId: String): Boolean {
        val loadedPlugin = loadedPlugins[pluginId] ?: return false
        
        try {
            // Dispatch plugin unloading event
            eventDispatcher.dispatchEvent(PluginEvent(
                type = PluginEventType.PLUGIN_UNLOADED,
                source = pluginId,
                data = mapOf("plugin" to loadedPlugin.plugin)
            ))
            
            // Unregister event handlers
            loadedPlugin.context.getRegisteredEventHandlers().forEach { handler ->
                eventDispatcher.unregisterHandler(handler)
            }
            
            // Unregister from registry
            pluginRegistry.unregisterPlugin(pluginId)
            
            // Shutdown plugin
            loadedPlugin.plugin.shutdown()
            
            // Clear context registrations
            loadedPlugin.context.clearRegistrations()
            
            // Remove from loaded plugins
            loadedPlugins.remove(pluginId)
            pluginClassLoaders.remove(pluginId)
            
            return true
            
        } catch (e: Exception) {
            throw PluginExecutionException("Failed to unload plugin $pluginId", e)
        }
    }
    
    /**
     * Reload a plugin
     */
    suspend fun reloadPlugin(pluginId: String): Plugin {
        val loadedPlugin = loadedPlugins[pluginId]
            ?: throw PluginLoadException("Plugin $pluginId is not loaded")
        
        val pluginPath = loadedPlugin.filePath
        
        // Unload current plugin
        unloadPlugin(pluginId)
        
        // Load plugin again
        return loadPlugin(pluginPath)
    }
    
    /**
     * Get all loaded plugins
     */
    fun getLoadedPlugins(): List<Plugin> {
        return loadedPlugins.values.map { it.plugin }
    }
    
    /**
     * Get a specific plugin by ID
     */
    fun getPlugin(pluginId: String): Plugin? {
        return loadedPlugins[pluginId]?.plugin
    }
    
    /**
     * Get plugin state
     */
    fun getPluginState(pluginId: String): PluginState? {
        return loadedPlugins[pluginId]?.state
    }
    
    /**
     * Get the plugin registry
     */
    fun getRegistry(): PluginRegistry = pluginRegistry
    
    /**
     * Get the event dispatcher
     */
    fun getEventDispatcher(): PluginEventDispatcher = eventDispatcher
    
    /**
     * Install a plugin from a file or URL
     */
    suspend fun installPlugin(pluginSource: String): Boolean {
        // TODO: Implement plugin installation from URL or file
        // For now, just copy to plugin directory if it's a local file
        val sourceFile = File(pluginSource)
        if (!sourceFile.exists()) {
            throw PluginLoadException("Plugin source not found: $pluginSource")
        }
        
        val metadata = readPluginMetadata(URLClassLoader(arrayOf(sourceFile.toURI().toURL())))
        val targetFile = File(pluginDir, "${metadata.id}-${metadata.version}.jar")
        
        sourceFile.copyTo(targetFile, overwrite = true)
        
        // Load the installed plugin
        loadPlugin(targetFile.absolutePath)
        
        return true
    }
    
    /**
     * Uninstall a plugin
     */
    suspend fun uninstallPlugin(pluginId: String): Boolean {
        val loadedPlugin = loadedPlugins[pluginId]
        
        // Unload if currently loaded
        if (loadedPlugin != null) {
            unloadPlugin(pluginId)
        }
        
        // Remove plugin file
        val pluginFiles = File(pluginDir).listFiles { file ->
            file.name.startsWith("$pluginId-") && file.name.endsWith(".jar")
        }
        
        return pluginFiles?.any { it.delete() } ?: false
    }
    
    /**
     * Update a plugin
     */
    suspend fun updatePlugin(pluginId: String): Boolean {
        // TODO: Implement plugin update mechanism
        // This would typically involve checking for updates from a repository
        throw NotImplementedError("Plugin update functionality not yet implemented")
    }
    
    /**
     * Validate a plugin file
     */
    fun validatePlugin(pluginPath: String): PluginValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            val pluginFile = File(pluginPath)
            if (!pluginFile.exists()) {
                errors.add("Plugin file does not exist: $pluginPath")
                return PluginValidationResult(false, errors, warnings)
            }
            
            if (!pluginFile.name.endsWith(".jar")) {
                errors.add("Plugin file must be a JAR file")
                return PluginValidationResult(false, errors, warnings)
            }
            
            // Validate JAR structure
            val classLoader = URLClassLoader(arrayOf(pluginFile.toURI().toURL()))
            
            // Check for plugin.json
            val metadataStream = classLoader.getResourceAsStream("plugin.json")
            if (metadataStream == null) {
                errors.add("Plugin metadata file (plugin.json) not found")
                return PluginValidationResult(false, errors, warnings)
            }
            
            // Validate metadata
            val jsonText = metadataStream.bufferedReader().use { it.readText() }
            val jsonElement = Json.parseToJsonElement(jsonText).jsonObject
            val metadata = PluginMetadata(
                id = jsonElement["id"]?.jsonPrimitive?.content ?: throw PluginLoadException("Missing plugin id"),
                name = jsonElement["name"]?.jsonPrimitive?.content ?: throw PluginLoadException("Missing plugin name"),
                version = jsonElement["version"]?.jsonPrimitive?.content ?: throw PluginLoadException("Missing plugin version"),
                description = jsonElement["description"]?.jsonPrimitive?.content ?: "",
                author = jsonElement["author"]?.jsonPrimitive?.content ?: "",
                mainClass = jsonElement["mainClass"]?.jsonPrimitive?.content ?: throw PluginLoadException("Missing main class")
            )
            
            // Check main class exists
            try {
                val mainClass = classLoader.loadClass(metadata.mainClass)
                if (!Plugin::class.java.isAssignableFrom(mainClass)) {
                    errors.add("Main class ${metadata.mainClass} does not implement Plugin interface")
                }
            } catch (e: ClassNotFoundException) {
                errors.add("Main class ${metadata.mainClass} not found in plugin")
            }
            
            // Validate dependencies
            metadata.dependencies.forEach { dependency ->
                if (!isPluginAvailable(dependency.id) && !dependency.optional) {
                    warnings.add("Required dependency ${dependency.id} is not available")
                }
            }
            
        } catch (e: Exception) {
            errors.add("Plugin validation failed: ${e.message}")
        }
        
        return PluginValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    private fun readPluginMetadata(classLoader: ClassLoader): PluginMetadata {
        val metadataStream = classLoader.getResourceAsStream("plugin.json")
            ?: throw PluginLoadException("Plugin metadata file (plugin.json) not found")

        val jsonText = metadataStream.bufferedReader().use { it.readText() }
        val jsonElement = Json.parseToJsonElement(jsonText).jsonObject

        return PluginMetadata(
            id = jsonElement["id"]?.jsonPrimitive?.content ?: throw PluginLoadException("Missing plugin id"),
            name = jsonElement["name"]?.jsonPrimitive?.content ?: throw PluginLoadException("Missing plugin name"),
            version = jsonElement["version"]?.jsonPrimitive?.content ?: throw PluginLoadException("Missing plugin version"),
            description = jsonElement["description"]?.jsonPrimitive?.content ?: "",
            author = jsonElement["author"]?.jsonPrimitive?.content ?: "",
            mainClass = jsonElement["mainClass"]?.jsonPrimitive?.content ?: throw PluginLoadException("Missing main class"),
            dependencies = emptyList(), // TODO: Parse dependencies
            permissions = emptyList(), // TODO: Parse permissions
            minCliVersion = jsonElement["minCliVersion"]?.jsonPrimitive?.content,
            website = jsonElement["website"]?.jsonPrimitive?.content
        )
    }
    
    private fun validateDependencies(dependencies: List<PluginDependency>) {
        dependencies.forEach { dependency ->
            if (!dependency.optional && !isPluginAvailable(dependency.id)) {
                throw PluginLoadException("Required dependency ${dependency.id} is not available")
            }
        }
    }
    
    private fun isPluginAvailable(pluginId: String): Boolean {
        return loadedPlugins.containsKey(pluginId)
    }
}

/**
 * Information about a loaded plugin
 */
private data class LoadedPlugin(
    val plugin: Plugin,
    val context: DefaultPluginContext,
    val classLoader: ClassLoader,
    val filePath: String,
    val state: PluginState
)
