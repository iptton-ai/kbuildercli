package com.aicodingcli.conversation

import kotlinx.serialization.Serializable

/**
 * Execution context for AI prompt decisions
 */
@Serializable
data class ExecutionContext(
    val projectPath: String,
    val language: String,
    val framework: String,
    val buildTool: String,
    val workingDirectory: String = System.getProperty("user.dir"),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Result of requirement analysis
 */
sealed class AnalysisResult {
    data class Success(
        val intent: String,
        val complexity: RequirementComplexity,
        val category: RequirementCategory,
        val prerequisites: List<String>,
        val estimatedSteps: Int,
        val reasoning: String
    ) : AnalysisResult()
    
    data class Failure(val error: String) : AnalysisResult()
    
    companion object {
        fun success(
            intent: String,
            complexity: RequirementComplexity,
            category: RequirementCategory,
            prerequisites: List<String>,
            estimatedSteps: Int,
            reasoning: String
        ): AnalysisResult = Success(intent, complexity, category, prerequisites, estimatedSteps, reasoning)
        
        fun failure(error: String): AnalysisResult = Failure(error)
    }
}

/**
 * Decision for next action to take
 */
sealed class ActionDecision {
    data class ExecuteTool(
        val toolName: String,
        val parameters: Map<String, String>,
        val reasoning: String,
        val confidence: Double
    ) : ActionDecision()
    
    data class Complete(val reasoning: String) : ActionDecision()
    data class WaitUser(val reasoning: String) : ActionDecision()
    data class Fail(val reasoning: String) : ActionDecision()
    data class Failure(val error: String) : ActionDecision()
    
    companion object {
        fun executeTool(
            toolName: String,
            parameters: Map<String, String>,
            reasoning: String,
            confidence: Double
        ): ActionDecision = ExecuteTool(toolName, parameters, reasoning, confidence)
        
        fun complete(reasoning: String): ActionDecision = Complete(reasoning)
        fun waitUser(reasoning: String): ActionDecision = WaitUser(reasoning)
        fun fail(reasoning: String): ActionDecision = Fail(reasoning)
        fun failure(error: String): ActionDecision = Failure(error)
    }
}

/**
 * Evaluation of requirement completion
 */
sealed class CompletionEvaluation {
    data class Success(
        val completed: Boolean,
        val completionPercentage: Int,
        val missingItems: List<String>,
        val summary: String,
        val nextSteps: List<String>
    ) : CompletionEvaluation()
    
    data class Failure(val error: String) : CompletionEvaluation()
    
    companion object {
        fun success(
            completed: Boolean,
            completionPercentage: Int,
            missingItems: List<String>,
            summary: String,
            nextSteps: List<String>
        ): CompletionEvaluation = Success(completed, completionPercentage, missingItems, summary, nextSteps)
        
        fun failure(error: String): CompletionEvaluation = Failure(error)
    }
}

/**
 * Complexity levels for requirements
 */
enum class RequirementComplexity {
    SIMPLE,     // Single file operation, basic task
    MODERATE,   // Multiple files, some logic
    COMPLEX     // Multiple components, complex logic, dependencies
}

/**
 * Categories for requirements
 */
enum class RequirementCategory {
    FILE_OPERATION,     // Creating, reading, writing files
    CODE_GENERATION,    // Generating classes, functions, tests
    TESTING,           // Running tests, creating test cases
    REFACTORING,       // Code improvements, restructuring
    ANALYSIS,          // Code analysis, diagnostics
    BUILD_OPERATION,   // Build, compile, package operations
    GIT_OPERATION,     // Version control operations
    CONFIGURATION,     // Configuration file changes
    DOCUMENTATION,     // Creating or updating documentation
    OTHER              // Other types of operations
}

/**
 * AI-driven execution strategy
 */
data class AiExecutionStrategy(
    val maxRounds: Int = 25,
    val confidenceThreshold: Double = 0.7,
    val enableSafetyChecks: Boolean = true,
    val allowFileOperations: Boolean = true,
    val allowNetworkOperations: Boolean = false,
    val allowSystemCommands: Boolean = false,
    val requireUserConfirmation: List<String> = listOf("remove-files", "git-operations")
) {
    fun shouldRequireConfirmation(toolName: String): Boolean {
        return requireUserConfirmation.contains(toolName)
    }
    
    fun isToolAllowed(toolName: String): Boolean {
        return when {
            !allowFileOperations && toolName in listOf("save-file", "str-replace-editor", "remove-files") -> false
            !allowNetworkOperations && toolName in listOf("web-search", "web-fetch", "open-browser") -> false
            !allowSystemCommands && toolName in listOf("launch-process", "git-operations") -> false
            else -> true
        }
    }
}

/**
 * Safety check result
 */
data class SafetyCheckResult(
    val allowed: Boolean,
    val reason: String,
    val requiresConfirmation: Boolean = false
) {
    companion object {
        fun allowed(): SafetyCheckResult = SafetyCheckResult(true, "Operation allowed")
        fun denied(reason: String): SafetyCheckResult = SafetyCheckResult(false, reason)
        fun requiresConfirmation(reason: String): SafetyCheckResult = 
            SafetyCheckResult(true, reason, requiresConfirmation = true)
    }
}

/**
 * AI execution round information
 */
data class AiExecutionRound(
    val roundNumber: Int,
    val requirement: String,
    val analysisResult: AnalysisResult?,
    val actionDecision: ActionDecision?,
    val executionStep: ExecutionStep?,
    val safetyCheck: SafetyCheckResult?,
    val completionEvaluation: CompletionEvaluation?,
    val timestamp: java.time.Instant = java.time.Instant.now()
) {
    val isSuccessful: Boolean
        get() = executionStep?.result?.success == true
    
    val shouldContinue: Boolean
        get() = when (actionDecision) {
            is ActionDecision.ExecuteTool -> true
            is ActionDecision.Complete -> false
            is ActionDecision.WaitUser -> false
            is ActionDecision.Fail -> false
            is ActionDecision.Failure -> false
            null -> false
        }
}

// ToolMetadata, ToolParameter, ToolExample, and ToolRiskLevel are already defined in ConversationModels.kt
