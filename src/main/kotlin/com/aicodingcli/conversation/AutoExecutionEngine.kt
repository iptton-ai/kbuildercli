package com.aicodingcli.conversation

import kotlin.time.measureTime

/**
 * Interface for automatic execution of conversations
 */
interface AutoExecutionEngine {
    /**
     * Execute a conversation from a requirement
     */
    suspend fun executeConversation(requirement: String): ExecutionResult
    
    /**
     * Continue an existing conversation
     */
    suspend fun continueConversation(sessionId: String): ExecutionResult
    
    /**
     * Execute a single step
     */
    suspend fun executeStep(step: ExecutionStep): StepResult
    
    /**
     * Set maximum execution rounds
     */
    fun setMaxExecutionRounds(maxRounds: Int)
}

/**
 * Default implementation of AutoExecutionEngine
 */
class DefaultAutoExecutionEngine(
    private val conversationStateManager: ConversationStateManager,
    private val taskDecomposer: TaskDecomposer,
    private val requirementParser: RequirementParser,
    private val toolExecutor: ToolExecutor,
    private var maxExecutionRounds: Int = 25
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
            
            // Parse requirement
            val parsedRequirement = requirementParser.parse(requirement)
            
            // Create project context (simplified for now)
            val projectContext = ProjectContext(
                projectPath = ".",
                language = "kotlin",
                framework = "spring-boot",
                buildTool = "gradle"
            )
            
            // Decompose into tasks
            val tasks = taskDecomposer.decompose(requirement, projectContext)
            
            // Update session with tasks
            val sessionWithTasks = session.withTasks(tasks)
            conversationStateManager.updateState(
                sessionWithTasks.id,
                sessionWithTasks.state.copy(status = ConversationStatus.PLANNING)
            )
            
            // Execute tasks
            return executeTasksInSession(sessionWithTasks, startTime)
            
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
            
            return executeTasksInSession(session, startTime)
            
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
        this.maxExecutionRounds = maxRounds
    }
    
    private suspend fun executeTasksInSession(
        session: ConversationSession,
        startTime: kotlin.time.TimeMark
    ): ExecutionResult {
        val executedSteps = mutableListOf<ExecutionStep>()
        var currentSession = session
        var executionRounds = currentSession.state.executionRound
        
        try {
            // Update status to executing
            currentSession = conversationStateManager.updateState(
                currentSession.id,
                currentSession.state.copy(status = ConversationStatus.EXECUTING)
            )
            
            // Execute tasks
            for (task in currentSession.tasks) {
                if (executionRounds >= maxExecutionRounds) {
                    // Reached max rounds, pause execution
                    val pausedSession = conversationStateManager.updateState(
                        currentSession.id,
                        currentSession.state.copy(
                            status = ConversationStatus.WAITING_USER,
                            executionRound = executionRounds
                        )
                    )
                    
                    return ExecutionResult.success(
                        sessionId = pausedSession.id,
                        finalStatus = ConversationStatus.WAITING_USER,
                        executedSteps = executedSteps,
                        executionRounds = executionRounds,
                        executionTime = startTime.elapsedNow(),
                        summary = "Execution paused after $executionRounds rounds. Use continueConversation to resume."
                    )
                }
                
                // Execute each tool call in the task
                for (toolCall in task.toolCalls) {
                    executionRounds++
                    
                    val stepStartTime = kotlin.time.TimeSource.Monotonic.markNow()
                    val toolResult = toolExecutor.execute(toolCall)
                    val stepDuration = stepStartTime.elapsedNow()
                    
                    val executionStep = ExecutionStep(
                        taskId = task.id,
                        toolCall = toolCall,
                        result = toolResult,
                        duration = stepDuration
                    )
                    
                    executedSteps.add(executionStep)
                    currentSession = currentSession.addExecutionStep(executionStep)
                    
                    // Check if tool execution failed
                    if (!toolResult.success) {
                        val failedSession = conversationStateManager.updateState(
                            currentSession.id,
                            currentSession.state.copy(
                                status = ConversationStatus.FAILED,
                                executionRound = executionRounds
                            ).withError(ExecutionError(
                                message = "Tool execution failed: ${toolResult.error}",
                                code = "TOOL_EXECUTION_FAILED"
                            ))
                        )
                        
                        return ExecutionResult.failure(
                            sessionId = failedSession.id,
                            finalStatus = ConversationStatus.FAILED,
                            executedSteps = executedSteps,
                            executionRounds = executionRounds,
                            executionTime = startTime.elapsedNow(),
                            error = toolResult.error ?: "Tool execution failed"
                        )
                    }
                    
                    // Check max rounds again after each tool call
                    if (executionRounds >= maxExecutionRounds) {
                        val pausedSession = conversationStateManager.updateState(
                            currentSession.id,
                            currentSession.state.copy(
                                status = ConversationStatus.WAITING_USER,
                                executionRound = executionRounds
                            )
                        )
                        
                        return ExecutionResult.success(
                            sessionId = pausedSession.id,
                            finalStatus = ConversationStatus.WAITING_USER,
                            executedSteps = executedSteps,
                            executionRounds = executionRounds,
                            executionTime = startTime.elapsedNow(),
                            summary = "Execution paused after $executionRounds rounds. Use continueConversation to resume."
                        )
                    }
                }
            }
            
            // All tasks completed successfully
            val completedSession = conversationStateManager.updateState(
                currentSession.id,
                currentSession.state.copy(
                    status = ConversationStatus.COMPLETED,
                    executionRound = executionRounds
                )
            )
            
            return ExecutionResult.success(
                sessionId = completedSession.id,
                finalStatus = ConversationStatus.COMPLETED,
                executedSteps = executedSteps,
                executionRounds = executionRounds,
                executionTime = startTime.elapsedNow(),
                summary = "Successfully completed ${currentSession.tasks.size} tasks in $executionRounds rounds"
            )
            
        } catch (e: Exception) {
            val failedSession = conversationStateManager.updateState(
                currentSession.id,
                currentSession.state.copy(
                    status = ConversationStatus.FAILED,
                    executionRound = executionRounds
                ).withError(ExecutionError(
                    message = "Execution failed: ${e.message}",
                    code = "EXECUTION_ERROR"
                ))
            )
            
            return ExecutionResult.failure(
                sessionId = failedSession.id,
                finalStatus = ConversationStatus.FAILED,
                executedSteps = executedSteps,
                executionRounds = executionRounds,
                executionTime = startTime.elapsedNow(),
                error = "Execution failed: ${e.message}"
            )
        }
    }
}
