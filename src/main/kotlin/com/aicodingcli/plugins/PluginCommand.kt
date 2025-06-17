package com.aicodingcli.plugins

/**
 * Command provided by a plugin
 */
data class PluginCommand(
    /**
     * Name of the command
     */
    val name: String,
    
    /**
     * Description of what the command does
     */
    val description: String,
    
    /**
     * Usage string showing how to use the command
     */
    val usage: String,
    
    /**
     * List of command options/flags
     */
    val options: List<CommandOption> = emptyList(),
    
    /**
     * Handler function that executes the command
     */
    val handler: suspend (args: CommandArgs, context: PluginContext) -> CommandResult
)

/**
 * Command option/flag definition
 */
data class CommandOption(
    /**
     * Long name of the option (e.g., "output")
     */
    val name: String,
    
    /**
     * Short name of the option (e.g., "o")
     */
    val shortName: String?,
    
    /**
     * Description of what the option does
     */
    val description: String,
    
    /**
     * Whether this option is required
     */
    val required: Boolean = false,
    
    /**
     * Whether this option takes a value
     */
    val hasValue: Boolean = true,
    
    /**
     * Default value for the option
     */
    val defaultValue: String? = null
)

/**
 * Command arguments passed to plugin command handlers
 */
data class CommandArgs(
    /**
     * Positional arguments
     */
    val args: List<String>,
    
    /**
     * Named options/flags
     */
    val options: Map<String, String?>,
    
    /**
     * Raw command line arguments
     */
    val rawArgs: Array<String>
) {
    /**
     * Get a positional argument by index
     */
    fun getArg(index: Int): String? = args.getOrNull(index)
    
    /**
     * Get an option value by name
     */
    fun getOption(name: String): String? = options[name]
    
    /**
     * Check if an option is present (for flags without values)
     */
    fun hasOption(name: String): Boolean = options.containsKey(name)
    
    /**
     * Get an option value with a default
     */
    fun getOptionOrDefault(name: String, default: String): String = options[name] ?: default
}

/**
 * Result of executing a plugin command
 */
data class CommandResult(
    /**
     * Whether the command executed successfully
     */
    val success: Boolean,
    
    /**
     * Message to display to the user
     */
    val message: String?,
    
    /**
     * Additional data returned by the command
     */
    val data: Any? = null,
    
    /**
     * Exit code for the command
     */
    val exitCode: Int = if (success) 0 else 1
) {
    companion object {
        /**
         * Create a successful result
         */
        fun success(message: String? = null, data: Any? = null): CommandResult {
            return CommandResult(true, message, data, 0)
        }
        
        /**
         * Create a failure result
         */
        fun failure(message: String, exitCode: Int = 1): CommandResult {
            return CommandResult(false, message, null, exitCode)
        }
        
        /**
         * Create an error result
         */
        fun error(message: String, throwable: Throwable? = null): CommandResult {
            val errorMessage = if (throwable != null) {
                "$message: ${throwable.message}"
            } else {
                message
            }
            return CommandResult(false, errorMessage, throwable, 1)
        }
    }
}

/**
 * Plugin event handler interface
 */
interface PluginEventHandler {
    /**
     * Handle a plugin event
     */
    suspend fun handleEvent(event: PluginEvent)
    
    /**
     * Get the types of events this handler is interested in
     */
    val eventTypes: Set<PluginEventType>
}

/**
 * Plugin event types
 */
enum class PluginEventType {
    PLUGIN_LOADED,
    PLUGIN_UNLOADED,
    COMMAND_EXECUTED,
    CONFIG_CHANGED,
    CONVERSATION_STARTED,
    CONVERSATION_ENDED,
    AI_REQUEST_SENT,
    AI_RESPONSE_RECEIVED
}

/**
 * Plugin event data
 */
data class PluginEvent(
    val type: PluginEventType,
    val source: String,
    val data: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)
