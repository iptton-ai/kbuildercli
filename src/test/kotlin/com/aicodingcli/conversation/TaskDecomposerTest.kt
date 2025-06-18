package com.aicodingcli.conversation

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class TaskDecomposerTest {

    private lateinit var taskDecomposer: TaskDecomposer

    @BeforeEach
    fun setUp() {
        taskDecomposer = DefaultTaskDecomposer()
    }

    @Test
    fun `should decompose simple requirement into basic tasks`() = runTest {
        // Arrange
        val requirement = "Create a simple Kotlin data class with name and age properties"
        val context = ProjectContext(
            projectPath = "/test/project",
            language = "kotlin",
            framework = "spring-boot",
            buildTool = "gradle"
        )

        // Act
        val tasks = taskDecomposer.decompose(requirement, context)

        // Assert
        assertNotNull(tasks)
        assertTrue(tasks.isNotEmpty())
        
        // Should have at least file creation and code generation tasks
        val fileCreationTask = tasks.find { it.description.contains("create", ignoreCase = true) }
        assertNotNull(fileCreationTask, "Should have a file creation task")
        
        val codeGenerationTask = tasks.find { it.description.contains("class", ignoreCase = true) }
        assertNotNull(codeGenerationTask, "Should have a code generation task")
        
        // Tasks should have tool calls
        tasks.forEach { task ->
            assertTrue(task.toolCalls.isNotEmpty(), "Each task should have tool calls")
        }
    }

    @Test
    fun `should decompose complex requirement into multiple dependent tasks`() = runTest {
        // Arrange
        val requirement = "Create a REST API endpoint for user management with CRUD operations, including validation and tests"
        val context = ProjectContext(
            projectPath = "/test/project",
            language = "kotlin",
            framework = "spring-boot",
            buildTool = "gradle"
        )

        // Act
        val tasks = taskDecomposer.decompose(requirement, context)

        // Assert
        assertTrue(tasks.size >= 4, "Complex requirement should generate multiple tasks, but got ${tasks.size}")
        
        // Should have tasks for model, controller, service, and tests
        val modelTask = tasks.find { it.description.contains("model", ignoreCase = true) || it.description.contains("entity", ignoreCase = true) }
        val controllerTask = tasks.find { it.description.contains("controller", ignoreCase = true) || it.description.contains("endpoint", ignoreCase = true) }
        val testTask = tasks.find { it.description.contains("test", ignoreCase = true) }
        
        assertNotNull(modelTask, "Should have a model/entity task")
        assertNotNull(controllerTask, "Should have a controller/endpoint task")
        assertNotNull(testTask, "Should have a test task")
        
        // Check dependencies - controller should depend on service, service should depend on model
        val serviceTask = tasks.find { it.description.contains("service", ignoreCase = true) }
        assertNotNull(serviceTask, "Should have a service task")

        val controllerDependsOnService = controllerTask!!.dependencies.contains(serviceTask!!.id)
        val serviceDependsOnModel = serviceTask.dependencies.contains(modelTask!!.id)

        assertTrue(controllerDependsOnService, "Controller task should depend on service task")
        assertTrue(serviceDependsOnModel, "Service task should depend on model task")
    }

    @Test
    fun `should assign appropriate priorities to tasks`() = runTest {
        // Arrange
        val requirement = "Create a user service with database integration and API endpoints"
        val context = ProjectContext(
            projectPath = "/test/project",
            language = "kotlin",
            framework = "spring-boot",
            buildTool = "gradle"
        )

        // Act
        val tasks = taskDecomposer.decompose(requirement, context)

        // Assert
        val priorities = tasks.map { it.priority }.distinct().sorted()
        assertTrue(priorities.size > 1, "Should have different priority levels")
        
        // Foundation tasks should have higher priority (lower number)
        val foundationTask = tasks.find { it.description.contains("model", ignoreCase = true) || it.description.contains("entity", ignoreCase = true) }
        val apiTask = tasks.find { it.description.contains("endpoint", ignoreCase = true) || it.description.contains("controller", ignoreCase = true) }
        
        if (foundationTask != null && apiTask != null) {
            assertTrue(foundationTask.priority <= apiTask.priority, "Foundation tasks should have higher priority")
        }
    }

    @Test
    fun `should generate appropriate tool calls for each task`() = runTest {
        // Arrange
        val requirement = "Create a simple configuration file for database connection"
        val context = ProjectContext(
            projectPath = "/test/project",
            language = "kotlin",
            framework = "spring-boot",
            buildTool = "gradle"
        )

        // Act
        val tasks = taskDecomposer.decompose(requirement, context)

        // Assert
        assertTrue(tasks.isNotEmpty())
        
        val configTask = tasks.first()
        assertTrue(configTask.toolCalls.isNotEmpty(), "Task should have tool calls")
        
        // Should have file creation tool call
        val fileCreationCall = configTask.toolCalls.find { it.toolName == "save-file" }
        assertNotNull(fileCreationCall, "Should have save-file tool call")
        
        // Tool call should have required parameters
        assertTrue(fileCreationCall!!.parameters.containsKey("path"), "Should have path parameter")
        assertTrue(fileCreationCall.parameters.containsKey("file_content"), "Should have file_content parameter")
    }

    @Test
    fun `should refine task based on feedback`() = runTest {
        // Arrange
        val originalTask = ExecutableTask(
            description = "Create a simple class",
            toolCalls = listOf(
                ToolCall(
                    toolName = "save-file",
                    parameters = mapOf("path" to "User.kt", "file_content" to "class User")
                )
            )
        )
        val feedback = "Add validation annotations and make properties immutable"

        // Act
        val refinedTask = taskDecomposer.refineTask(originalTask, feedback)

        // Assert
        assertNotEquals(originalTask.description, refinedTask.description)
        assertTrue(refinedTask.description.contains("validation", ignoreCase = true) || 
                  refinedTask.description.contains("immutable", ignoreCase = true),
                  "Refined task should incorporate feedback")
        
        // Tool calls might be updated
        assertNotNull(refinedTask.toolCalls)
    }

    @Test
    fun `should validate task sequence for dependency conflicts`() = runTest {
        // Arrange
        val task1 = ExecutableTask(
            id = "task1",
            description = "Create model",
            toolCalls = listOf(ToolCall("save-file", mapOf("path" to "Model.kt"))),
            dependencies = listOf("task2") // Circular dependency
        )
        val task2 = ExecutableTask(
            id = "task2", 
            description = "Create controller",
            toolCalls = listOf(ToolCall("save-file", mapOf("path" to "Controller.kt"))),
            dependencies = listOf("task1") // Circular dependency
        )
        val tasks = listOf(task1, task2)

        // Act
        val validation = taskDecomposer.validateTaskSequence(tasks)

        // Assert
        assertFalse(validation.isValid, "Should detect circular dependency")
        assertTrue(validation.errors.any { it.contains("circular", ignoreCase = true) || it.contains("dependency", ignoreCase = true) },
                  "Should report circular dependency error")
    }

    @Test
    fun `should validate task sequence successfully for valid dependencies`() = runTest {
        // Arrange
        val task1 = ExecutableTask(
            id = "task1",
            description = "Create model",
            toolCalls = listOf(ToolCall("save-file", mapOf("path" to "Model.kt"))),
            dependencies = emptyList()
        )
        val task2 = ExecutableTask(
            id = "task2",
            description = "Create controller", 
            toolCalls = listOf(ToolCall("save-file", mapOf("path" to "Controller.kt"))),
            dependencies = listOf("task1")
        )
        val tasks = listOf(task1, task2)

        // Act
        val validation = taskDecomposer.validateTaskSequence(tasks)

        // Assert
        assertTrue(validation.isValid, "Should validate correct dependency sequence")
        assertTrue(validation.errors.isEmpty(), "Should have no errors for valid sequence")
    }

    @Test
    fun `should handle empty requirement gracefully`() = runTest {
        // Arrange
        val requirement = ""
        val context = ProjectContext(
            projectPath = "/test/project",
            language = "kotlin"
        )

        // Act
        val tasks = taskDecomposer.decompose(requirement, context)

        // Assert
        assertTrue(tasks.isEmpty(), "Empty requirement should result in empty task list")
    }

    @Test
    fun `should adapt tasks based on project context`() = runTest {
        // Arrange
        val requirement = "Create a data model"
        val springContext = ProjectContext(
            projectPath = "/test/project",
            language = "kotlin",
            framework = "spring-boot",
            buildTool = "gradle"
        )
        val plainContext = ProjectContext(
            projectPath = "/test/project", 
            language = "kotlin",
            buildTool = "gradle"
        )

        // Act
        val springTasks = taskDecomposer.decompose(requirement, springContext)
        val plainTasks = taskDecomposer.decompose(requirement, plainContext)

        // Assert
        assertNotNull(springTasks)
        assertNotNull(plainTasks)
        
        // Spring context might generate additional tasks (e.g., JPA annotations)
        val springHasJpa = springTasks.any { task ->
            task.description.contains("jpa", ignoreCase = true) || 
            task.description.contains("entity", ignoreCase = true) ||
            task.toolCalls.any { it.parameters.values.any { value -> value.contains("@Entity", ignoreCase = true) } }
        }
        
        val plainHasJpa = plainTasks.any { task ->
            task.description.contains("jpa", ignoreCase = true) || 
            task.description.contains("entity", ignoreCase = true) ||
            task.toolCalls.any { it.parameters.values.any { value -> value.contains("@Entity", ignoreCase = true) } }
        }
        
        // Spring context should be more likely to include JPA-related tasks
        if (springHasJpa || plainHasJpa) {
            assertTrue(springHasJpa || !plainHasJpa, "Spring context should handle JPA appropriately")
        }
    }
}
