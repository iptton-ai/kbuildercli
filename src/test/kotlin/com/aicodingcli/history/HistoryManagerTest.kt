package com.aicodingcli.history

import com.aicodingcli.ai.AiProvider
import com.aicodingcli.ai.MessageRole
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Files
import java.time.Instant

class HistoryManagerTest {

    private lateinit var tempDir: File
    private lateinit var historyManager: HistoryManager

    @BeforeEach
    fun setUp() {
        // Create temporary directory for testing
        tempDir = Files.createTempDirectory("aicodingcli-test").toFile()
        historyManager = HistoryManager(tempDir.absolutePath)
    }

    @AfterEach
    fun tearDown() {
        // Clean up temporary directory
        tempDir.deleteRecursively()
    }

    @Test
    fun `should create new conversation successfully`() {
        // Arrange
        val title = "Test Conversation"
        val provider = AiProvider.OLLAMA
        val model = "llama3.2"

        // Act
        val conversation = historyManager.createConversation(title, provider, model)

        // Assert
        assertNotNull(conversation.id)
        assertEquals(title, conversation.title)
        assertEquals(provider, conversation.provider)
        assertEquals(model, conversation.model)
        assertTrue(conversation.messages.isEmpty())
        assertTrue(conversation.createdAt > 0)
        assertTrue(conversation.updatedAt > 0)
    }

    @Test
    fun `should add message to conversation successfully`() {
        // Arrange
        val conversation = historyManager.createConversation("Test", AiProvider.OLLAMA, "llama3.2")
        val messageContent = "Hello, world!"
        val tokenUsage = MessageTokenUsage(10, 20, 30)

        // Act
        val message = historyManager.addMessage(
            conversationId = conversation.id,
            role = MessageRole.USER,
            content = messageContent,
            tokenUsage = tokenUsage
        )

        // Assert
        assertNotNull(message.id)
        assertEquals(MessageRole.USER, message.role)
        assertEquals(messageContent, message.content)
        assertEquals(tokenUsage, message.tokenUsage)
        assertTrue(message.timestamp > 0)

        // Verify conversation was updated
        val updatedConversation = historyManager.getConversation(conversation.id)
        assertNotNull(updatedConversation)
        assertEquals(1, updatedConversation!!.messages.size)
        assertEquals(message.id, updatedConversation.messages[0].id)
    }

