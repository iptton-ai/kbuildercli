package com.aicodingcli.history

import com.aicodingcli.ai.AiProvider
import com.aicodingcli.ai.MessageRole
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import java.io.IOException
import java.time.Instant

/**
 * Manager for conversation history storage and retrieval
 */
class HistoryManager(
    private val historyDir: String = System.getProperty("user.home") + "/.aicodingcli/history"
) {
    
    private val historyFile = File(historyDir, "conversations.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private var conversations: MutableList<ConversationSession> = mutableListOf()
    
    init {
        ensureHistoryDirectoryExists()
        loadConversations()
    }
    
    /**
     * Create a new conversation session
     */
    fun createConversation(
        title: String,
        provider: AiProvider,
        model: String
    ): ConversationSession {
        val conversation = ConversationSession(
            title = title,
            provider = provider,
            model = model
        )
        conversations.add(conversation)
        saveConversations()
        return conversation
    }
    
    /**
     * Add a message to a conversation
     */
    fun addMessage(
        conversationId: String,
        role: MessageRole,
        content: String,
        tokenUsage: MessageTokenUsage? = null
    ): ConversationMessage {
        val conversation = getConversation(conversationId)
            ?: throw IllegalArgumentException("Conversation not found: $conversationId")
        
        val message = ConversationMessage(
            role = role,
            content = content,
            tokenUsage = tokenUsage
        )
        
        conversation.addMessage(message)
        conversation.copy(updatedAt = Instant.now().epochSecond)
        saveConversations()
        
        return message
    }
    
    /**
     * Get a conversation by ID (supports partial ID matching)
     */
    fun getConversation(conversationId: String): ConversationSession? {
        // First try exact match
        conversations.find { it.id == conversationId }?.let { return it }

        // Then try partial match (if ID is at least 4 characters)
        if (conversationId.length >= 4) {
            val matches = conversations.filter { it.id.startsWith(conversationId) }
            when (matches.size) {
                1 -> return matches.first()
                0 -> return null
                else -> {
                    // Multiple matches - this is ambiguous
                    throw IllegalArgumentException("Ambiguous conversation ID '$conversationId'. Matches: ${matches.map { it.id.take(8) }}")
                }
            }
        }

        return null
    }
    
    /**
     * Get all conversations
     */
    fun getAllConversations(): List<ConversationSession> {
        return conversations.sortedByDescending { it.updatedAt }
    }
    
    /**
     * Search conversations
     */
    fun searchConversations(criteria: HistorySearchCriteria): List<ConversationSession> {
        var result = conversations.asSequence()
        
        // Filter by provider
        criteria.provider?.let { provider ->
            result = result.filter { it.provider == provider }
        }
        
        // Filter by model
        criteria.model?.let { model ->
            result = result.filter { it.model == model }
        }
        
        // Filter by date range
        criteria.fromDate?.let { fromDate ->
            result = result.filter { it.createdAt >= fromDate }
        }
        
        criteria.toDate?.let { toDate ->
            result = result.filter { it.createdAt <= toDate }
        }
        
        // Filter by query (search in title and message content)
        criteria.query?.let { query ->
            val queryLower = query.lowercase()
            result = result.filter { conversation ->
                conversation.title.lowercase().contains(queryLower) ||
                conversation.messages.any { it.content.lowercase().contains(queryLower) }
            }
        }
        
        return result
            .sortedByDescending { it.updatedAt }
            .take(criteria.limit)
            .toList()
    }
    
    /**
     * Delete a conversation
     */
    fun deleteConversation(conversationId: String): Boolean {
        val removed = conversations.removeIf { it.id == conversationId }
        if (removed) {
            saveConversations()
        }
        return removed
    }
    
    /**
     * Clear all conversations
     */
    fun clearAllConversations() {
        conversations.clear()
        saveConversations()
    }
    
    /**
     * Get history statistics
     */
    fun getStatistics(): HistoryStatistics {
        val totalMessages = conversations.sumOf { it.messages.size }
        val totalTokens = conversations.sumOf { conversation ->
            conversation.messages.sumOf { it.tokenUsage?.totalTokens?.toLong() ?: 0L }
        }
        
        val providerBreakdown = conversations.groupBy { it.provider }
            .mapValues { it.value.size }
        
        val oldestConversation = conversations.minOfOrNull { it.createdAt }
        val newestConversation = conversations.maxOfOrNull { it.createdAt }
        
        return HistoryStatistics(
            totalConversations = conversations.size,
            totalMessages = totalMessages,
            totalTokensUsed = totalTokens,
            providerBreakdown = providerBreakdown,
            oldestConversation = oldestConversation,
            newestConversation = newestConversation
        )
    }
    
    /**
     * Load conversations from file
     */
    private fun loadConversations() {
        try {
            if (historyFile.exists()) {
                val content = historyFile.readText()
                if (content.isNotBlank()) {
                    conversations = json.decodeFromString<MutableList<ConversationSession>>(content)
                }
            }
        } catch (e: Exception) {
            // If loading fails, start with empty list
            conversations = mutableListOf()
        }
    }
    
    /**
     * Save conversations to file
     */
    private fun saveConversations() {
        try {
            val content = json.encodeToString(conversations)
            historyFile.writeText(content)
        } catch (e: IOException) {
            throw RuntimeException("Failed to save conversation history: ${e.message}", e)
        }
    }
    
    /**
     * Ensure history directory exists
     */
    private fun ensureHistoryDirectoryExists() {
        val dir = File(historyDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }
}
