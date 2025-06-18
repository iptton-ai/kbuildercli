package com.aicodingcli.conversation

import com.aicodingcli.ai.AiService
import com.aicodingcli.ai.AiRequest
import com.aicodingcli.ai.AiMessage
import com.aicodingcli.ai.MessageRole
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * AI Prompt Engine for driving automatic tool calls
 */
interface AiPromptEngine {
    /**
     * Analyze requirement and suggest next actions
     */
    suspend fun analyzeRequirement(requirement: String, context: ExecutionContext): AnalysisResult
    
    /**
     * Decide next tool call based on current state
     */
    suspend fun decideNextAction(
        requirement: String,
        executionHistory: List<ExecutionStep>,
        availableTools: List<ToolMetadata>,
        context: ExecutionContext
    ): ActionDecision
    
    /**
     * Evaluate if the requirement has been completed
     */
    suspend fun evaluateCompletion(
        requirement: String,
        executionHistory: List<ExecutionStep>,
        context: ExecutionContext
    ): CompletionEvaluation
}

/**
 * Default implementation of AI Prompt Engine
 */
class DefaultAiPromptEngine(
    private val aiService: AiService,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : AiPromptEngine {
    
    override suspend fun analyzeRequirement(requirement: String, context: ExecutionContext): AnalysisResult {
        val prompt = buildAnalysisPrompt(requirement, context)
        
        try {
            val response = aiService.chat(AiRequest(
                messages = listOf(AiMessage(MessageRole.USER, prompt)),
                model = "gpt-4",
                temperature = 0.3f
            ))
            
            return parseAnalysisResponse(response.content)
        } catch (e: Exception) {
            return AnalysisResult.failure("Failed to analyze requirement: ${e.message}")
        }
    }
    
    override suspend fun decideNextAction(
        requirement: String,
        executionHistory: List<ExecutionStep>,
        availableTools: List<ToolMetadata>,
        context: ExecutionContext
    ): ActionDecision {
        val prompt = buildActionDecisionPrompt(requirement, executionHistory, availableTools, context)
        
        try {
            val response = aiService.chat(AiRequest(
                messages = listOf(AiMessage(MessageRole.USER, prompt)),
                model = "gpt-4",
                temperature = 0.2f
            ))
            
            return parseActionDecisionResponse(response.content)
        } catch (e: Exception) {
            return ActionDecision.failure("Failed to decide next action: ${e.message}")
        }
    }
    
    override suspend fun evaluateCompletion(
        requirement: String,
        executionHistory: List<ExecutionStep>,
        context: ExecutionContext
    ): CompletionEvaluation {
        val prompt = buildCompletionEvaluationPrompt(requirement, executionHistory, context)
        
        try {
            val response = aiService.chat(AiRequest(
                messages = listOf(AiMessage(MessageRole.USER, prompt)),
                model = "gpt-4",
                temperature = 0.1f
            ))
            
            return parseCompletionEvaluationResponse(response.content)
        } catch (e: Exception) {
            return CompletionEvaluation.failure("Failed to evaluate completion: ${e.message}")
        }
    }
    
    private fun buildAnalysisPrompt(requirement: String, context: ExecutionContext): String {
        return """
            You are an AI assistant that analyzes programming requirements and breaks them down into actionable steps.
            
            **Requirement**: $requirement
            
            **Context**:
            - Project Path: ${context.projectPath}
            - Language: ${context.language}
            - Framework: ${context.framework}
            - Build Tool: ${context.buildTool}
            
            Please analyze this requirement and provide:
            1. **Intent**: What is the user trying to achieve?
            2. **Complexity**: How complex is this requirement? (SIMPLE, MODERATE, COMPLEX)
            3. **Category**: What category does this fall into? (FILE_OPERATION, CODE_GENERATION, TESTING, REFACTORING, ANALYSIS, OTHER)
            4. **Prerequisites**: What needs to exist before this can be completed?
            5. **Estimated Steps**: How many steps do you estimate this will take?
            
            Respond in JSON format:
            {
                "intent": "string",
                "complexity": "SIMPLE|MODERATE|COMPLEX",
                "category": "FILE_OPERATION|CODE_GENERATION|TESTING|REFACTORING|ANALYSIS|OTHER",
                "prerequisites": ["string"],
                "estimatedSteps": number,
                "reasoning": "string"
            }
        """.trimIndent()
    }
    
    private fun buildActionDecisionPrompt(
        requirement: String,
        executionHistory: List<ExecutionStep>,
        availableTools: List<ToolMetadata>,
        context: ExecutionContext
    ): String {
        val historyText = if (executionHistory.isEmpty()) {
            "No previous actions taken."
        } else {
            executionHistory.takeLast(5).joinToString("\n") { step ->
                "- ${step.toolCall.toolName}: ${step.result.success} - ${step.result.output.take(100)}"
            }
        }
        
        val toolsText = availableTools.joinToString("\n") { tool ->
            "- ${tool.name}: ${tool.description}"
        }
        
        return """
            You are an AI assistant that decides the next action to take in completing a programming requirement.
            
            **Original Requirement**: $requirement
            
            **Execution History** (last 5 actions):
            $historyText
            
            **Available Tools**:
            $toolsText
            
            **Context**:
            - Project Path: ${context.projectPath}
            - Language: ${context.language}
            - Framework: ${context.framework}
            
            Based on the requirement and execution history, decide the next action to take.
            
            Respond in JSON format:
            {
                "action": "EXECUTE_TOOL|COMPLETE|WAIT_USER|FAIL",
                "toolName": "string (if action is EXECUTE_TOOL)",
                "parameters": {"key": "value"} (if action is EXECUTE_TOOL),
                "reasoning": "string",
                "confidence": number (0.0 to 1.0)
            }
            
            Guidelines:
            - If the requirement seems complete, use action "COMPLETE"
            - If you need user input or clarification, use action "WAIT_USER"
            - If something went wrong and cannot be fixed, use action "FAIL"
            - Otherwise, use action "EXECUTE_TOOL" with the appropriate tool and parameters
            - Be conservative with file operations and always check if files exist first
            - Prefer using 'view' tool to understand current state before making changes
        """.trimIndent()
    }
    
    private fun buildCompletionEvaluationPrompt(
        requirement: String,
        executionHistory: List<ExecutionStep>,
        context: ExecutionContext
    ): String {
        val historyText = executionHistory.joinToString("\n") { step ->
            "- ${step.toolCall.toolName}: ${if (step.result.success) "SUCCESS" else "FAILED"} - ${step.result.output.take(100)}"
        }
        
        return """
            You are an AI assistant that evaluates whether a programming requirement has been completed.
            
            **Original Requirement**: $requirement
            
            **Execution History**:
            $historyText
            
            **Context**:
            - Project Path: ${context.projectPath}
            - Language: ${context.language}
            - Framework: ${context.framework}
            
            Evaluate whether the original requirement has been successfully completed based on the execution history.
            
            Respond in JSON format:
            {
                "completed": boolean,
                "completionPercentage": number (0 to 100),
                "missingItems": ["string"],
                "summary": "string",
                "nextSteps": ["string"] (if not completed)
            }
        """.trimIndent()
    }
    
    private fun parseAnalysisResponse(response: String): AnalysisResult {
        return try {
            val jsonResponse = json.decodeFromString<AnalysisResponseJson>(response)
            AnalysisResult.success(
                intent = jsonResponse.intent,
                complexity = RequirementComplexity.valueOf(jsonResponse.complexity),
                category = RequirementCategory.valueOf(jsonResponse.category),
                prerequisites = jsonResponse.prerequisites,
                estimatedSteps = jsonResponse.estimatedSteps,
                reasoning = jsonResponse.reasoning
            )
        } catch (e: Exception) {
            AnalysisResult.failure("Failed to parse analysis response: ${e.message}")
        }
    }
    
    private fun parseActionDecisionResponse(response: String): ActionDecision {
        return try {
            val jsonResponse = json.decodeFromString<ActionDecisionResponseJson>(response)
            when (jsonResponse.action) {
                "EXECUTE_TOOL" -> ActionDecision.executeTool(
                    toolName = jsonResponse.toolName ?: "",
                    parameters = jsonResponse.parameters ?: emptyMap(),
                    reasoning = jsonResponse.reasoning,
                    confidence = jsonResponse.confidence
                )
                "COMPLETE" -> ActionDecision.complete(jsonResponse.reasoning)
                "WAIT_USER" -> ActionDecision.waitUser(jsonResponse.reasoning)
                "FAIL" -> ActionDecision.fail(jsonResponse.reasoning)
                else -> ActionDecision.failure("Unknown action: ${jsonResponse.action}")
            }
        } catch (e: Exception) {
            ActionDecision.failure("Failed to parse action decision response: ${e.message}")
        }
    }
    
    private fun parseCompletionEvaluationResponse(response: String): CompletionEvaluation {
        return try {
            val jsonResponse = json.decodeFromString<CompletionEvaluationResponseJson>(response)
            CompletionEvaluation.success(
                completed = jsonResponse.completed,
                completionPercentage = jsonResponse.completionPercentage,
                missingItems = jsonResponse.missingItems,
                summary = jsonResponse.summary,
                nextSteps = jsonResponse.nextSteps
            )
        } catch (e: Exception) {
            CompletionEvaluation.failure("Failed to parse completion evaluation response: ${e.message}")
        }
    }
}

// JSON response data classes
@Serializable
private data class AnalysisResponseJson(
    val intent: String,
    val complexity: String,
    val category: String,
    val prerequisites: List<String>,
    val estimatedSteps: Int,
    val reasoning: String
)

@Serializable
private data class ActionDecisionResponseJson(
    val action: String,
    val toolName: String? = null,
    val parameters: Map<String, String>? = null,
    val reasoning: String,
    val confidence: Double
)

@Serializable
private data class CompletionEvaluationResponseJson(
    val completed: Boolean,
    val completionPercentage: Int,
    val missingItems: List<String>,
    val summary: String,
    val nextSteps: List<String>
)
