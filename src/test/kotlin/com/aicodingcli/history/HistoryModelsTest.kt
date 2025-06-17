package com.aicodingcli.history

import com.aicodingcli.ai.AiProvider
import com.aicodingcli.ai.MessageRole
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.time.Instant

class HistoryModelsTest {

    @Test
    fun `should create ConversationSession with default values`() {
        // Arrange
        val title = "Test Conversation"
        val provider = AiProvider.OLLAMA
        val model = "llama3.2"

        // Act
        val conversation = ConversationSession(
            title = title,
            provider = provider,
            model = model
        )

        // Assert
        assertNotNull(conversation.id)
        assertEquals(title, conversation.title)
        assertEquals(provider, conversation.provider)
        assertEquals(model, conversation.model)
        assertTrue(conversation.createdAt > 0)
        assertTrue(conversation.updatedAt > 0)
        assertTrue(conversation.messages.isEmpty())
    }

    @Test
    fun `should add message to conversation`() {
        // Arrange
        val conversation = ConversationSession(
            title = "Test",
            provider = AiProvider.OLLAMA,
            model = "llama3.2"
        )
        val message = ConversationMessage(
            role = MessageRole.USER,
            content = "Hello, world!"
        )

        // Act
        conversation.addMessage(message)

        // Assert
        assertEquals(1, conversation.messages.size)
        assertEquals(message, conversation.messages[0])
    }

    @Test
    fun `should get last user message`() {
        // Arrange
        val conversation = ConversationSession(
            title = "Test",
            provider = AiProvider.OLLAMA,
            model = "llama3.2"
        )
        
        val userMessage1 = ConversationMessage(role = MessageRole.USER, content = "First user message")
        val assistantMessage = ConversationMessage(role = MessageRole.ASSISTANT, content = "Assistant response")
        val userMessage2 = ConversationMessage(role = MessageRole.USER, content = "Second user message")
        
        conversation.addMessage(userMessage1)
        conversation.addMessage(assistantMessage)
        conversation.addMessage(userMessage2)

        // Act
        val lastUserMessage = conversation.getLastUserMessage()

        // Assert
        assertNotNull(lastUserMessage)
        assertEquals(userMessage2.content, lastUserMessage!!.content)
    }

    @Test
    fun `should get last assistant message`() {
        // Arrange
        val conversation = ConversationSession(
            title = "Test",
            provider = AiProvider.OLLAMA,
            model = "llama3.2"
        )
        
        val userMessage = ConversationMessage(role = MessageRole.USER, content = "User message")
        val assistantMessage1 = ConversationMessage(role = MessageRole.ASSISTANT, content = "First assistant response")
        val assistantMessage2 = ConversationMessage(role = MessageRole.ASSISTANT, content = "Second assistant response")
        
        conversation.addMessage(userMessage)
        conversation.addMessage(assistantMessage1)
        conversation.addMessage(assistantMessage2)

        // Act
        val lastAssistantMessage = conversation.getLastAssistantMessage()

        // Assert
        assertNotNull(lastAssistantMessage)
        assertEquals(assistantMessage2.content, lastAssistantMessage!!.content)
    }

    @Test
    fun `should return null when no user message exists`() {
        // Arrange
        val conversation = ConversationSession(
            title = "Test",
            provider = AiProvider.OLLAMA,
            model = "llama3.2"
        )
        
        val assistantMessage = ConversationMessage(role = MessageRole.ASSISTANT, content = "Assistant response")
        conversation.addMessage(assistantMessage)

        // Act
        val lastUserMessage = conversation.getLastUserMessage()

        // Assert
        assertNull(lastUserMessage)
    }

    @Test
    fun `should return null when no assistant message exists`() {
        // Arrange
        val conversation = ConversationSession(
            title = "Test",
            provider = AiProvider.OLLAMA,
            model = "llama3.2"
        )
        
        val userMessage = ConversationMessage(role = MessageRole.USER, content = "User message")
        conversation.addMessage(userMessage)

        // Act
        val lastAssistantMessage = conversation.getLastAssistantMessage()

        // Assert
        assertNull(lastAssistantMessage)
    }

    @Test
    fun `should generate conversation summary`() {
        // Arrange
        val conversation = ConversationSession(
            title = "Kotlin Learning",
            provider = AiProvider.OLLAMA,
            model = "llama3.2"
        )
        
        val message1 = ConversationMessage(role = MessageRole.USER, content = "What are Kotlin data classes?")
        val message2 = ConversationMessage(role = MessageRole.ASSISTANT, content = "Kotlin data classes are a concise way to create classes that are primarily used to hold data...")
        
        conversation.addMessage(message1)
        conversation.addMessage(message2)

        // Act
        val summary = conversation.getSummary()

        // Assert
        assertTrue(summary.contains("Kotlin Learning"))
        assertTrue(summary.contains("2 messages"))
        assertTrue(summary.contains("Kotlin data classes are a concise way to create"))
    }

    @Test
    fun `should generate summary for empty conversation`() {
        // Arrange
        val conversation = ConversationSession(
            title = "Empty Conversation",
            provider = AiProvider.OLLAMA,
            model = "llama3.2"
        )

        // Act
        val summary = conversation.getSummary()

        // Assert
        assertTrue(summary.contains("Empty Conversation"))
        assertTrue(summary.contains("0 messages"))
        assertTrue(summary.contains("Empty conversation"))
    }

