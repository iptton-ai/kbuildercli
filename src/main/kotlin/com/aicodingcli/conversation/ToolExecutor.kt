package com.aicodingcli.conversation

import com.aicodingcli.conversation.tools.*
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
 * Default implementation of ToolExecutor using handler-based architecture
 */
class DefaultToolExecutor(
    private val workingDirectory: String = System.getProperty("user.dir"),
    private val handlerRegistry: ToolHandlerRegistry = DefaultToolHandlerFactory.createDefaultRegistry(),
    private val metadataManager: ExecutionMetadataManager = ExecutionMetadataManager(),
    private val statsCollector: ToolExecutionStatsCollector = ToolExecutionStatsCollector()
) : ToolExecutor {
    
    override suspend fun execute(tool: ToolCall): ToolResult {
        val startTime = Instant.now()

        try {
            // Get handler for the tool
            val handler = handlerRegistry.getHandler(tool.toolName)
                ?: return ToolResult.failure("Unsupported tool: ${tool.toolName}")

            // Measure execution time and execute the tool
            var result: ToolResult
            val executionTime = measureTime {
                result = handler.execute(tool.parameters, workingDirectory)
            }

            // Record statistics
            statsCollector.recordExecution(
                toolName = tool.toolName,
                success = result.success,
                executionTime = executionTime
            )

            // Enhance with metadata
            return metadataManager.enhanceWithMetadata(tool, result, startTime, executionTime)

        } catch (e: Exception) {
            val executionTime = measureTime { /* Exception occurred */ }
            statsCollector.recordExecution(
                toolName = tool.toolName,
                success = false,
                executionTime = executionTime
            )

            return ToolResult.failure(
                error = "Tool execution failed: ${e.message}",
                output = e.stackTraceToString()
            )
        }
    }
    
    override fun getSupportedTools(): List<ToolMetadata> {
        return handlerRegistry.getAllToolMetadata()
    }
    
    override suspend fun validateParameters(tool: ToolCall): ValidationResult {
        val handler = handlerRegistry.getHandler(tool.toolName)
            ?: return ValidationResult.invalid("Unsupported tool: ${tool.toolName}")

        return handler.validateParameters(tool.parameters)
    }

    /**
     * Get execution statistics for tools
     */
    fun getExecutionStats(): List<ToolExecutionStats> {
        return statsCollector.getAllStats()
    }

    /**
     * Get execution statistics for a specific tool
     */
    fun getExecutionStatsForTool(toolName: String): ToolExecutionStats? {
        return statsCollector.getStatsForTool(toolName)
    }

    /**
     * Clear execution statistics
     */
    fun clearExecutionStats() {
        statsCollector.clearStats()
    }
}
