package com.aicodingcli.plugins

/**
 * Plugin that provides CLI commands
 */
interface CommandPlugin : Plugin {
    /**
     * List of commands provided by this plugin
     */
    val commands: List<PluginCommand>
    
    /**
     * Get a specific command by name
     */
    fun getCommand(name: String): PluginCommand? {
        return commands.find { it.name == name }
    }
    
    /**
     * Check if this plugin provides a specific command
     */
    fun hasCommand(name: String): Boolean {
        return commands.any { it.name == name }
    }
}

/**
 * Base implementation for command plugins
 */
abstract class BaseCommandPlugin : CommandPlugin {
    private var isInitialized = false
    private lateinit var pluginContext: PluginContext
    
    override fun initialize(context: PluginContext) {
        if (isInitialized) {
            throw IllegalStateException("Plugin ${metadata.id} is already initialized")
        }
        
        this.pluginContext = context
        
        // Register all commands
        commands.forEach { command ->
            context.registerCommand(command)
        }
        
        // Perform plugin-specific initialization
        onInitialize(context)
        
        isInitialized = true
        context.logger.info("Command plugin ${metadata.name} initialized with ${commands.size} commands")
    }
    
    override fun shutdown() {
        if (!isInitialized) {
            return
        }
        
        // Perform plugin-specific cleanup
        onShutdown()
        
        isInitialized = false
        pluginContext.logger.info("Command plugin ${metadata.name} shut down")
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
    
    /**
     * Helper method to create a command with common error handling
     */
    protected fun createCommand(
        name: String,
        description: String,
        usage: String,
        options: List<CommandOption> = emptyList(),
        handler: suspend (args: CommandArgs, context: PluginContext) -> CommandResult
    ): PluginCommand {
        return PluginCommand(
            name = name,
            description = description,
            usage = usage,
            options = options,
            handler = { args, context ->
                try {
                    handler(args, context)
                } catch (e: Exception) {
                    context.logger.error("Error executing command '$name'", e)
                    CommandResult.error("Command execution failed: ${e.message}", e)
                }
            }
        )
    }
    
    /**
     * Helper method to validate required arguments
     */
    protected fun validateArgs(args: CommandArgs, minArgs: Int): CommandResult? {
        if (args.args.size < minArgs) {
            return CommandResult.failure("Insufficient arguments. Expected at least $minArgs, got ${args.args.size}")
        }
        return null
    }
    
    /**
     * Helper method to validate required options
     */
    protected fun validateOptions(args: CommandArgs, requiredOptions: List<String>): CommandResult? {
        val missingOptions = requiredOptions.filter { !args.hasOption(it) }
        if (missingOptions.isNotEmpty()) {
            return CommandResult.failure("Missing required options: ${missingOptions.joinToString(", ")}")
        }
        return null
    }
}

/**
 * Simple command plugin implementation for single commands
 */
abstract class SingleCommandPlugin : BaseCommandPlugin() {
    /**
     * The single command provided by this plugin
     */
    abstract val command: PluginCommand
    
    override val commands: List<PluginCommand>
        get() = listOf(command)
}
