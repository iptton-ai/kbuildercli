package com.aicodingcli.conversation.tools

import com.aicodingcli.conversation.*

/**
 * Interface for handling specific tool execution
 */
interface ToolHandler {
    /**
     * The name of the tool this handler supports
     */
    val toolName: String
    
    /**
     * Execute the tool with given parameters
     */
    suspend fun execute(parameters: Map<String, String>, workingDirectory: String): ToolResult
    
    /**
     * Validate tool parameters
     */
    fun validateParameters(parameters: Map<String, String>): ValidationResult
    
    /**
     * Get tool metadata
     */
    fun getMetadata(): ToolMetadata
}

/**
 * Abstract base class for tool handlers
 */
abstract class BaseToolHandler(override val toolName: String) : ToolHandler {
    
    override suspend fun execute(parameters: Map<String, String>, workingDirectory: String): ToolResult {
        return try {
            val validation = validateParameters(parameters)
            if (!validation.isValid) {
                return ToolResult.failure(
                    error = "Parameter validation failed: ${validation.errors.joinToString(", ")}"
                )
            }
            
            executeInternal(parameters, workingDirectory)
        } catch (e: Exception) {
            ToolResult.failure("Tool execution failed: ${e.message}")
        }
    }
    
    /**
     * Internal execution method to be implemented by subclasses
     */
    protected abstract suspend fun executeInternal(
        parameters: Map<String, String>, 
        workingDirectory: String
    ): ToolResult
    
    /**
     * Helper method to check required parameters
     */
    protected fun checkRequiredParameters(
        parameters: Map<String, String>, 
        required: List<String>
    ): List<String> {
        val errors = mutableListOf<String>()
        required.forEach { param ->
            if (!parameters.containsKey(param) || parameters[param].isNullOrBlank()) {
                errors.add("Missing required parameter: $param")
            }
        }
        return errors
    }
}

/**
 * Registry for tool handlers
 */
class ToolHandlerRegistry {
    private val handlers = mutableMapOf<String, ToolHandler>()
    
    /**
     * Register a tool handler
     */
    fun register(handler: ToolHandler) {
        handlers[handler.toolName] = handler
    }
    
    /**
     * Get handler for a tool
     */
    fun getHandler(toolName: String): ToolHandler? {
        return handlers[toolName]
    }
    
    /**
     * Get all registered handlers
     */
    fun getAllHandlers(): List<ToolHandler> {
        return handlers.values.toList()
    }
    
    /**
     * Get metadata for all tools
     */
    fun getAllToolMetadata(): List<ToolMetadata> {
        return handlers.values.map { it.getMetadata() }
    }
}

/**
 * Factory for creating default tool handlers
 */
object DefaultToolHandlerFactory {
    
    fun createDefaultHandlers(): List<ToolHandler> {
        return listOf(
            // File operations
            SaveFileHandler(),
            ViewHandler(),
            StrReplaceEditorHandler(),
            RemoveFilesHandler(),

            // Analysis tools
            CodebaseRetrievalHandler(),
            DiagnosticsHandler(),

            // Task management
            TaskManagementHandler(),
            UpdateTasksHandler(),

            // Process management
            LaunchProcessHandler(),
            ReadTerminalHandler(),

            // Network tools
            WebSearchHandler(),
            WebFetchHandler(),
            OpenBrowserHandler(),

            // Git operations
            GitHubApiHandler(),
            GitOperationsHandler()
        )
    }
    
    fun createDefaultRegistry(): ToolHandlerRegistry {
        val registry = ToolHandlerRegistry()
        createDefaultHandlers().forEach { registry.register(it) }
        return registry
    }
}
