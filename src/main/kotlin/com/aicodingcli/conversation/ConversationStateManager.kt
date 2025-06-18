package com.aicodingcli.conversation

import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Manager for conversation state storage and retrieval
 */
class ConversationStateManager(
    private val stateDir: String = System.getProperty("user.home") + "/.aicodingcli/conversations"
) {
    
    private val stateFile = File(stateDir, "sessions.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private val sessions = ConcurrentHashMap<String, ConversationSession>()
    
    init {
        ensureStateDirectoryExists()
        loadSessions()
    }
    
    /**
     * Create a new conversation session
     */
    suspend fun createSession(requirement: String): ConversationSession {
        val session = ConversationSession(
            requirement = requirement,
            state = ConversationState(
                status = ConversationStatus.CREATED,
                currentTaskIndex = 0,
                executionRound = 0,
                context = emptyMap(),
                errors = emptyList()
            )
        )
        
        sessions[session.id] = session
        saveSessions()
        return session
    }
    
    /**
     * Update conversation state
     */
    suspend fun updateState(sessionId: String, state: ConversationState): ConversationSession {
        val session = sessions[sessionId]
            ?: throw IllegalArgumentException("Conversation session not found: $sessionId")
        
        val updatedSession = session.withUpdatedState(state)
        sessions[sessionId] = updatedSession
        saveSessions()
        return updatedSession
    }
    
    /**
     * Get conversation session by ID
     */
    suspend fun getSession(sessionId: String): ConversationSession? {
        return sessions[sessionId]
    }
    
    /**
     * End conversation session
     */
    suspend fun endSession(sessionId: String) {
        val session = sessions[sessionId]
            ?: throw IllegalArgumentException("Conversation session not found: $sessionId")
        
        val endedState = session.state.copy(status = ConversationStatus.COMPLETED)
        val endedSession = session.withUpdatedState(endedState)
        sessions[sessionId] = endedSession
        saveSessions()
    }
    
    /**
     * Get all active sessions
     */
    suspend fun getActiveSessions(): List<ConversationSession> {
        return sessions.values.filter { 
            it.state.status !in listOf(ConversationStatus.COMPLETED, ConversationStatus.FAILED, ConversationStatus.CANCELLED)
        }.sortedByDescending { it.updatedAt }
    }
    
    /**
     * Delete a conversation session
     */
    suspend fun deleteSession(sessionId: String): Boolean {
        val removed = sessions.remove(sessionId) != null
        if (removed) {
            saveSessions()
        }
        return removed
    }
    
    /**
     * Clear all sessions
     */
    suspend fun clearAllSessions() {
        sessions.clear()
        saveSessions()
    }
    
    /**
     * Ensure state directory exists
     */
    private fun ensureStateDirectoryExists() {
        val dir = File(stateDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }
    
    /**
     * Load sessions from file
     */
    private fun loadSessions() {
        try {
            if (stateFile.exists()) {
                val content = stateFile.readText()
                if (content.isNotBlank()) {
                    val sessionList = json.decodeFromString<List<ConversationSession>>(content)
                    sessions.clear()
                    sessionList.forEach { session ->
                        sessions[session.id] = session
                    }
                }
            }
        } catch (e: Exception) {
            // Log error but don't fail - start with empty sessions
            println("Warning: Failed to load conversation sessions: ${e.message}")
        }
    }
    
    /**
     * Save sessions to file
     */
    private fun saveSessions() {
        try {
            ensureStateDirectoryExists()
            val sessionList = sessions.values.toList()
            val content = json.encodeToString(sessionList)
            stateFile.writeText(content)
        } catch (e: Exception) {
            throw IOException("Failed to save conversation sessions: ${e.message}", e)
        }
    }
}
