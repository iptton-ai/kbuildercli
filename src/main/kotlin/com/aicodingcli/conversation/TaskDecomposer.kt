package com.aicodingcli.conversation

/**
 * Interface for decomposing requirements into executable tasks
 */
interface TaskDecomposer {
    /**
     * Decompose a requirement into executable tasks
     */
    suspend fun decompose(requirement: String, context: ProjectContext): List<ExecutableTask>
    
    /**
     * Refine a task based on feedback
     */
    suspend fun refineTask(task: ExecutableTask, feedback: String): ExecutableTask
    
    /**
     * Validate task sequence for dependency conflicts
     */
    suspend fun validateTaskSequence(tasks: List<ExecutableTask>): ValidationResult
}

/**
 * Default implementation of TaskDecomposer
 */
class DefaultTaskDecomposer : TaskDecomposer {
    
    override suspend fun decompose(requirement: String, context: ProjectContext): List<ExecutableTask> {
        if (requirement.isBlank()) {
            return emptyList()
        }
        
        val tasks = mutableListOf<ExecutableTask>()
        val requirementLower = requirement.lowercase()
        
        // Analyze requirement and generate appropriate tasks
        when {
            isSimpleClassCreation(requirementLower) -> {
                tasks.addAll(generateClassCreationTasks(requirement, context))
            }
            isRestApiRequirement(requirementLower) -> {
                tasks.addAll(generateRestApiTasks(requirement, context))
            }
            isConfigurationRequirement(requirementLower) -> {
                tasks.addAll(generateConfigurationTasks(requirement, context))
            }
            isDataModelRequirement(requirementLower) -> {
                tasks.addAll(generateDataModelTasks(requirement, context))
            }
            else -> {
                // Generic task generation
                tasks.addAll(generateGenericTasks(requirement, context))
            }
        }
        
        // Assign priorities and dependencies
        assignPrioritiesAndDependencies(tasks, context)
        
        return tasks
    }
    
    override suspend fun refineTask(task: ExecutableTask, feedback: String): ExecutableTask {
        val feedbackLower = feedback.lowercase()
        var refinedDescription = task.description
        var refinedToolCalls = task.toolCalls.toMutableList()
        
        // Incorporate feedback into task description
        when {
            feedbackLower.contains("validation") -> {
                refinedDescription += " with validation annotations"
            }
            feedbackLower.contains("immutable") -> {
                refinedDescription += " with immutable properties"
            }
            feedbackLower.contains("test") -> {
                refinedDescription += " including unit tests"
            }
        }
        
        // Update tool calls based on feedback
        if (feedbackLower.contains("validation") || feedbackLower.contains("immutable")) {
            refinedToolCalls = refinedToolCalls.map { toolCall ->
                if (toolCall.toolName == "save-file" && toolCall.parameters.containsKey("file_content")) {
                    val content = toolCall.parameters["file_content"] ?: ""
                    val enhancedContent = enhanceCodeWithFeedback(content, feedback)
                    toolCall.copy(parameters = toolCall.parameters + ("file_content" to enhancedContent))
                } else {
                    toolCall
                }
            }.toMutableList()
        }
        
        return task.copy(
            description = refinedDescription,
            toolCalls = refinedToolCalls
        )
    }
    
    override suspend fun validateTaskSequence(tasks: List<ExecutableTask>): ValidationResult {
        val errors = mutableListOf<String>()
        val taskIds = tasks.map { it.id }.toSet()
        
        // Check for circular dependencies
        if (hasCircularDependencies(tasks)) {
            errors.add("Circular dependency detected in task sequence")
        }
        
        // Check for missing dependencies
        tasks.forEach { task ->
            task.dependencies.forEach { depId ->
                if (depId !in taskIds) {
                    errors.add("Task ${task.id} depends on non-existent task $depId")
                }
            }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.valid()
        } else {
            ValidationResult.invalid(errors)
        }
    }
    
    private fun isSimpleClassCreation(requirement: String): Boolean {
        return requirement.contains("class") &&
               !requirement.contains("api") && !requirement.contains("endpoint") &&
               !requirement.contains("rest") && !requirement.contains("crud") &&
               (requirement.contains("simple") || requirement.contains("data") ||
                requirement.contains("create") || requirement.contains("user") ||
                requirement.contains("person") || requirement.contains("customer") ||
                requirement.contains("product") || requirement.contains("validation"))
    }
    
