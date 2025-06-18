package com.aicodingcli.conversation

import kotlinx.serialization.Serializable
import java.time.Duration
import java.time.Instant
import java.util.*

/**
 * Represents a conversation session for continuous dialogue
 */
@Serializable
data class ConversationSession(
    val id: String = UUID.randomUUID().toString(),
    val requirement: String,
    val state: ConversationState,
    val tasks: List<ExecutableTask> = emptyList(),
    val executionHistory: List<ExecutionStep> = emptyList(),
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant = Instant.now(),
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant = Instant.now()
) {
    fun withUpdatedState(newState: ConversationState): ConversationSession {
        return copy(state = newState, updatedAt = Instant.now())
    }

    fun withTasks(newTasks: List<ExecutableTask>): ConversationSession {
        return copy(tasks = newTasks, updatedAt = Instant.now())
    }

    fun addExecutionStep(step: ExecutionStep): ConversationSession {
        return copy(
            executionHistory = executionHistory + step,
            updatedAt = Instant.now()
        )
    }
}

/**
 * Current state of a conversation
 */
@Serializable
data class ConversationState(
    val status: ConversationStatus,
    val currentTaskIndex: Int = 0,
    val executionRound: Int = 0,
    val context: Map<String, String> = emptyMap(),
    val errors: List<ExecutionError> = emptyList()
) {
    fun nextRound(): ConversationState {
        return copy(executionRound = executionRound + 1)
    }

    fun nextTask(): ConversationState {
        return copy(currentTaskIndex = currentTaskIndex + 1)
    }

    fun withError(error: ExecutionError): ConversationState {
        return copy(errors = errors + error)
    }

    fun withContext(key: String, value: String): ConversationState {
        return copy(context = context + (key to value))
    }
}

/**
 * Status of a conversation
 */
@Serializable
enum class ConversationStatus {
    CREATED,        // Just created, not started
    PLANNING,       // Analyzing requirements and planning tasks
    EXECUTING,      // Currently executing tasks
    WAITING_USER,   // Waiting for user input/confirmation
    COMPLETED,      // Successfully completed
    FAILED,         // Failed with errors
    CANCELLED       // Cancelled by user
}

/**
 * A task that can be executed as part of a conversation
 */
@Serializable
data class ExecutableTask(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val toolCalls: List<ToolCall>,
    val dependencies: List<String> = emptyList(),
    val priority: Int = 0,
    @Serializable(with = DurationSerializer::class)
    val estimatedDuration: Duration? = null,
    val status: TaskStatus = TaskStatus.PENDING
) {
    fun withStatus(newStatus: TaskStatus): ExecutableTask {
        return copy(status = newStatus)
    }
}

/**
 * Status of an executable task
 */
@Serializable
enum class TaskStatus {
    PENDING,        // Not started yet
    RUNNING,        // Currently executing
    COMPLETED,      // Successfully completed
    FAILED,         // Failed with errors
    SKIPPED         // Skipped due to dependencies or conditions
}

/**
 * A tool call within a task
 */
@Serializable
data class ToolCall(
    val toolName: String,
    val parameters: Map<String, String>,
    val expectedResult: String? = null
) {
    fun withParameter(key: String, value: String): ToolCall {
        return copy(parameters = parameters + (key to value))
    }
}

/**
 * A step in the execution history
 */
@Serializable
data class ExecutionStep(
    val id: String = UUID.randomUUID().toString(),
    val taskId: String,
    val toolCall: ToolCall,
    val result: ToolResult,
    @Serializable(with = InstantSerializer::class)
    val executedAt: Instant = Instant.now(),
    val duration: kotlin.time.Duration
)

/**
 * Result of a tool execution
 */
@Serializable
data class ToolResult(
    val success: Boolean,
    val output: String,
    val error: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        fun success(output: String, metadata: Map<String, String> = emptyMap()): ToolResult {
            return ToolResult(success = true, output = output, metadata = metadata)
        }

        fun failure(error: String, output: String = ""): ToolResult {
            return ToolResult(success = false, output = output, error = error)
        }
    }
}

