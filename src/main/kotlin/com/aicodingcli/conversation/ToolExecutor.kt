package com.aicodingcli.conversation

import java.io.File
import java.time.Instant
import kotlin.time.measureTime

/**
 * Interface for executing tools
 */
interface ToolExecutor {
    /**
     * Execute a tool call
     */
    suspend fun execute(tool: ToolCall): ToolResult
    
    /**
     * Get supported tools metadata
     */
    fun getSupportedTools(): List<ToolMetadata>
    
    /**
     * Validate tool parameters
     */
    suspend fun validateParameters(tool: ToolCall): ValidationResult
}

/**
 * Default implementation of ToolExecutor
 */
class DefaultToolExecutor(
    private val workingDirectory: String = System.getProperty("user.dir")
) : ToolExecutor {
    
    override suspend fun execute(tool: ToolCall): ToolResult {
        val startTime = Instant.now()
        
        try {
            // Validate parameters first
            val validation = validateParameters(tool)
            if (!validation.isValid) {
                return ToolResult.failure(
                    error = "Parameter validation failed: ${validation.errors.joinToString(", ")}"
                )
            }
            
            val result = when (tool.toolName) {
                "save-file" -> executeSaveFile(tool)
                "view" -> executeView(tool)
                "str-replace-editor" -> executeStrReplaceEditor(tool)
                "codebase-retrieval" -> executeCodebaseRetrieval(tool)
                "add_tasks" -> executeAddTasks(tool)
                "update_tasks" -> executeUpdateTasks(tool)
                "remove-files" -> executeRemoveFiles(tool)
                else -> ToolResult.failure("Unsupported tool: ${tool.toolName}")
            }
            
            // Add execution metadata
            val executionTime = measureTime { /* Already executed above */ }
            val metadata = result.metadata.toMutableMap()
            metadata["tool_name"] = tool.toolName
            metadata["execution_time"] = executionTime.toString()
            metadata["executed_at"] = startTime.toString()
            
            return result.copy(metadata = metadata)
            
        } catch (e: Exception) {
            return ToolResult.failure(
                error = "Tool execution failed: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    override fun getSupportedTools(): List<ToolMetadata> {
        return listOf(
            ToolMetadata(
                name = "save-file",
                description = "Create a new file with content",
                parameters = listOf(
                    ToolParameter("path", "string", "File path relative to working directory", required = true),
                    ToolParameter("file_content", "string", "Content to write to the file", required = true)
                ),
                category = "file"
            ),
            ToolMetadata(
                name = "view",
                description = "View file or directory contents",
                parameters = listOf(
                    ToolParameter("path", "string", "Path to view", required = true),
                    ToolParameter("type", "string", "Type: 'file' or 'directory'", required = true)
                ),
                category = "file"
            ),
            ToolMetadata(
                name = "str-replace-editor",
                description = "Replace text in a file",
                parameters = listOf(
                    ToolParameter("path", "string", "File path", required = true),
                    ToolParameter("old_str", "string", "Text to replace", required = true),
                    ToolParameter("new_str", "string", "Replacement text", required = true)
                ),
                category = "file"
            ),
            ToolMetadata(
                name = "codebase-retrieval",
                description = "Retrieve information from codebase",
                parameters = listOf(
                    ToolParameter("information_request", "string", "What information to retrieve", required = true)
                ),
                category = "analysis"
            ),
            ToolMetadata(
                name = "add_tasks",
                description = "Add new tasks to task list",
                parameters = listOf(
                    ToolParameter("name", "string", "Task name", required = true),
                    ToolParameter("description", "string", "Task description", required = true)
                ),
                category = "task"
            ),
            ToolMetadata(
                name = "update_tasks",
                description = "Update existing tasks",
                parameters = listOf(
                    ToolParameter("task_id", "string", "Task ID to update", required = false),
                    ToolParameter("state", "string", "New task state", required = false)
                ),
                category = "task"
            ),
            ToolMetadata(
                name = "remove-files",
                description = "Remove files from filesystem",
                parameters = listOf(
                    ToolParameter("file_paths", "array", "Array of file paths to remove", required = true)
                ),
                category = "file"
            )
        )
    }
    
    override suspend fun validateParameters(tool: ToolCall): ValidationResult {
        val supportedTool = getSupportedTools().find { it.name == tool.toolName }
            ?: return ValidationResult.invalid("Unsupported tool: ${tool.toolName}")
        
        val errors = mutableListOf<String>()
        
        // Check required parameters
        supportedTool.parameters.filter { it.required }.forEach { param ->
            if (!tool.parameters.containsKey(param.name)) {
                errors.add("Missing required parameter: ${param.name}")
            }
        }
        
        // Validate specific tool parameters
        when (tool.toolName) {
            "save-file" -> validateSaveFileParameters(tool, errors)
            "view" -> validateViewParameters(tool, errors)
            "str-replace-editor" -> validateStrReplaceParameters(tool, errors)
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(errors)
        }
    }
    
    private fun executeSaveFile(tool: ToolCall): ToolResult {
        val path = tool.parameters["path"]!!
        val content = tool.parameters["file_content"]!!

        return try {
            // Validate path first
            if (path.contains('\u0000') || (path.startsWith("/") && File(path).parentFile?.canWrite() != true)) {
                return ToolResult.failure("Invalid or inaccessible file path: $path")
            }

            val file = File(workingDirectory, path)

            // Try to create parent directories, but handle failures
            val parentDir = file.parentFile
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    return ToolResult.failure("Failed to create parent directories for: $path")
                }
            }

            file.writeText(content)

            ToolResult.success(
                output = "File saved successfully: $path",
                metadata = mapOf("file_size" to file.length().toString())
            )
        } catch (e: Exception) {
            ToolResult.failure("Failed to save file: ${e.message}")
        }
    }
    
    private fun executeView(tool: ToolCall): ToolResult {
        val path = tool.parameters["path"]!!
        val type = tool.parameters["type"]!!
        
        return try {
            val file = File(workingDirectory, path)
            
            if (!file.exists()) {
                return ToolResult.failure("File or directory does not exist: $path")
            }
            
            val output = when (type) {
                "file" -> {
                    if (file.isFile) {
                        file.readText()
                    } else {
                        "Path is not a file: $path"
                    }
                }
                "directory" -> {
                    if (file.isDirectory) {
                        file.listFiles()?.joinToString("\n") { it.name } ?: "Empty directory"
                    } else {
                        "Path is not a directory: $path"
                    }
                }
                else -> "Invalid type: $type. Use 'file' or 'directory'"
            }
            
            ToolResult.success(output)
        } catch (e: Exception) {
            ToolResult.failure("Failed to view path: ${e.message}")
        }
    }
    
    private fun executeStrReplaceEditor(tool: ToolCall): ToolResult {
        val path = tool.parameters["path"]!!
        val oldStr = tool.parameters["old_str"]!!
        val newStr = tool.parameters["new_str"]!!
        
        return try {
            val file = File(workingDirectory, path)
            
            if (!file.exists()) {
                return ToolResult.failure("File does not exist: $path")
            }
            
            val content = file.readText()
            if (!content.contains(oldStr)) {
                return ToolResult.failure("Text to replace not found: $oldStr")
            }
            
            val newContent = content.replace(oldStr, newStr)
            file.writeText(newContent)
            
            ToolResult.success(
                output = "Text replaced successfully in $path",
                metadata = mapOf("replacements" to "1")
            )
        } catch (e: Exception) {
            ToolResult.failure("Failed to replace text: ${e.message}")
        }
    }
    
    private fun executeCodebaseRetrieval(tool: ToolCall): ToolResult {
        val request = tool.parameters["information_request"]!!
        
        // Mock implementation - in real scenario this would integrate with actual codebase analysis
        return ToolResult.success(
            output = "Mock codebase retrieval result for: $request\n" +
                    "Found classes: User, Product, Order\n" +
                    "Found interfaces: Service, Repository\n" +
                    "Found packages: com.example.model, com.example.service",
            metadata = mapOf("mock" to "true", "request" to request)
        )
    }
    
    private fun executeAddTasks(tool: ToolCall): ToolResult {
        val name = tool.parameters["name"]!!
        val description = tool.parameters["description"]!!
        
        // Mock implementation - in real scenario this would integrate with actual task management
        return ToolResult.success(
            output = "Task added successfully: $name",
            metadata = mapOf("task_name" to name, "task_description" to description)
        )
    }
    
    private fun executeUpdateTasks(tool: ToolCall): ToolResult {
        val taskId = tool.parameters["task_id"]
        val state = tool.parameters["state"]
        
        // Mock implementation
        return ToolResult.success(
            output = "Task updated successfully${if (taskId != null) ": $taskId" else ""}",
            metadata = mapOf(
                "task_id" to (taskId ?: "batch"),
                "state" to (state ?: "unknown")
            )
        )
    }
    
    private fun executeRemoveFiles(tool: ToolCall): ToolResult {
        val filePaths = tool.parameters["file_paths"]!!
        
        return try {
            val paths = filePaths.split(",").map { it.trim() }
            val removedFiles = mutableListOf<String>()
            
            paths.forEach { path ->
                val file = File(workingDirectory, path)
                if (file.exists() && file.delete()) {
                    removedFiles.add(path)
                }
            }
            
            ToolResult.success(
                output = "Removed ${removedFiles.size} files: ${removedFiles.joinToString(", ")}",
                metadata = mapOf("removed_count" to removedFiles.size.toString())
            )
        } catch (e: Exception) {
            ToolResult.failure("Failed to remove files: ${e.message}")
        }
    }
    
    private fun validateSaveFileParameters(tool: ToolCall, errors: MutableList<String>) {
        val path = tool.parameters["path"]
        if (path.isNullOrBlank()) {
            errors.add("Path cannot be empty")
        }
        
        val content = tool.parameters["file_content"]
        if (content == null) {
            errors.add("File content is required")
        }
    }
    
    private fun validateViewParameters(tool: ToolCall, errors: MutableList<String>) {
        val path = tool.parameters["path"]
        if (path.isNullOrBlank()) {
            errors.add("Path cannot be empty")
        }
        
        val type = tool.parameters["type"]
        if (type !in listOf("file", "directory")) {
            errors.add("Type must be 'file' or 'directory'")
        }
    }
    
    private fun validateStrReplaceParameters(tool: ToolCall, errors: MutableList<String>) {
        val path = tool.parameters["path"]
        if (path.isNullOrBlank()) {
            errors.add("Path cannot be empty")
        }
        
        val oldStr = tool.parameters["old_str"]
        if (oldStr.isNullOrBlank()) {
            errors.add("Old string cannot be empty")
        }
        
        val newStr = tool.parameters["new_str"]
        if (newStr == null) {
            errors.add("New string is required")
        }
    }
}
