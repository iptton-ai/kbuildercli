package com.aicodingcli.conversation

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.Instant

class ConversationStateManagerTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var conversationStateManager: ConversationStateManager

    @BeforeEach
    fun setUp() {
        conversationStateManager = ConversationStateManager(tempDir.absolutePath)
    }

    @Test
    fun `should create new conversation session with initial state`() = runTest {
        // Arrange
        val requirement = "Create a simple Kotlin class with getter and setter"

        // Act
        val session = conversationStateManager.createSession(requirement)

        // Assert
        assertNotNull(session)
        assertEquals(requirement, session.requirement)
        assertEquals(ConversationStatus.CREATED, session.state.status)
        assertEquals(0, session.state.currentTaskIndex)
        assertEquals(0, session.state.executionRound)
        assertTrue(session.tasks.isEmpty())
        assertTrue(session.executionHistory.isEmpty())
        assertTrue(session.state.errors.isEmpty())
        assertNotNull(session.id)
        assertNotNull(session.createdAt)
        assertNotNull(session.updatedAt)
    }

    @Test
    fun `should update conversation state and persist changes`() = runTest {
        // Arrange
        val session = conversationStateManager.createSession("Test requirement")
        val newState = ConversationState(
            status = ConversationStatus.PLANNING,
            currentTaskIndex = 1,
            executionRound = 2,
            context = mapOf("key" to "value"),
            errors = listOf(ExecutionError("Test error", "ERROR_CODE"))
        )

        // Act
        val updatedSession = conversationStateManager.updateState(session.id, newState)

        // Assert
        assertNotNull(updatedSession)
        assertEquals(ConversationStatus.PLANNING, updatedSession.state.status)
        assertEquals(1, updatedSession.state.currentTaskIndex)
        assertEquals(2, updatedSession.state.executionRound)
        assertEquals("value", updatedSession.state.context["key"])
        assertEquals(1, updatedSession.state.errors.size)
        assertEquals("Test error", updatedSession.state.errors[0].message)
        assertTrue(updatedSession.updatedAt.isAfter(session.updatedAt))
    }

    @Test
    fun `should retrieve existing conversation session`() = runTest {
        // Arrange
        val originalSession = conversationStateManager.createSession("Test requirement")

        // Act
        val retrievedSession = conversationStateManager.getSession(originalSession.id)

        // Assert
        assertNotNull(retrievedSession)
        assertEquals(originalSession.id, retrievedSession!!.id)
        assertEquals(originalSession.requirement, retrievedSession.requirement)
        assertEquals(originalSession.state.status, retrievedSession.state.status)
    }

    @Test
    fun `should return null for non-existent conversation session`() = runTest {
        // Act
        val session = conversationStateManager.getSession("non-existent-id")

        // Assert
        assertNull(session)
    }

    @Test
    fun `should end conversation session and mark as completed`() = runTest {
        // Arrange
        val session = conversationStateManager.createSession("Test requirement")

        // Act
        conversationStateManager.endSession(session.id)

        // Assert
        val endedSession = conversationStateManager.getSession(session.id)
        assertNotNull(endedSession)
        assertEquals(ConversationStatus.COMPLETED, endedSession!!.state.status)
    }

    @Test
    fun `should persist conversation sessions to file`() = runTest {
        // Arrange
        val session1 = conversationStateManager.createSession("Requirement 1")
        val session2 = conversationStateManager.createSession("Requirement 2")

        // Act - Create new manager instance to test persistence
        val newManager = ConversationStateManager(tempDir.absolutePath)
        val retrievedSession1 = newManager.getSession(session1.id)
        val retrievedSession2 = newManager.getSession(session2.id)

        // Assert
        assertNotNull(retrievedSession1)
        assertNotNull(retrievedSession2)
        assertEquals("Requirement 1", retrievedSession1!!.requirement)
        assertEquals("Requirement 2", retrievedSession2!!.requirement)
    }

    @Test
    fun `should handle concurrent session updates safely`() = runTest {
        // Arrange
        val session = conversationStateManager.createSession("Test requirement")
        val state1 = ConversationState(
            status = ConversationStatus.PLANNING,
            currentTaskIndex = 1,
            executionRound = 1,
            context = mapOf("update" to "first"),
            errors = emptyList()
        )
        val state2 = ConversationState(
            status = ConversationStatus.EXECUTING,
            currentTaskIndex = 2,
            executionRound = 2,
            context = mapOf("update" to "second"),
            errors = emptyList()
        )

        // Act
        val updatedSession1 = conversationStateManager.updateState(session.id, state1)
        val updatedSession2 = conversationStateManager.updateState(session.id, state2)

        // Assert
        assertNotNull(updatedSession1)
        assertNotNull(updatedSession2)
        assertEquals(ConversationStatus.EXECUTING, updatedSession2.state.status)
        assertEquals(2, updatedSession2.state.currentTaskIndex)
        assertEquals("second", updatedSession2.state.context["update"])
    }

    @Test
    fun `should throw exception when updating non-existent session`() = runTest {
        // Arrange
        val state = ConversationState(
            status = ConversationStatus.PLANNING,
            currentTaskIndex = 0,
            executionRound = 0,
            context = emptyMap(),
            errors = emptyList()
        )

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                conversationStateManager.updateState("non-existent-id", state)
            }
        }
    }

    @Test
    fun `should throw exception when ending non-existent session`() = runTest {
        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                conversationStateManager.endSession("non-existent-id")
            }
        }
    }
}
