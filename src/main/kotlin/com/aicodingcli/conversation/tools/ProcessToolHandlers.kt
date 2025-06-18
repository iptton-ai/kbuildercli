package com.aicodingcli.conversation.tools

import com.aicodingcli.conversation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Handler for launch-process tool
 */
class LaunchProcessHandler : BaseToolHandler("launch-process") {
    
    override suspend fun executeInternal(parameters: Map<String, String>, workingDirectory: String): ToolResult {
        val command = parameters["command"]!!
        val wait = parameters["wait"]?.toBoolean() ?: false
        val maxWaitSeconds = parameters["max_wait_seconds"]?.toLong() ?: 30L
        
        return withContext(Dispatchers.IO) {
            try {
                val processBuilder = ProcessBuilder()
                    .command(command.split(" "))
                    .directory(File(workingDirectory))
                    .redirectErrorStream(true)
                
                val process = processBuilder.start()
                
                if (wait) {
                    val finished = process.waitFor(maxWaitSeconds, TimeUnit.SECONDS)
                    val output = process.inputStream.bufferedReader().readText()
                    val exitCode = if (finished) process.exitValue() else -1
                    
                    if (!finished) {
                        process.destroyForcibly()
                        return@withContext ToolResult.failure(
                            error = "Process timed out after $maxWaitSeconds seconds",
                            output = output
                        )
                    }
                    
                    if (exitCode == 0) {
                        ToolResult.success(
                            output = output,
                            metadata = mapOf(
                                "exit_code" to exitCode.toString(),
                                "command" to command,
                                "duration" to "${maxWaitSeconds}s"
                            )
                        )
                    } else {
                        ToolResult.failure(
                            error = "Process exited with code $exitCode",
                            output = output
                        )
                    }
                } else {
                    // Non-blocking execution
                    ToolResult.success(
                        output = "Process launched successfully: $command",
                        metadata = mapOf(
                            "command" to command,
                            "pid" to process.pid().toString(),
                            "blocking" to "false"
                        )
                    )
                }
            } catch (e: Exception) {
                ToolResult.failure("Failed to launch process: ${e.message}")
            }
        }
    }
    
    override fun validateParameters(parameters: Map<String, String>): ValidationResult {
        val errors = checkRequiredParameters(parameters, listOf("command")).toMutableList()

        val wait = parameters["wait"]
        if (wait != null && wait !in listOf("true", "false")) {
            errors.add("wait parameter must be 'true' or 'false'")
        }

        val maxWaitSeconds = parameters["max_wait_seconds"]
        if (maxWaitSeconds != null) {
            try {
                val seconds = maxWaitSeconds.toLong()
                if (seconds <= 0) {
                    errors.add("max_wait_seconds must be positive")
                }
            } catch (e: NumberFormatException) {
                errors.add("max_wait_seconds must be a valid number")
            }
        }

        return if (errors.isEmpty()) ValidationResult.valid() else ValidationResult.invalid(errors)
    }
    
    override fun getMetadata(): ToolMetadata {
        return ToolMetadata(
            name = toolName,
            description = "Launch a process with a shell command",
            parameters = listOf(
                ToolParameter("command", "string", "The shell command to execute", required = true),
                ToolParameter("wait", "boolean", "Whether to wait for the command to complete", required = false),
                ToolParameter("max_wait_seconds", "number", "Number of seconds to wait for the command to complete", required = false)
            ),
            category = "process"
        )
    }
}

/**
 * Handler for read-terminal tool
 */
class ReadTerminalHandler : BaseToolHandler("read-terminal") {
    
    override suspend fun executeInternal(parameters: Map<String, String>, workingDirectory: String): ToolResult {
        // Mock implementation - in real scenario this would read from actual terminal
        return ToolResult.success(
            output = "Mock terminal output - this would read from the active terminal",
            metadata = mapOf("mock" to "true")
        )
    }
    
    override fun validateParameters(parameters: Map<String, String>): ValidationResult {
        // read-terminal has optional parameters
        return ValidationResult.valid()
    }
    
    override fun getMetadata(): ToolMetadata {
        return ToolMetadata(
            name = toolName,
            description = "Read output from the active or most-recently used terminal",
            parameters = listOf(
                ToolParameter("only_selected", "boolean", "Whether to read only the selected text in the terminal", required = false)
            ),
            category = "process"
        )
    }
}

/**
 * Handler for diagnostics tool
 */
class DiagnosticsHandler : BaseToolHandler("diagnostics") {
    
    override suspend fun executeInternal(parameters: Map<String, String>, workingDirectory: String): ToolResult {
        val paths = parameters["paths"]?.split(",")?.map { it.trim() } ?: emptyList()
        
        // Mock implementation - in real scenario this would get actual IDE diagnostics
        val diagnostics = paths.map { path ->
            "File: $path\n" +
            "  - Warning: Unused import at line 5\n" +
            "  - Error: Unresolved reference at line 12\n"
        }.joinToString("\n")
        
        return ToolResult.success(
            output = if (diagnostics.isNotEmpty()) diagnostics else "No diagnostics found",
            metadata = mapOf(
                "paths_count" to paths.size.toString(),
                "mock" to "true"
            )
        )
    }
    
    override fun validateParameters(parameters: Map<String, String>): ValidationResult {
        val errors = checkRequiredParameters(parameters, listOf("paths"))
        return if (errors.isEmpty()) ValidationResult.valid() else ValidationResult.invalid(errors)
    }
    
    override fun getMetadata(): ToolMetadata {
        return ToolMetadata(
            name = toolName,
            description = "Get issues (errors, warnings, etc.) from the IDE",
            parameters = listOf(
                ToolParameter("paths", "array", "Required list of file paths to get issues for from the IDE", required = true)
            ),
            category = "analysis"
        )
    }
}
