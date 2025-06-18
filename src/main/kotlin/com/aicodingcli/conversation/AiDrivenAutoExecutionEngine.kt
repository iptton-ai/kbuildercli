package com.aicodingcli.conversation

import kotlin.time.measureTime

/**
 * AI-driven automatic execution engine that uses AI prompts to make decisions
 */
class AiDrivenAutoExecutionEngine(
    private val conversationStateManager: ConversationStateManager,
    private val aiPromptEngine: AiPromptEngine,
    private val toolExecutor: ToolExecutor,
    private val strategy: AiExecutionStrategy = AiExecutionStrategy()
) : AutoExecutionEngine {
    
    override suspend fun executeConversation(requirement: String): ExecutionResult {
        val startTime = kotlin.time.TimeSource.Monotonic.markNow()
        
        try {
            // Handle empty requirement
            if (requirement.isBlank()) {
                return ExecutionResult.success(
                    sessionId = "empty-session",
                    finalStatus = ConversationStatus.COMPLETED,
                    executedSteps = emptyList(),
                    executionRounds = 0,
                    executionTime = startTime.elapsedNow(),
                    summary = "Empty requirement - no action taken"
                )
            }
            
            // Create conversation session
            val session = conversationStateManager.createSession(requirement)
            
            // Create execution context
            val context = ExecutionContext(
                projectPath = "",
                language = "kotlin",
                framework = "spring-boot",
                buildTool = "gradle"
            )
            
            // Start AI-driven execution
            return executeWithAiGuidance(session, context, startTime)
            
        } catch (e: Exception) {
            return ExecutionResult.failure(
                sessionId = null,
                finalStatus = ConversationStatus.FAILED,
                executedSteps = emptyList(),
                executionRounds = 0,
                executionTime = startTime.elapsedNow(),
                error = "Failed to execute conversation: ${e.message}"
            )
        }
    }
    
    override suspend fun continueConversation(sessionId: String): ExecutionResult {
        val startTime = kotlin.time.TimeSource.Monotonic.markNow()
        
        try {
            val session = conversationStateManager.getSession(sessionId)
                ?: return ExecutionResult.failure(
                    sessionId = sessionId,
                    finalStatus = ConversationStatus.FAILED,
                    executedSteps = emptyList(),
                    executionRounds = 0,
                    executionTime = startTime.elapsedNow(),
                    error = "Session not found: $sessionId"
                )
            
            val context = ExecutionContext(
                projectPath = "",
                language = "kotlin",
                framework = "spring-boot",
                buildTool = "gradle"
            )
            
            return executeWithAiGuidance(session, context, startTime)
            
        } catch (e: Exception) {
            return ExecutionResult.failure(
                sessionId = sessionId,
                finalStatus = ConversationStatus.FAILED,
                executedSteps = emptyList(),
                executionRounds = 0,
                executionTime = startTime.elapsedNow(),
                error = "Failed to continue conversation: ${e.message}"
            )
        }
    }
    
    override suspend fun executeStep(step: ExecutionStep): StepResult {
        try {
            val toolResult = toolExecutor.execute(step.toolCall)
            return StepResult.success(toolResult)
        } catch (e: Exception) {
            return StepResult.failure("Failed to execute step: ${e.message}")
        }
    }
    
    override fun setMaxExecutionRounds(maxRounds: Int) {
        if (maxRounds <= 0) {
            throw IllegalArgumentException("Max execution rounds must be positive, got: $maxRounds")
        }
        // Update strategy with new max rounds
        // Note: This would require making strategy mutable or creating a new one
    }
    
    private suspend fun executeWithAiGuidance(
        session: ConversationSession,
        context: ExecutionContext,
        startTime: kotlin.time.TimeMark
    ): ExecutionResult {
        val executedSteps = session.executionHistory.toMutableList()
        val executionRounds = mutableListOf<AiExecutionRound>()
        var currentSession = session
        var roundNumber = currentSession.state.executionRound
        
        // Update session status to executing
        currentSession = conversationStateManager.updateState(
            currentSession.id,
            currentSession.state.copy(status = ConversationStatus.EXECUTING)
        )
        
        // Initial requirement analysis
        val analysisResult = aiPromptEngine.analyzeRequirement(currentSession.requirement, context)
        
        // Main execution loop
        while (roundNumber < strategy.maxRounds) {
            roundNumber++
            
            // Get available tools
            val availableTools = toolExecutor.getSupportedTools()
                .filter { strategy.isToolAllowed(it.name) }
            
            // AI decides next action
            val actionDecision = aiPromptEngine.decideNextAction(
                requirement = currentSession.requirement,
                executionHistory = executedSteps,
                availableTools = availableTools,
                context = context
            )
            
            val round = AiExecutionRound(
                roundNumber = roundNumber,
                requirement = currentSession.requirement,
                analysisResult = if (roundNumber == 1) analysisResult else null,
                actionDecision = actionDecision,
                executionStep = null,
                safetyCheck = null,
                completionEvaluation = null
            )
            
            when (actionDecision) {
                is ActionDecision.ExecuteTool -> {
                    // Perform safety check
                    val safetyCheck = performSafetyCheck(actionDecision.toolName, actionDecision.parameters)
                    val updatedRound = round.copy(safetyCheck = safetyCheck)
                    
                    if (!safetyCheck.allowed) {
                        executionRounds.add(updatedRound)
                        return createFailureResult(
                            currentSession,
                            executedSteps,
                            roundNumber,
                            startTime,
                            "Safety check failed: ${safetyCheck.reason}"
                        )
                    }
                    
                    if (safetyCheck.requiresConfirmation) {
                        executionRounds.add(updatedRound)
                        return createWaitingResult(
                            currentSession,
                            executedSteps,
                            roundNumber,
                            startTime,
                            "User confirmation required for ${actionDecision.toolName}: ${safetyCheck.reason}"
                        )
                    }
                    
                    // Execute the tool
                    val toolCall = ToolCall(
                        toolName = actionDecision.toolName,
                        parameters = actionDecision.parameters
                    )
                    
                    val stepStartTime = kotlin.time.TimeSource.Monotonic.markNow()
                    val toolResult = toolExecutor.execute(toolCall)
                    val stepDuration = stepStartTime.elapsedNow()
                    
                    val executionStep = ExecutionStep(
                        taskId = "ai-driven-task",
                        toolCall = toolCall,
                        result = toolResult,
                        duration = stepDuration
                    )
                    
                    executedSteps.add(executionStep)
                    
                    val finalRound = updatedRound.copy(executionStep = executionStep)
                    executionRounds.add(finalRound)
                    
                    // Update session with new step
                    currentSession = currentSession.copy(
                        executionHistory = currentSession.executionHistory + executionStep,
                        state = currentSession.state.copy(executionRound = roundNumber)
                    )
                    conversationStateManager.updateSession(currentSession)
                    
                    // If tool execution failed, decide whether to continue or stop
                    if (!toolResult.success && actionDecision.confidence < strategy.confidenceThreshold) {
                        return createFailureResult(
                            currentSession,
                            executedSteps,
                            roundNumber,
                            startTime,
                            "Tool execution failed with low confidence: ${toolResult.error}"
                        )
                    }
                }
                
                is ActionDecision.Complete -> {
                    executionRounds.add(round)
                    
                    // Evaluate completion
                    val completionEvaluation = aiPromptEngine.evaluateCompletion(
                        requirement = currentSession.requirement,
                        executionHistory = executedSteps,
                        context = context
                    )
                    
                    val finalRound = round.copy(completionEvaluation = completionEvaluation)
                    executionRounds.add(finalRound)
                    
                    return createSuccessResult(
                        currentSession,
                        executedSteps,
                        roundNumber,
                        startTime,
                        actionDecision.reasoning,
                        completionEvaluation
                    )
                }
                
                is ActionDecision.WaitUser -> {
                    executionRounds.add(round)
                    return createWaitingResult(
                        currentSession,
                        executedSteps,
                        roundNumber,
                        startTime,
                        actionDecision.reasoning
                    )
                }
                
                is ActionDecision.Fail -> {
                    executionRounds.add(round)
                    return createFailureResult(
                        currentSession,
                        executedSteps,
                        roundNumber,
                        startTime,
                        actionDecision.reasoning
                    )
                }
                
                is ActionDecision.Failure -> {
                    executionRounds.add(round)
                    return createFailureResult(
                        currentSession,
                        executedSteps,
                        roundNumber,
                        startTime,
                        "AI decision failed: ${actionDecision.error}"
                    )
                }
            }
        }
        
        // Reached max rounds
        return createWaitingResult(
            currentSession,
            executedSteps,
            roundNumber,
            startTime,
            "Reached maximum execution rounds ($roundNumber). Use continueConversation to resume."
        )
    }
    
    private fun performSafetyCheck(toolName: String, parameters: Map<String, String>): SafetyCheckResult {
        if (!strategy.enableSafetyChecks) {
            return SafetyCheckResult.allowed()
        }
        
        if (!strategy.isToolAllowed(toolName)) {
            return SafetyCheckResult.denied("Tool $toolName is not allowed by current strategy")
        }
        
        if (strategy.shouldRequireConfirmation(toolName)) {
            return SafetyCheckResult.requiresConfirmation("Tool $toolName requires user confirmation")
        }
        
        // Additional safety checks based on parameters
        when (toolName) {
            "remove-files" -> {
                val paths = parameters["file_paths"]
                if (paths?.contains("..") == true) {
                    return SafetyCheckResult.denied("Path traversal detected in remove-files operation")
                }
            }
            "launch-process" -> {
                val command = parameters["command"]
                if (command?.contains("rm -rf") == true || command?.contains("del /f") == true) {
                    return SafetyCheckResult.denied("Dangerous command detected in launch-process")
                }
            }
        }
        
        return SafetyCheckResult.allowed()
    }
    
    private suspend fun createSuccessResult(
        session: ConversationSession,
        executedSteps: List<ExecutionStep>,
        rounds: Int,
        startTime: kotlin.time.TimeMark,
        reasoning: String,
        completionEvaluation: CompletionEvaluation?
    ): ExecutionResult {
        val summary = when (completionEvaluation) {
            is CompletionEvaluation.Success -> completionEvaluation.summary
            else -> reasoning
        }

        // Update session status to completed
        val completedSession = session.copy(
            state = session.state.copy(status = ConversationStatus.COMPLETED),
            executionHistory = executedSteps
        )
        conversationStateManager.updateSession(completedSession)

        return ExecutionResult.success(
            sessionId = session.id,
            finalStatus = ConversationStatus.COMPLETED,
            executedSteps = executedSteps,
            executionRounds = rounds,
            executionTime = startTime.elapsedNow(),
            summary = summary
        )
    }
    
    private suspend fun createWaitingResult(
        session: ConversationSession,
        executedSteps: List<ExecutionStep>,
        rounds: Int,
        startTime: kotlin.time.TimeMark,
        reason: String
    ): ExecutionResult {
        // Update session status to waiting for user
        val waitingSession = session.copy(
            state = session.state.copy(status = ConversationStatus.WAITING_USER),
            executionHistory = executedSteps
        )
        conversationStateManager.updateSession(waitingSession)

        return ExecutionResult.success(
            sessionId = session.id,
            finalStatus = ConversationStatus.WAITING_USER,
            executedSteps = executedSteps,
            executionRounds = rounds,
            executionTime = startTime.elapsedNow(),
            summary = reason
        )
    }
    
    private suspend fun createFailureResult(
        session: ConversationSession,
        executedSteps: List<ExecutionStep>,
        rounds: Int,
        startTime: kotlin.time.TimeMark,
        error: String
    ): ExecutionResult {
        // Update session status to failed
        val failedSession = session.copy(
            state = session.state.copy(status = ConversationStatus.FAILED),
            executionHistory = executedSteps
        )
        conversationStateManager.updateSession(failedSession)

        return ExecutionResult.failure(
            sessionId = session.id,
            finalStatus = ConversationStatus.FAILED,
            executedSteps = executedSteps,
            executionRounds = rounds,
            executionTime = startTime.elapsedNow(),
            error = error
        )
    }
}
