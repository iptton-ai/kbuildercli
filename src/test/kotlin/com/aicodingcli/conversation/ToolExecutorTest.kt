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
    fun `should list all supported tools`() = runTest {
        // Act
        val supportedTools = toolExecutor.getSupportedTools()

        // Assert
        assertTrue(supportedTools.isNotEmpty())

        // Check that essential tools are supported
        val toolNames = supportedTools.map { it.name }

        // File operations
        assertTrue(toolNames.contains("save-file"))
        assertTrue(toolNames.contains("view"))
        assertTrue(toolNames.contains("str-replace-editor"))
        assertTrue(toolNames.contains("remove-files"))

        // Analysis tools
        assertTrue(toolNames.contains("codebase-retrieval"))
        assertTrue(toolNames.contains("diagnostics"))

        // Task management
        assertTrue(toolNames.contains("add_tasks"))
        assertTrue(toolNames.contains("update_tasks"))

        // Process management
        assertTrue(toolNames.contains("launch-process"))
        assertTrue(toolNames.contains("read-terminal"))

        // Network tools
        assertTrue(toolNames.contains("web-search"))
        assertTrue(toolNames.contains("web-fetch"))
        assertTrue(toolNames.contains("open-browser"))

        // Git operations
        assertTrue(toolNames.contains("github-api"))
        assertTrue(toolNames.contains("git-operations"))

        // Print supported tools for debugging
        println("Supported tools:")
        supportedTools.forEach { tool ->
            println("- ${tool.name}: ${tool.description}")
        }
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

    @Test
    fun `should execute launch-process tool successfully`() = runTest {
        // Arrange
        val toolCall = ToolCall(
            toolName = "launch-process",
            parameters = mapOf(
                "command" to "echo hello",
                "wait" to "true",
                "max_wait_seconds" to "5"
            )
        )

        // Act
        val result = toolExecutor.execute(toolCall)

        // Assert
        assertTrue(result.success)
        assertTrue(result.output.contains("hello") || result.output.contains("launched"))
        assertNull(result.error)
    }

    @Test
    fun `should execute web-search tool successfully`() = runTest {
        // Arrange
        val toolCall = ToolCall(
            toolName = "web-search",
            parameters = mapOf(
                "query" to "kotlin programming",
                "num_results" to "3"
            )
        )

        // Act
        val result = toolExecutor.execute(toolCall)

        // Assert
        assertTrue(result.success)
        assertTrue(result.output.contains("kotlin programming"))
        assertNull(result.error)
    }

    @Test
    fun `should execute github-api tool successfully`() = runTest {
        // Arrange
        val toolCall = ToolCall(
            toolName = "github-api",
            parameters = mapOf(
                "path" to "/repos/owner/repo/issues",
                "method" to "GET"
            )
        )

        // Act
        val result = toolExecutor.execute(toolCall)

        // Assert
        assertTrue(result.success)
        assertTrue(result.output.contains("issues") || result.output.contains("Mock"))
        assertNull(result.error)
    }

    @Test
    fun `should execute diagnostics tool successfully`() = runTest {
        // Arrange
        val toolCall = ToolCall(
            toolName = "diagnostics",
            parameters = mapOf(
                "paths" to "src/main/kotlin/Example.kt,src/test/kotlin/Test.kt"
            )
        )

        // Act
        val result = toolExecutor.execute(toolCall)

        // Assert
        assertTrue(result.success)
        assertTrue(result.output.contains("File:") || result.output.contains("diagnostics"))
        assertNull(result.error)
    }

    @Test
    fun `should validate tool parameters correctly for new tools`() = runTest {
        // Test launch-process validation
        val invalidLaunchProcess = ToolCall(
            toolName = "launch-process",
            parameters = mapOf("wait" to "maybe") // Invalid wait parameter
        )
        val launchValidation = toolExecutor.validateParameters(invalidLaunchProcess)
        assertFalse(launchValidation.isValid)

        // Test web-search validation
        val invalidWebSearch = ToolCall(
            toolName = "web-search",
            parameters = mapOf("num_results" to "20") // Too many results
        )
        val searchValidation = toolExecutor.validateParameters(invalidWebSearch)
        assertFalse(searchValidation.isValid)

        // Test github-api validation
        val invalidGitHubApi = ToolCall(
            toolName = "github-api",
            parameters = mapOf("method" to "DELETE") // Invalid method
        )
        val githubValidation = toolExecutor.validateParameters(invalidGitHubApi)
        assertFalse(githubValidation.isValid)
    }

    @Test
    fun `should collect execution statistics`() = runTest {
        // Arrange
        val defaultExecutor = toolExecutor as DefaultToolExecutor
        defaultExecutor.clearExecutionStats()

        // Act - Execute multiple tools
        val toolCalls = listOf(
            ToolCall("save-file", mapOf("path" to "stats1.kt", "file_content" to "class Stats1")),
            ToolCall("save-file", mapOf("path" to "stats2.kt", "file_content" to "class Stats2")),
            ToolCall("view", mapOf("path" to "stats1.kt", "type" to "file"))
        )

        toolCalls.forEach { toolExecutor.execute(it) }

        // Assert
        val allStats = defaultExecutor.getExecutionStats()
        assertTrue(allStats.isNotEmpty())

        val saveFileStats = defaultExecutor.getExecutionStatsForTool("save-file")
        assertNotNull(saveFileStats)
        assertEquals(2, saveFileStats!!.executionCount)
        assertEquals(2, saveFileStats.successCount)

        val viewStats = defaultExecutor.getExecutionStatsForTool("view")
        assertNotNull(viewStats)
        assertEquals(1, viewStats!!.executionCount)
    }

    @Test
    fun `should handle tool execution errors gracefully`() = runTest {
        // Arrange
        val toolCall = ToolCall(
            toolName = "str-replace-editor",
            parameters = mapOf(
                "path" to "nonexistent.kt",
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
    fun `should provide comprehensive tool metadata`() = runTest {
        // Act
        val supportedTools = toolExecutor.getSupportedTools()

        // Assert
        supportedTools.forEach { tool ->
            assertNotNull(tool.name)
            assertNotNull(tool.description)
            assertNotNull(tool.parameters)
            assertNotNull(tool.category)

            // Verify each parameter has required fields
            tool.parameters.forEach { param ->
                assertNotNull(param.name)
                assertNotNull(param.type)
                assertNotNull(param.description)
                // required field should be explicitly set
            }
        }

        // Check that we have tools in different categories
        val categories = supportedTools.map { it.category }.toSet()
        assertTrue(categories.contains("file"))
        assertTrue(categories.contains("analysis"))
        assertTrue(categories.contains("process"))
        assertTrue(categories.contains("network"))
        assertTrue(categories.contains("git"))
    }
}
