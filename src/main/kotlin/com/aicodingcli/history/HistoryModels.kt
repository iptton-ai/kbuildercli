package com.aicodingcli.history

import com.aicodingcli.ai.AiProvider
import com.aicodingcli.ai.MessageRole
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * Conversation session containing multiple messages
 */
@Serializable
data class ConversationSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val provider: AiProvider,
    val model: String,
    val createdAt: Long = Instant.now().epochSecond,
    val updatedAt: Long = Instant.now().epochSecond,
    val messages: MutableList<ConversationMessage> = mutableListOf()
) {
    /**
     * Add a message to the conversation
     */
    fun addMessage(message: ConversationMessage) {
        messages.add(message)
    }

    /**
     * Get the last user message
     */
    fun getLastUserMessage(): ConversationMessage? {
        return messages.lastOrNull { it.role == MessageRole.USER }
    }

    /**
     * Get the last assistant message
     */
    fun getLastAssistantMessage(): ConversationMessage? {
        return messages.lastOrNull { it.role == MessageRole.ASSISTANT }
    }

    /**
     * Get conversation summary for display
     */
    fun getSummary(): String {
        val messageCount = messages.size
        val lastMessage = messages.lastOrNull()
        val preview = lastMessage?.content?.take(50) ?: "Empty conversation"
        return "$title ($messageCount messages) - $preview..."
    }
}

/**
 * Individual message in a conversation
 */
@Serializable
data class ConversationMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = Instant.now().epochSecond,
    val tokenUsage: MessageTokenUsage? = null
) {
    init {
        require(content.isNotBlank()) { "Message content cannot be empty" }
    }
}

/**
 * Token usage information for a message
 */
@Serializable
data class MessageTokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/**
 * Search criteria for conversation history
 */
data class HistorySearchCriteria(
    val query: String? = null,
    val provider: AiProvider? = null,
    val model: String? = null,
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val limit: Int = 50
)

/**
 * History statistics
 */
@Serializable
data class HistoryStatistics(
    val totalConversations: Int,
    val totalMessages: Int,
    val totalTokensUsed: Long,
    val providerBreakdown: Map<AiProvider, Int>,
    val oldestConversation: Long?,
    val newestConversation: Long?
)
