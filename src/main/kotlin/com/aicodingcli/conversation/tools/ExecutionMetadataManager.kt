package com.aicodingcli.conversation.tools

import com.aicodingcli.conversation.ToolCall
import com.aicodingcli.conversation.ToolResult
import java.time.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

/**
 * Manages execution metadata for tool calls
 */
class ExecutionMetadataManager {
    
    /**
     * Enhance tool result with execution metadata
     */
    fun enhanceWithMetadata(
        toolCall: ToolCall,
        result: ToolResult,
        startTime: Instant,
        executionDuration: Duration
    ): ToolResult {
        val metadata = result.metadata.toMutableMap()
        
        // Add standard execution metadata
        metadata["tool_name"] = toolCall.toolName
        metadata["execution_time"] = executionDuration.toString()
        metadata["executed_at"] = startTime.toString()
        metadata["success"] = result.success.toString()
        
        // Add parameter count
        metadata["parameter_count"] = toolCall.parameters.size.toString()
        
        // Add output size
        metadata["output_size"] = result.output.length.toString()
        
        return result.copy(metadata = metadata)
    }
    
    /**
     * Measure execution time for an operation
     */
    suspend fun <T> measureExecution(
        operation: suspend () -> T
    ): Pair<T, Duration> {
        var result: T? = null
        val duration = measureTime {
            result = operation()
        }
        return Pair(result!!, duration)
    }
}

/**
 * Execution context for tool calls
 */
data class ExecutionContext(
    val toolCall: ToolCall,
    val workingDirectory: String,
    val startTime: Instant = Instant.now(),
    val metadata: MutableMap<String, String> = mutableMapOf()
) {
    fun addMetadata(key: String, value: String) {
        metadata[key] = value
    }
    
    fun getElapsedTime(): kotlin.time.Duration {
        val now = Instant.now()
        return (now.toEpochMilli() - startTime.toEpochMilli()).milliseconds
    }
}

/**
 * Tool execution statistics
 */
data class ToolExecutionStats(
    val toolName: String,
    val executionCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val averageExecutionTime: Duration,
    val totalExecutionTime: Duration
) {
    val successRate: Double
        get() = if (executionCount > 0) successCount.toDouble() / executionCount else 0.0
}

/**
 * Collects and manages tool execution statistics
 */
class ToolExecutionStatsCollector {
    private val executions = mutableMapOf<String, MutableList<ToolExecutionRecord>>()
    
    data class ToolExecutionRecord(
        val toolName: String,
        val success: Boolean,
        val executionTime: Duration,
        val timestamp: Instant
    )
    
    /**
     * Record a tool execution
     */
    fun recordExecution(
        toolName: String,
        success: Boolean,
        executionTime: Duration
    ) {
        val record = ToolExecutionRecord(
            toolName = toolName,
            success = success,
            executionTime = executionTime,
            timestamp = Instant.now()
        )
        
        executions.getOrPut(toolName) { mutableListOf() }.add(record)
    }
    
    /**
     * Get statistics for a specific tool
     */
    fun getStatsForTool(toolName: String): ToolExecutionStats? {
        val records = executions[toolName] ?: return null
        
        if (records.isEmpty()) return null
        
        val executionCount = records.size
        val successCount = records.count { it.success }
        val failureCount = executionCount - successCount
        val totalTime = records.fold(Duration.ZERO) { acc, record -> acc + record.executionTime }
        val averageTime = totalTime / executionCount
        
        return ToolExecutionStats(
            toolName = toolName,
            executionCount = executionCount,
            successCount = successCount,
            failureCount = failureCount,
            averageExecutionTime = averageTime,
            totalExecutionTime = totalTime
        )
    }
    
    /**
     * Get statistics for all tools
     */
    fun getAllStats(): List<ToolExecutionStats> {
        return executions.keys.mapNotNull { getStatsForTool(it) }
    }
    
    /**
     * Clear all statistics
     */
    fun clearStats() {
        executions.clear()
    }
}