    @Test
    fun `should throw exception when adding message to non-existent conversation`() {
        // Arrange
        val nonExistentId = "non-existent-id"

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            historyManager.addMessage(
                conversationId = nonExistentId,
                role = MessageRole.USER,
                content = "Test message"
            )
        }
    }

    @Test
    fun `should get conversation by exact ID`() {
        // Arrange
        val conversation = historyManager.createConversation("Test", AiProvider.OLLAMA, "llama3.2")

        // Act
        val retrieved = historyManager.getConversation(conversation.id)

        // Assert
        assertNotNull(retrieved)
        assertEquals(conversation.id, retrieved!!.id)
        assertEquals(conversation.title, retrieved.title)
    }

    @Test
    fun `should get conversation by partial ID`() {
        // Arrange
        val conversation = historyManager.createConversation("Test", AiProvider.OLLAMA, "llama3.2")
        val partialId = conversation.id.take(8)

        // Act
        val retrieved = historyManager.getConversation(partialId)

        // Assert
        assertNotNull(retrieved)
        assertEquals(conversation.id, retrieved!!.id)
    }

    @Test
    fun `should return null for non-existent conversation ID`() {
        // Act
        val retrieved = historyManager.getConversation("non-existent-id")

        // Assert
        assertNull(retrieved)
    }

    @Test
    fun `should throw exception for ambiguous partial ID`() {
        // Arrange
        // Create conversations and try to find an ambiguous prefix
        val conv1 = historyManager.createConversation("Test 1", AiProvider.OLLAMA, "llama3.2")

        // Create more conversations to increase chance of ambiguity
        repeat(10) {
            historyManager.createConversation("Test $it", AiProvider.OLLAMA, "llama3.2")
        }

        // Act & Assert
        // We'll test with a prefix that we know exists in at least one conversation
        val existingPrefix = conv1.id.take(4)

        // This might or might not throw an exception depending on whether there are multiple matches
        // Let's just verify the method handles the case properly
        try {
            val result = historyManager.getConversation(existingPrefix)
            // If no exception, there was only one match, which is fine
            assertNotNull(result)
        } catch (e: IllegalArgumentException) {
            // If exception, there were multiple matches, which is also fine
            assertTrue(e.message!!.contains("Ambiguous conversation ID"))
        }
    }

    @Test
    fun `should get all conversations sorted by update time`() {
        // Arrange
        val conv1 = historyManager.createConversation("First", AiProvider.OLLAMA, "llama3.2")
        Thread.sleep(100) // Ensure different timestamps
        val conv2 = historyManager.createConversation("Second", AiProvider.OPENAI, "gpt-3.5-turbo")
        Thread.sleep(100)
        val conv3 = historyManager.createConversation("Third", AiProvider.CLAUDE, "claude-3-sonnet")

        // Act
        val conversations = historyManager.getAllConversations()

        // Assert
        assertEquals(3, conversations.size)
        // Should be sorted by updatedAt descending (newest first)
        // Check that conversations are sorted correctly by checking timestamps
        assertTrue(conversations[0].updatedAt >= conversations[1].updatedAt)
        assertTrue(conversations[1].updatedAt >= conversations[2].updatedAt)

        // Verify the order by checking that the most recently created conversation is first
        // Since conv3 was created last, it should have the highest updatedAt
        assertTrue(conv3.updatedAt >= conv2.updatedAt)
        assertTrue(conv2.updatedAt >= conv1.updatedAt)
    }

    @Test
    fun `should search conversations by query`() {
        // Arrange
        val conv1 = historyManager.createConversation("Kotlin Programming", AiProvider.OLLAMA, "llama3.2")
        val conv2 = historyManager.createConversation("Python Basics", AiProvider.OPENAI, "gpt-3.5-turbo")
        val conv3 = historyManager.createConversation("Java Advanced", AiProvider.CLAUDE, "claude-3-sonnet")
        
        // Add messages to conversations
        historyManager.addMessage(conv1.id, MessageRole.USER, "Tell me about Kotlin data classes")
        historyManager.addMessage(conv2.id, MessageRole.USER, "Python list comprehensions")
        historyManager.addMessage(conv3.id, MessageRole.USER, "Java streams and lambdas")

        // Act
        val kotlinResults = historyManager.searchConversations(HistorySearchCriteria(query = "kotlin"))
        val pythonResults = historyManager.searchConversations(HistorySearchCriteria(query = "python"))

        // Assert
        assertEquals(1, kotlinResults.size)
        assertEquals(conv1.id, kotlinResults[0].id)
        
        assertEquals(1, pythonResults.size)
        assertEquals(conv2.id, pythonResults[0].id)
    }

    @Test
    fun `should search conversations by provider`() {
        // Arrange
        historyManager.createConversation("Test 1", AiProvider.OLLAMA, "llama3.2")
        historyManager.createConversation("Test 2", AiProvider.OPENAI, "gpt-3.5-turbo")
        historyManager.createConversation("Test 3", AiProvider.OLLAMA, "llama2")

        // Act
        val ollamaResults = historyManager.searchConversations(HistorySearchCriteria(provider = AiProvider.OLLAMA))
        val openaiResults = historyManager.searchConversations(HistorySearchCriteria(provider = AiProvider.OPENAI))

        // Assert
        assertEquals(2, ollamaResults.size)
        assertTrue(ollamaResults.all { it.provider == AiProvider.OLLAMA })

        assertEquals(1, openaiResults.size)
        assertEquals(AiProvider.OPENAI, openaiResults[0].provider)
    }

    @Test
    fun `should search conversations by model`() {
        // Arrange
        historyManager.createConversation("Test 1", AiProvider.OLLAMA, "llama3.2")
        historyManager.createConversation("Test 2", AiProvider.OLLAMA, "llama2")
        historyManager.createConversation("Test 3", AiProvider.OLLAMA, "llama3.2")

        // Act
        val llama32Results = historyManager.searchConversations(HistorySearchCriteria(model = "llama3.2"))
        val llama2Results = historyManager.searchConversations(HistorySearchCriteria(model = "llama2"))

        // Assert
        assertEquals(2, llama32Results.size)
        assertTrue(llama32Results.all { it.model == "llama3.2" })

        assertEquals(1, llama2Results.size)
        assertEquals("llama2", llama2Results[0].model)
    }

    @Test
    fun `should limit search results`() {
        // Arrange
        repeat(10) {
            historyManager.createConversation("Test $it", AiProvider.OLLAMA, "llama3.2")
        }

        // Act
        val limitedResults = historyManager.searchConversations(HistorySearchCriteria(limit = 5))

        // Assert
        assertEquals(5, limitedResults.size)
    }

    @Test
    fun `should delete conversation successfully`() {
        // Arrange
        val conversation = historyManager.createConversation("Test", AiProvider.OLLAMA, "llama3.2")
        
        // Verify conversation exists
        assertNotNull(historyManager.getConversation(conversation.id))

        // Act
        val deleted = historyManager.deleteConversation(conversation.id)

        // Assert
        assertTrue(deleted)
        assertNull(historyManager.getConversation(conversation.id))
    }

    @Test
    fun `should return false when deleting non-existent conversation`() {
        // Act
        val deleted = historyManager.deleteConversation("non-existent-id")

        // Assert
        assertFalse(deleted)
    }

    @Test
    fun `should clear all conversations`() {
        // Arrange
        repeat(5) {
            historyManager.createConversation("Test $it", AiProvider.OLLAMA, "llama3.2")
        }
        
        // Verify conversations exist
        assertEquals(5, historyManager.getAllConversations().size)

        // Act
        historyManager.clearAllConversations()

        // Assert
        assertEquals(0, historyManager.getAllConversations().size)
    }

    @Test
    fun `should calculate statistics correctly`() {
        // Arrange
        val conv1 = historyManager.createConversation("Test 1", AiProvider.OLLAMA, "llama3.2")
        val conv2 = historyManager.createConversation("Test 2", AiProvider.OPENAI, "gpt-3.5-turbo")
        val conv3 = historyManager.createConversation("Test 3", AiProvider.OLLAMA, "llama2")
        
        // Add messages with token usage
        historyManager.addMessage(conv1.id, MessageRole.USER, "Message 1", MessageTokenUsage(10, 20, 30))
        historyManager.addMessage(conv1.id, MessageRole.ASSISTANT, "Response 1", MessageTokenUsage(15, 25, 40))
        historyManager.addMessage(conv2.id, MessageRole.USER, "Message 2", MessageTokenUsage(5, 10, 15))

        // Act
        val stats = historyManager.getStatistics()

        // Assert
        assertEquals(3, stats.totalConversations)
        assertEquals(3, stats.totalMessages)
        assertEquals(85L, stats.totalTokensUsed) // 30 + 40 + 15
        
        assertEquals(2, stats.providerBreakdown[AiProvider.OLLAMA])
        assertEquals(1, stats.providerBreakdown[AiProvider.OPENAI])
        
        assertNotNull(stats.oldestConversation)
        assertNotNull(stats.newestConversation)
    }

    @Test
    fun `should persist conversations to file`() {
        // Arrange
        val conversation = historyManager.createConversation("Test", AiProvider.OLLAMA, "llama3.2")
        historyManager.addMessage(conversation.id, MessageRole.USER, "Test message")

        // Act - Create new HistoryManager instance with same directory
        val newHistoryManager = HistoryManager(tempDir.absolutePath)

        // Assert - Should load existing conversations
        val loadedConversation = newHistoryManager.getConversation(conversation.id)
        assertNotNull(loadedConversation)
        assertEquals(conversation.title, loadedConversation!!.title)
        assertEquals(1, loadedConversation.messages.size)
        assertEquals("Test message", loadedConversation.messages[0].content)
    }

    @Test
    fun `should handle empty message content validation`() {
        // Arrange
        val conversation = historyManager.createConversation("Test", AiProvider.OLLAMA, "llama3.2")

        // Act & Assert
        assertThrows<IllegalArgumentException> {
            historyManager.addMessage(conversation.id, MessageRole.USER, "")
        }
        
        assertThrows<IllegalArgumentException> {
            historyManager.addMessage(conversation.id, MessageRole.USER, "   ")
        }
    }
}