    private fun isRestApiRequirement(requirement: String): Boolean {
        return requirement.contains("api") || requirement.contains("endpoint") || 
               requirement.contains("rest") || requirement.contains("crud")
    }
    
    private fun isConfigurationRequirement(requirement: String): Boolean {
        return requirement.contains("config") || requirement.contains("properties") ||
               requirement.contains("settings")
    }
    
    private fun isDataModelRequirement(requirement: String): Boolean {
        return requirement.contains("model") || requirement.contains("entity") ||
               (requirement.contains("data") && !requirement.contains("api"))
    }
    
    private fun generateClassCreationTasks(requirement: String, context: ProjectContext): List<ExecutableTask> {
        val tasks = mutableListOf<ExecutableTask>()
        
        // Extract class name from requirement (simplified)
        val className = extractClassName(requirement) ?: "MyClass"
        val filePath = if (context.projectPath.isBlank()) {
            "${className}.kt"
        } else {
            "${context.projectPath}/src/main/kotlin/${className}.kt"
        }
        
        // Generate basic class content
        val classContent = generateBasicClassContent(className, requirement, context)
        
        tasks.add(
            ExecutableTask(
                description = "Create $className class file",
                toolCalls = listOf(
                    ToolCall(
                        toolName = "save-file",
                        parameters = mapOf(
                            "path" to filePath,
                            "file_content" to classContent
                        )
                    )
                ),
                priority = 1
            )
        )
        
        return tasks
    }
    
    private fun generateRestApiTasks(requirement: String, context: ProjectContext): List<ExecutableTask> {
        val tasks = mutableListOf<ExecutableTask>()

        // Model/Entity task
        val modelTask = ExecutableTask(
            id = "model-task",
            description = "Create data model/entity class",
            toolCalls = listOf(
                ToolCall(
                    toolName = "save-file",
                    parameters = mapOf(
                        "path" to if (context.projectPath.isBlank()) "User.kt" else "${context.projectPath}/src/main/kotlin/User.kt",
                        "file_content" to generateEntityClass(context)
                    )
                )
            ),
            priority = 1
        )
        tasks.add(modelTask)

        // Service task
        val serviceTask = ExecutableTask(
            id = "service-task",
            description = "Create service layer for business logic",
            toolCalls = listOf(
                ToolCall(
                    toolName = "save-file",
                    parameters = mapOf(
                        "path" to if (context.projectPath.isBlank()) "UserService.kt" else "${context.projectPath}/src/main/kotlin/UserService.kt",
                        "file_content" to generateServiceClass(context)
                    )
                )
            ),
            dependencies = listOf(modelTask.id),
            priority = 2
        )
        tasks.add(serviceTask)

        // Controller task
        val controllerTask = ExecutableTask(
            id = "controller-task",
            description = "Create REST controller with CRUD endpoints",
            toolCalls = listOf(
                ToolCall(
                    toolName = "save-file",
                    parameters = mapOf(
                        "path" to if (context.projectPath.isBlank()) "UserController.kt" else "${context.projectPath}/src/main/kotlin/UserController.kt",
                        "file_content" to generateControllerClass(context)
                    )
                )
            ),
            dependencies = listOf(serviceTask.id),
            priority = 3
        )
        tasks.add(controllerTask)

        // Validation task (if mentioned in requirement)
        if (requirement.contains("validation")) {
            val validationTask = ExecutableTask(
                id = "validation-task",
                description = "Add validation annotations and constraints",
                toolCalls = listOf(
                    ToolCall(
                        toolName = "str-replace-editor",
                        parameters = mapOf(
                            "path" to "${context.projectPath}/src/main/kotlin/User.kt",
                            "old_str" to "val name: String",
                            "new_str" to "@field:NotBlank val name: String"
                        )
                    )
                ),
                dependencies = listOf(modelTask.id),
                priority = 2
            )
            tasks.add(validationTask)
        }

        // Test task
        if (requirement.contains("test")) {
            val testTask = ExecutableTask(
                id = "test-task",
                description = "Create unit tests for API endpoints",
                toolCalls = listOf(
                    ToolCall(
                        toolName = "save-file",
                        parameters = mapOf(
                            "path" to "${context.projectPath}/src/test/kotlin/UserControllerTest.kt",
                            "file_content" to generateTestClass(context)
                        )
                    )
                ),
                dependencies = listOf(controllerTask.id),
                priority = 4
            )
            tasks.add(testTask)
        }

        return tasks
    }
    
