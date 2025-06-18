package com.aicodingcli.conversation.tools

import com.aicodingcli.conversation.*
import java.io.File

/**
 * Handler for save-file tool
 */
class SaveFileHandler : BaseToolHandler("save-file") {
    
    override suspend fun executeInternal(parameters: Map<String, String>, workingDirectory: String): ToolResult {
        val path = parameters["path"]!!
        val content = parameters["file_content"]!!
        
        // Validate path
        if (path.contains('\u0000') || (path.startsWith("/") && File(path).parentFile?.canWrite() != true)) {
            return ToolResult.failure("Invalid or inaccessible file path: $path")
        }
        
        val file = File(workingDirectory, path)
        
        // Try to create parent directories
        val parentDir = file.parentFile
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                return ToolResult.failure("Failed to create parent directories for: $path")
            }
        }
        
        file.writeText(content)
        
        return ToolResult.success(
            output = "File saved successfully: $path",
            metadata = mapOf("file_size" to file.length().toString())
        )
    }
    
    override fun validateParameters(parameters: Map<String, String>): ValidationResult {
        val errors = checkRequiredParameters(parameters, listOf("path", "file_content")).toMutableList()

        val path = parameters["path"]
        if (path.isNullOrBlank()) {
            errors.add("Path cannot be empty")
        }

        return if (errors.isEmpty()) ValidationResult.valid() else ValidationResult.invalid(errors)
    }
    
    override fun getMetadata(): ToolMetadata {
        return ToolMetadata(
            name = toolName,
            description = "Create a new file with content",
            parameters = listOf(
                ToolParameter("path", "string", "File path relative to working directory", required = true),
                ToolParameter("file_content", "string", "Content to write to the file", required = true)
            ),
            category = "file"
        )
    }
}

/**
 * Handler for view tool
 */
class ViewHandler : BaseToolHandler("view") {
    
    override suspend fun executeInternal(parameters: Map<String, String>, workingDirectory: String): ToolResult {
        val path = parameters["path"]!!
        val type = parameters["type"]!!
        
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
        
        return ToolResult.success(output)
    }
    
    override fun validateParameters(parameters: Map<String, String>): ValidationResult {
        val errors = checkRequiredParameters(parameters, listOf("path", "type")).toMutableList()

        val type = parameters["type"]
        if (type !in listOf("file", "directory")) {
            errors.add("Type must be 'file' or 'directory'")
        }

        return if (errors.isEmpty()) ValidationResult.valid() else ValidationResult.invalid(errors)
    }
    
    override fun getMetadata(): ToolMetadata {
        return ToolMetadata(
            name = toolName,
            description = "View file or directory contents",
            parameters = listOf(
                ToolParameter("path", "string", "Path to view", required = true),
                ToolParameter("type", "string", "Type: 'file' or 'directory'", required = true)
            ),
            category = "file"
        )
    }
}

/**
 * Handler for str-replace-editor tool
 */
class StrReplaceEditorHandler : BaseToolHandler("str-replace-editor") {
    
    override suspend fun executeInternal(parameters: Map<String, String>, workingDirectory: String): ToolResult {
        val path = parameters["path"]!!
        val oldStr = parameters["old_str"]!!
        val newStr = parameters["new_str"]!!
        
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
        
        return ToolResult.success(
            output = "Text replaced successfully in $path",
            metadata = mapOf("replacements" to "1")
        )
    }
    
    override fun validateParameters(parameters: Map<String, String>): ValidationResult {
        val errors = checkRequiredParameters(parameters, listOf("path", "old_str", "new_str"))
        return if (errors.isEmpty()) ValidationResult.valid() else ValidationResult.invalid(errors)
    }
    
    override fun getMetadata(): ToolMetadata {
        return ToolMetadata(
            name = toolName,
            description = "Replace text in a file",
            parameters = listOf(
                ToolParameter("path", "string", "File path", required = true),
                ToolParameter("old_str", "string", "Text to replace", required = true),
                ToolParameter("new_str", "string", "Replacement text", required = true)
            ),
            category = "file"
        )
    }
}

/**
 * Handler for remove-files tool
 */
class RemoveFilesHandler : BaseToolHandler("remove-files") {
    
    override suspend fun executeInternal(parameters: Map<String, String>, workingDirectory: String): ToolResult {
        val filePaths = parameters["file_paths"]!!
        val paths = filePaths.split(",").map { it.trim() }
        val removedFiles = mutableListOf<String>()
        
        paths.forEach { path ->
            val file = File(workingDirectory, path)
            if (file.exists() && file.delete()) {
                removedFiles.add(path)
            }
        }
        
        return ToolResult.success(
            output = "Removed ${removedFiles.size} files: ${removedFiles.joinToString(", ")}",
            metadata = mapOf("removed_count" to removedFiles.size.toString())
        )
    }
    
    override fun validateParameters(parameters: Map<String, String>): ValidationResult {
        val errors = checkRequiredParameters(parameters, listOf("file_paths"))
        return if (errors.isEmpty()) ValidationResult.valid() else ValidationResult.invalid(errors)
    }
    
    override fun getMetadata(): ToolMetadata {
        return ToolMetadata(
            name = toolName,
            description = "Remove files from filesystem",
            parameters = listOf(
                ToolParameter("file_paths", "array", "Array of file paths to remove", required = true)
            ),
            category = "file"
        )
    }
}