/**
 * An execution error
 */
@Serializable
data class ExecutionError(
    val message: String,
    val code: String,
    val details: String? = null,
    @Serializable(with = InstantSerializer::class)
    val timestamp: Instant = Instant.now()
)

/**
 * Metadata about a tool
 */
@Serializable
data class ToolMetadata(
    val name: String,
    val description: String,
    val parameters: List<ToolParameter>,
    val category: String = "general"
)

/**
 * Parameter definition for a tool
 */
@Serializable
data class ToolParameter(
    val name: String,
    val type: String,
    val description: String,
    val required: Boolean = true,
    val defaultValue: String? = null
)

/**
 * Validation result for operations
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
) {
    companion object {
        fun valid(): ValidationResult = ValidationResult(true)
        fun invalid(errors: List<String>): ValidationResult = ValidationResult(false, errors)
        fun invalid(error: String): ValidationResult = ValidationResult(false, listOf(error))
    }
}

/**
 * Context for project operations
 */
data class ProjectContext(
    val projectPath: String,
    val language: String,
    val framework: String? = null,
    val buildTool: String? = null,
    val dependencies: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Result of auto execution
 */
data class ExecutionResult(
    val success: Boolean,
    val sessionId: String?,
    val finalStatus: ConversationStatus,
    val executedSteps: List<ExecutionStep>,
    val executionRounds: Int,
    val executionTime: kotlin.time.Duration,
    val summary: String? = null,
    val error: String? = null
) {
    companion object {
        fun success(
            sessionId: String,
            finalStatus: ConversationStatus,
            executedSteps: List<ExecutionStep>,
            executionRounds: Int,
            executionTime: kotlin.time.Duration,
            summary: String? = null
        ): ExecutionResult {
            return ExecutionResult(
                success = true,
                sessionId = sessionId,
                finalStatus = finalStatus,
                executedSteps = executedSteps,
                executionRounds = executionRounds,
                executionTime = executionTime,
                summary = summary
            )
        }

        fun failure(
            sessionId: String?,
            finalStatus: ConversationStatus,
            executedSteps: List<ExecutionStep>,
            executionRounds: Int,
            executionTime: kotlin.time.Duration,
            error: String
        ): ExecutionResult {
            return ExecutionResult(
                success = false,
                sessionId = sessionId,
                finalStatus = finalStatus,
                executedSteps = executedSteps,
                executionRounds = executionRounds,
                executionTime = executionTime,
                error = error
            )
        }
    }
}

/**
 * Result of a single step execution
 */
data class StepResult(
    val success: Boolean,
    val toolResult: ToolResult?,
    val error: String? = null
) {
    companion object {
        fun success(toolResult: ToolResult): StepResult {
            return StepResult(success = true, toolResult = toolResult)
        }

        fun failure(error: String): StepResult {
            return StepResult(success = false, toolResult = null, error = error)
        }
    }
}

/**
 * Parsed requirement with intent and parameters
 */
@Serializable
data class ParsedRequirement(
    val intent: Intent,
    val parameters: Map<String, String>,
    val confidence: Float = 1.0f,
    val originalText: String
)

/**
 * Intent types for requirements
 */
@Serializable
enum class Intent {
    CREATE_CLASS,           // Create a new class/interface
    CREATE_API,             // Create API endpoints
    CREATE_CONFIG,          // Create configuration files
    CREATE_TESTS,           // Create test files
    CREATE_SERVICE,         // Create service/microservice
    CREATE_SYSTEM,          // Create complete system
    CREATE_FORM,            // Create forms/UI components
    CREATE_DOCUMENTATION,   // Create documentation
    REFACTOR_CODE,          // Refactor existing code
    IMPROVE_CODE,           // General code improvement
    OPTIMIZE_PERFORMANCE,   // Performance optimization
    FIX_BUG,               // Bug fixing
    ADD_FEATURE,           // Add new feature
    REMOVE_FEATURE,        // Remove feature
    UPDATE_DEPENDENCY,     // Update dependencies
    UNKNOWN                // Unknown intent
}