    private fun generateConfigurationTasks(requirement: String, context: ProjectContext): List<ExecutableTask> {
        val tasks = mutableListOf<ExecutableTask>()
        
        val configContent = if (requirement.contains("database")) {
            generateDatabaseConfig(context)
        } else {
            generateGenericConfig()
        }
        
        tasks.add(
            ExecutableTask(
                description = "Create configuration file",
                toolCalls = listOf(
                    ToolCall(
                        toolName = "save-file",
                        parameters = mapOf(
                            "path" to "${context.projectPath}/src/main/resources/application.yml",
                            "file_content" to configContent
                        )
                    )
                ),
                priority = 1
            )
        )
        
        return tasks
    }
    
    private fun generateDataModelTasks(requirement: String, context: ProjectContext): List<ExecutableTask> {
        val tasks = mutableListOf<ExecutableTask>()
        
        val modelContent = if (context.framework == "spring-boot") {
            generateSpringDataModel()
        } else {
            generatePlainDataModel()
        }
        
        tasks.add(
            ExecutableTask(
                description = "Create data model class",
                toolCalls = listOf(
                    ToolCall(
                        toolName = "save-file",
                        parameters = mapOf(
                            "path" to "${context.projectPath}/src/main/kotlin/DataModel.kt",
                            "file_content" to modelContent
                        )
                    )
                ),
                priority = 1
            )
        )
        
        return tasks
    }
    
    private fun generateGenericTasks(requirement: String, context: ProjectContext): List<ExecutableTask> {
        return listOf(
            ExecutableTask(
                description = "Implement requirement: $requirement",
                toolCalls = listOf(
                    ToolCall(
                        toolName = "save-file",
                        parameters = mapOf(
                            "path" to "${context.projectPath}/src/main/kotlin/Implementation.kt",
                            "file_content" to "// TODO: Implement $requirement"
                        )
                    )
                ),
                priority = 1
            )
        )
    }
    
    private fun assignPrioritiesAndDependencies(tasks: MutableList<ExecutableTask>, context: ProjectContext) {
        // Priority assignment is already done in individual generators
        // This method can be used for additional logic if needed
    }
    
    private fun hasCircularDependencies(tasks: List<ExecutableTask>): Boolean {
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        
        fun dfs(taskId: String): Boolean {
            if (recursionStack.contains(taskId)) return true
            if (visited.contains(taskId)) return false
            
            visited.add(taskId)
            recursionStack.add(taskId)
            
            val task = tasks.find { it.id == taskId }
            task?.dependencies?.forEach { depId ->
                if (dfs(depId)) return true
            }
            
            recursionStack.remove(taskId)
            return false
        }
        
        return tasks.any { dfs(it.id) }
    }
    
    private fun extractClassName(requirement: String): String? {
        // Simple extraction - look for patterns like "create X class" or "X data class"
        val words = requirement.split(" ")
        val classIndex = words.indexOfFirst { it.lowercase() == "class" }

        return when {
            // First check for specific class names mentioned in the requirement
            requirement.contains("user", ignoreCase = true) -> "User"
            requirement.contains("person", ignoreCase = true) -> "Person"
            requirement.contains("customer", ignoreCase = true) -> "Customer"
            requirement.contains("product", ignoreCase = true) -> "Product"
            // Then try to extract from position relative to "class"
            classIndex > 0 -> {
                val candidateName = words[classIndex - 1]
                // Skip common words like "data", "simple", etc.
                if (candidateName.lowercase() in listOf("data", "simple", "basic", "new", "a", "an", "the")) {
                    // Look for a proper noun before these words
                    if (classIndex > 1) {
                        words[classIndex - 2].replaceFirstChar { it.uppercase() }
                    } else {
                        "MyClass"
                    }
                } else {
                    candidateName.replaceFirstChar { it.uppercase() }
                }
            }
            else -> "MyClass"
        }
    }
    
    private fun generateBasicClassContent(className: String, requirement: String, context: ProjectContext): String {
        val properties = extractProperties(requirement)
        val propertiesCode = properties.joinToString("\n    ") { "val $it: String" }
        
        return """
            data class $className(
                $propertiesCode
            )
        """.trimIndent()
    }
    
    private fun extractProperties(requirement: String): List<String> {
        val properties = mutableListOf<String>()
        
        if (requirement.contains("name", ignoreCase = true)) properties.add("name")
        if (requirement.contains("age", ignoreCase = true)) properties.add("age")
        if (requirement.contains("email", ignoreCase = true)) properties.add("email")
        
        return properties.ifEmpty { listOf("id", "name") }
    }
    
