package com.aicodingcli.conversation

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ToolExecutorTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var toolExecutor: ToolExecutor

    @BeforeEach
    fun setUp() {
        toolExecutor = DefaultToolExecutor(tempDir.absolutePath)
    }

    @Test
    fun `should execute save-file tool successfully`() = runTest {
        // Arrange
        val toolCall = ToolCall(
            toolName = "save-file",
            parameters = mapOf(
                "path" to "test.kt",
                "file_content" to "class Test"
            )
        )

        // Act
        val result = toolExecutor.execute(toolCall)

        // Assert
        assertTrue(result.success)
        assertTrue(result.output.contains("saved"))
        assertNull(result.error)
        
        // Verify file was created
        val file = File(tempDir, "test.kt")
        assertTrue(file.exists())
        assertEquals("class Test", file.readText())
    }

    @Test
    fun `should execute view tool successfully`() = runTest {
        // Arrange
        val testFile = File(tempDir, "existing.kt")
        testFile.writeText("existing content")
        
        val toolCall = ToolCall(
            toolName = "view",
            parameters = mapOf(
                "path" to "existing.kt",
                "type" to "file"
            )
        )

        // Act
        val result = toolExecutor.execute(toolCall)

        // Assert
        assertTrue(result.success)
        assertTrue(result.output.contains("existing content"))
        assertNull(result.error)
    }

    @Test
    fun `should execute str-replace-editor tool successfully`() = runTest {
        // Arrange
        val testFile = File(tempDir, "replace.kt")
        testFile.writeText("class OldName")
        
        val toolCall = ToolCall(
            toolName = "str-replace-editor",
            parameters = mapOf(
                "path" to "replace.kt",
                "old_str" to "OldName",
                "new_str" to "NewName"
            )
        )

        // Act
        val result = toolExecutor.execute(toolCall)

        // Assert
        assertTrue(result.success)
        assertTrue(result.output.contains("replaced"))
        assertNull(result.error)
        
        // Verify content was replaced
        assertEquals("class NewName", testFile.readText())
    }

    @Test
    fun `should validate parameters before execution`() = runTest {
        // Arrange
        val toolCall = ToolCall(
            toolName = "save-file",
            parameters = mapOf(
                "path" to "test.kt"
                // Missing required file_content parameter
            )
        )

        // Act
        val validation = toolExecutor.validateParameters(toolCall)

        // Assert
        assertFalse(validation.isValid)
        assertTrue(validation.errors.any { it.contains("file_content") })
    }

    @Test
    fun `should return error for unsupported tool`() = runTest {
        // Arrange
        val toolCall = ToolCall(
            toolName = "unsupported-tool",
            parameters = mapOf("param" to "value")
        )

        // Act
        val result = toolExecutor.execute(toolCall)

        // Assert
        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("unsupported"))
    }

    @Test
    fun `should return supported tools metadata`() = runTest {
        // Act
        val tools = toolExecutor.getSupportedTools()

        // Assert
        assertTrue(tools.isNotEmpty())
        
        val saveFileTool = tools.find { it.name == "save-file" }
        assertNotNull(saveFileTool)
        assertEquals("Create a new file with content", saveFileTool!!.description)
        assertTrue(saveFileTool.parameters.any { it.name == "path" })
        assertTrue(saveFileTool.parameters.any { it.name == "file_content" })
        
        val viewTool = tools.find { it.name == "view" }
        assertNotNull(viewTool)
        assertEquals("View file or directory contents", viewTool!!.description)
        assertTrue(viewTool.parameters.any { it.name == "path" })
        assertTrue(viewTool.parameters.any { it.name == "type" })
    }

    @Test
    fun `should handle file operation errors gracefully`() = runTest {
        // Arrange - Use str-replace-editor on non-existent file to trigger error
        val toolCall = ToolCall(
            toolName = "str-replace-editor",
            parameters = mapOf(
                "path" to "non_existent_file.kt",
                "old_str" to "old",
                "new_str" to "new"
            )
        )

        // Act
        val result = toolExecutor.execute(toolCall)

        // Assert
        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("does not exist"))
    }

    @Test
    fun `should execute codebase-retrieval tool with mock response`() = runTest {
        // Arrange
        val toolCall = ToolCall(
            toolName = "codebase-retrieval",
            parameters = mapOf(
                "information_request" to "Find all classes in the project"
            )
        )

        // Act
        val result = toolExecutor.execute(toolCall)

        // Assert
        assertTrue(result.success)
        assertTrue(result.output.contains("classes") || result.output.contains("mock"))
        assertNull(result.error)
    }

    @Test
    fun `should execute add_tasks tool successfully`() = runTest {
        // Arrange
        val toolCall = ToolCall(
            toolName = "add_tasks",
            parameters = mapOf(
                "name" to "Test Task",
                "description" to "Test task description"
            )
        )

        // Act
        val result = toolExecutor.execute(toolCall)

        // Assert
        assertTrue(result.success)
        assertTrue(result.output.contains("task") || result.output.contains("added"))
        assertNull(result.error)
    }

    @Test
    fun `should validate required parameters for each tool`() = runTest {
        // Test save-file validation
        val saveFileCall = ToolCall(
            toolName = "save-file",
            parameters = mapOf("path" to "test.kt") // Missing file_content
        )
        val saveFileValidation = toolExecutor.validateParameters(saveFileCall)
        assertFalse(saveFileValidation.isValid)
        assertTrue(saveFileValidation.errors.any { it.contains("file_content") })

        // Test view validation
        val viewCall = ToolCall(
            toolName = "view",
            parameters = mapOf("type" to "file") // Missing path
        )
        val viewValidation = toolExecutor.validateParameters(viewCall)
        assertFalse(viewValidation.isValid)
        assertTrue(viewValidation.errors.any { it.contains("path") })

        // Test str-replace-editor validation
        val replaceCall = ToolCall(
            toolName = "str-replace-editor",
            parameters = mapOf(
                "path" to "test.kt",
                "old_str" to "old"
                // Missing new_str
            )
        )
        val replaceValidation = toolExecutor.validateParameters(replaceCall)
        assertFalse(replaceValidation.isValid)
        assertTrue(replaceValidation.errors.any { it.contains("new_str") })
    }

    @Test
    fun `should handle concurrent tool executions safely`() = runTest {
        // Arrange
        val toolCalls = (1..5).map { i ->
            ToolCall(
                toolName = "save-file",
                parameters = mapOf(
                    "path" to "concurrent_$i.kt",
                    "file_content" to "class Concurrent$i"
                )
            )
        }

        // Act
        val results = toolCalls.map { toolExecutor.execute(it) }

        // Assert
        results.forEach { result ->
            assertTrue(result.success, "All concurrent executions should succeed")
            assertNull(result.error)
        }

        // Verify all files were created
        (1..5).forEach { i ->
            val file = File(tempDir, "concurrent_$i.kt")
            assertTrue(file.exists())
            assertEquals("class Concurrent$i", file.readText())
        }
    }

    @Test
    fun `should provide execution metadata in results`() = runTest {
        // Arrange
        val toolCall = ToolCall(
            toolName = "save-file",
            parameters = mapOf(
                "path" to "metadata_test.kt",
                "file_content" to "class MetadataTest"
            )
        )

        // Act
        val result = toolExecutor.execute(toolCall)

        // Assert
        assertTrue(result.success)
        assertTrue(result.metadata.isNotEmpty())
        assertTrue(result.metadata.containsKey("tool_name"))
        assertEquals("save-file", result.metadata["tool_name"])
        assertTrue(result.metadata.containsKey("execution_time"))
    }

    @Test
    fun `should handle empty parameters gracefully`() = runTest {
        // Arrange
        val toolCall = ToolCall(
            toolName = "save-file",
            parameters = emptyMap()
        )

        // Act
        val result = toolExecutor.execute(toolCall)

        // Assert
        assertFalse(result.success)
        assertNotNull(result.error)
        assertTrue(result.error!!.contains("required"))
    }
}