    @Test
    fun `should create ConversationMessage with default values`() {
        // Arrange
        val role = MessageRole.USER
        val content = "Test message"

        // Act
        val message = ConversationMessage(
            role = role,
            content = content
        )

        // Assert
        assertNotNull(message.id)
        assertEquals(role, message.role)
        assertEquals(content, message.content)
        assertTrue(message.timestamp > 0)
        assertNull(message.tokenUsage)
    }

    @Test
    fun `should create ConversationMessage with token usage`() {
        // Arrange
        val role = MessageRole.ASSISTANT
        val content = "Test response"
        val tokenUsage = MessageTokenUsage(10, 20, 30)

        // Act
        val message = ConversationMessage(
            role = role,
            content = content,
            tokenUsage = tokenUsage
        )

        // Assert
        assertEquals(tokenUsage, message.tokenUsage)
        assertEquals(10, message.tokenUsage!!.promptTokens)
        assertEquals(20, message.tokenUsage!!.completionTokens)
        assertEquals(30, message.tokenUsage!!.totalTokens)
    }

    @Test
    fun `should validate message content is not blank`() {
        // Act & Assert
        assertThrows<IllegalArgumentException> {
            ConversationMessage(
                role = MessageRole.USER,
                content = ""
            )
        }
        
        assertThrows<IllegalArgumentException> {
            ConversationMessage(
                role = MessageRole.USER,
                content = "   "
            )
        }
        
        assertThrows<IllegalArgumentException> {
            ConversationMessage(
                role = MessageRole.USER,
                content = "\n\t  \n"
            )
        }
    }

    @Test
    fun `should create MessageTokenUsage correctly`() {
        // Arrange
        val promptTokens = 15
        val completionTokens = 25
        val totalTokens = 40

        // Act
        val tokenUsage = MessageTokenUsage(
            promptTokens = promptTokens,
            completionTokens = completionTokens,
            totalTokens = totalTokens
        )

        // Assert
        assertEquals(promptTokens, tokenUsage.promptTokens)
        assertEquals(completionTokens, tokenUsage.completionTokens)
        assertEquals(totalTokens, tokenUsage.totalTokens)
    }

    @Test
    fun `should create HistorySearchCriteria with default values`() {
        // Act
        val criteria = HistorySearchCriteria()

        // Assert
        assertNull(criteria.query)
        assertNull(criteria.provider)
        assertNull(criteria.model)
        assertNull(criteria.fromDate)
        assertNull(criteria.toDate)
        assertEquals(50, criteria.limit)
    }

    @Test
    fun `should create HistorySearchCriteria with custom values`() {
        // Arrange
        val query = "kotlin"
        val provider = AiProvider.OLLAMA
        val model = "llama3.2"
        val fromDate = Instant.now().epochSecond - 86400 // 1 day ago
        val toDate = Instant.now().epochSecond
        val limit = 10

        // Act
        val criteria = HistorySearchCriteria(
            query = query,
            provider = provider,
            model = model,
            fromDate = fromDate,
            toDate = toDate,
            limit = limit
        )

        // Assert
        assertEquals(query, criteria.query)
        assertEquals(provider, criteria.provider)
        assertEquals(model, criteria.model)
        assertEquals(fromDate, criteria.fromDate)
        assertEquals(toDate, criteria.toDate)
        assertEquals(limit, criteria.limit)
    }

    @Test
    fun `should create HistoryStatistics correctly`() {
        // Arrange
        val totalConversations = 5
        val totalMessages = 20
        val totalTokensUsed = 1500L
        val providerBreakdown = mapOf(
            AiProvider.OLLAMA to 3,
            AiProvider.OPENAI to 2
        )
        val oldestConversation = Instant.now().epochSecond - 86400
        val newestConversation = Instant.now().epochSecond

        // Act
        val stats = HistoryStatistics(
            totalConversations = totalConversations,
            totalMessages = totalMessages,
            totalTokensUsed = totalTokensUsed,
            providerBreakdown = providerBreakdown,
            oldestConversation = oldestConversation,
            newestConversation = newestConversation
        )

        // Assert
        assertEquals(totalConversations, stats.totalConversations)
        assertEquals(totalMessages, stats.totalMessages)
        assertEquals(totalTokensUsed, stats.totalTokensUsed)
        assertEquals(providerBreakdown, stats.providerBreakdown)
        assertEquals(oldestConversation, stats.oldestConversation)
        assertEquals(newestConversation, stats.newestConversation)
    }

    @Test
    fun `should handle null values in HistoryStatistics`() {
        // Act
        val stats = HistoryStatistics(
            totalConversations = 0,
            totalMessages = 0,
            totalTokensUsed = 0L,
            providerBreakdown = emptyMap(),
            oldestConversation = null,
            newestConversation = null
        )

        // Assert
        assertEquals(0, stats.totalConversations)
        assertEquals(0, stats.totalMessages)
        assertEquals(0L, stats.totalTokensUsed)
        assertTrue(stats.providerBreakdown.isEmpty())
        assertNull(stats.oldestConversation)
        assertNull(stats.newestConversation)
    }
}