    private fun generateEntityClass(context: ProjectContext): String {
        return if (context.framework == "spring-boot") {
            """
                import javax.persistence.*
                
                @Entity
                @Table(name = "users")
                data class User(
                    @Id
                    @GeneratedValue(strategy = GenerationType.IDENTITY)
                    val id: Long = 0,
                    
                    @Column(nullable = false)
                    val name: String,
                    
                    @Column(nullable = false, unique = true)
                    val email: String
                )
            """.trimIndent()
        } else {
            """
                data class User(
                    val id: Long,
                    val name: String,
                    val email: String
                )
            """.trimIndent()
        }
    }
    
    private fun generateServiceClass(context: ProjectContext): String {
        return """
            import org.springframework.stereotype.Service

            @Service
            class UserService {

                fun getAllUsers(): List<User> {
                    // TODO: Implement business logic
                    return emptyList()
                }

                fun createUser(user: User): User {
                    // TODO: Implement business logic
                    return user
                }

                fun getUserById(id: Long): User? {
                    // TODO: Implement business logic
                    return null
                }

                fun updateUser(id: Long, user: User): User {
                    // TODO: Implement business logic
                    return user
                }

                fun deleteUser(id: Long) {
                    // TODO: Implement business logic
                }
            }
        """.trimIndent()
    }

    private fun generateControllerClass(context: ProjectContext): String {
        return """
            import org.springframework.web.bind.annotation.*

            @RestController
            @RequestMapping("/api/users")
            class UserController(private val userService: UserService) {

                @GetMapping
                fun getAllUsers(): List<User> {
                    return userService.getAllUsers()
                }

                @PostMapping
                fun createUser(@RequestBody user: User): User {
                    return userService.createUser(user)
                }

                @GetMapping("/{id}")
                fun getUserById(@PathVariable id: Long): User? {
                    return userService.getUserById(id)
                }

                @PutMapping("/{id}")
                fun updateUser(@PathVariable id: Long, @RequestBody user: User): User {
                    return userService.updateUser(id, user)
                }

                @DeleteMapping("/{id}")
                fun deleteUser(@PathVariable id: Long) {
                    userService.deleteUser(id)
                }
            }
        """.trimIndent()
    }
    
    private fun generateTestClass(context: ProjectContext): String {
        return """
            import org.junit.jupiter.api.Test
            import org.junit.jupiter.api.Assertions.*
            
            class UserControllerTest {
                
                @Test
                fun `should get all users`() {
                    // TODO: Implement test
                }
                
                @Test
                fun `should create user`() {
                    // TODO: Implement test
                }
                
                @Test
                fun `should get user by id`() {
                    // TODO: Implement test
                }
                
                @Test
                fun `should update user`() {
                    // TODO: Implement test
                }
                
                @Test
                fun `should delete user`() {
                    // TODO: Implement test
                }
            }
        """.trimIndent()
    }
    
    private fun generateDatabaseConfig(context: ProjectContext): String {
        return """
            spring:
              datasource:
                url: jdbc:h2:mem:testdb
                driver-class-name: org.h2.Driver
                username: sa
                password: 
              jpa:
                hibernate:
                  ddl-auto: create-drop
                show-sql: true
        """.trimIndent()
    }
    
    private fun generateGenericConfig(): String {
        return """
            # Application Configuration
            app:
              name: MyApplication
              version: 1.0.0
        """.trimIndent()
    }
    
    private fun generateSpringDataModel(): String {
        return """
            import javax.persistence.*
            
            @Entity
            data class DataModel(
                @Id
                @GeneratedValue(strategy = GenerationType.IDENTITY)
                val id: Long = 0,
                
                val name: String,
                val value: String
            )
        """.trimIndent()
    }
    
    private fun generatePlainDataModel(): String {
        return """
            data class DataModel(
                val id: Long,
                val name: String,
                val value: String
            )
        """.trimIndent()
    }
    
    private fun enhanceCodeWithFeedback(content: String, feedback: String): String {
        var enhancedContent = content
        
        if (feedback.contains("validation", ignoreCase = true)) {
            enhancedContent = enhancedContent.replace("val ", "@field:NotBlank val ")
        }
        
        if (feedback.contains("immutable", ignoreCase = true)) {
            enhancedContent = enhancedContent.replace("var ", "val ")
        }
        
        return enhancedContent
    }
}
