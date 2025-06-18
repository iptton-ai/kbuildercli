package com.aicodingcli.conversation

/**
 * Interface for parsing natural language requirements
 */
interface RequirementParser {
    /**
     * Parse a requirement into intent and parameters
     */
    suspend fun parse(requirement: String): ParsedRequirement
    
    /**
     * Extract intent from requirement
     */
    suspend fun extractIntent(requirement: String): Intent
    
    /**
     * Extract parameters based on intent
     */
    suspend fun extractParameters(requirement: String, intent: Intent): Map<String, String>
}

/**
 * Default implementation of RequirementParser
 */
class DefaultRequirementParser : RequirementParser {
    
    override suspend fun parse(requirement: String): ParsedRequirement {
        if (requirement.isBlank()) {
            return ParsedRequirement(
                intent = Intent.UNKNOWN,
                parameters = emptyMap(),
                originalText = requirement
            )
        }
        
        val intent = extractIntent(requirement)
        val parameters = extractParameters(requirement, intent)
        
        return ParsedRequirement(
            intent = intent,
            parameters = parameters,
            originalText = requirement
        )
    }
    
    override suspend fun extractIntent(requirement: String): Intent {
        val requirementLower = requirement.lowercase()

        return when {
            // Order matters - more specific patterns first
            isTestCreation(requirementLower) -> Intent.CREATE_TESTS
            isDocumentationCreation(requirementLower) -> Intent.CREATE_DOCUMENTATION
            isSystemCreation(requirementLower) -> Intent.CREATE_SYSTEM
            isFormCreation(requirementLower) -> Intent.CREATE_FORM
            isServiceCreation(requirementLower) -> Intent.CREATE_SERVICE
            isConfigCreation(requirementLower) -> Intent.CREATE_CONFIG
            isApiCreation(requirementLower) -> Intent.CREATE_API
            isClassCreation(requirementLower) -> Intent.CREATE_CLASS

            // Modification patterns
            isRefactoring(requirementLower) -> Intent.REFACTOR_CODE
            isOptimization(requirementLower) -> Intent.OPTIMIZE_PERFORMANCE
            isImprovement(requirementLower) -> Intent.IMPROVE_CODE
            isBugFix(requirementLower) -> Intent.FIX_BUG
            isFeatureAddition(requirementLower) -> Intent.ADD_FEATURE
            isFeatureRemoval(requirementLower) -> Intent.REMOVE_FEATURE
            isDependencyUpdate(requirementLower) -> Intent.UPDATE_DEPENDENCY

            else -> Intent.UNKNOWN
        }
    }
    
    override suspend fun extractParameters(requirement: String, intent: Intent): Map<String, String> {
        val parameters = mutableMapOf<String, String>()
        val requirementLower = requirement.lowercase()
        
        // Add default language
        parameters["language"] = "kotlin"
        
        when (intent) {
            Intent.CREATE_CLASS -> extractClassParameters(requirement, parameters)
            Intent.CREATE_API -> extractApiParameters(requirement, parameters)
            Intent.CREATE_CONFIG -> extractConfigParameters(requirement, parameters)
            Intent.CREATE_TESTS -> extractTestParameters(requirement, parameters)
            Intent.CREATE_SERVICE -> extractServiceParameters(requirement, parameters)
            Intent.CREATE_SYSTEM -> extractSystemParameters(requirement, parameters)
            Intent.CREATE_FORM -> extractFormParameters(requirement, parameters)
            Intent.CREATE_DOCUMENTATION -> extractDocumentationParameters(requirement, parameters)
            Intent.REFACTOR_CODE -> extractRefactorParameters(requirement, parameters)
            Intent.OPTIMIZE_PERFORMANCE -> extractOptimizationParameters(requirement, parameters)
            Intent.IMPROVE_CODE -> extractImprovementParameters(requirement, parameters)
            else -> {
                // For unknown intents, try to extract general parameters
                if (requirementLower.isEmpty()) {
                    // Empty requirement
                } else {
                    parameters["scope"] = "general"
                }
            }
        }
        
        return parameters
    }
    
    // Intent detection methods
    private fun isClassCreation(requirement: String): Boolean {
        return (requirement.contains("create") || requirement.contains("generate")) &&
               (requirement.contains("class") || requirement.contains("interface") ||
                requirement.contains("entity") || requirement.contains("model") ||
                requirement.contains("data class")) &&
               !requirement.contains("api") && !requirement.contains("endpoint") &&
               !(requirement.contains("service") && !requirement.contains("class")) // Allow "service class" but exclude standalone "service"
    }
    
    private fun isApiCreation(requirement: String): Boolean {
        return (requirement.contains("create") || requirement.contains("build") || requirement.contains("generate")) &&
               (requirement.contains("api") || requirement.contains("endpoint") || 
                requirement.contains("rest") || requirement.contains("graphql"))
    }
    
    private fun isConfigCreation(requirement: String): Boolean {
        return (requirement.contains("create") || requirement.contains("generate")) &&
               (requirement.contains("config") || requirement.contains("properties") ||
                requirement.contains("settings") || requirement.contains("configuration")) &&
               !requirement.contains("class") && !requirement.contains("data class") // Exclude class creation
    }
    
    private fun isTestCreation(requirement: String): Boolean {
        return (requirement.contains("generate") || requirement.contains("write") || requirement.contains("create")) &&
               (requirement.contains("test") || requirement.contains("spec")) &&
               !requirement.contains("api") && !requirement.contains("endpoint")
    }
    
    private fun isServiceCreation(requirement: String): Boolean {
        return (requirement.contains("create") || requirement.contains("build")) &&
               (requirement.contains("service") || requirement.contains("microservice")) &&
               !requirement.contains("class") && // Exclude "service class" which should be CREATE_CLASS
               !requirement.contains("delete") // Exclude refactoring operations
    }
    
    private fun isSystemCreation(requirement: String): Boolean {
        return (requirement.contains("create") || requirement.contains("build")) &&
               (requirement.contains("system") || requirement.contains("application") ||
                (requirement.contains("complete") && requirement.contains("e-commerce")))
    }
    
    private fun isFormCreation(requirement: String): Boolean {
        return (requirement.contains("create") || requirement.contains("build")) &&
               (requirement.contains("form") || requirement.contains("registration") ||
                requirement.contains("login"))
    }
    
    private fun isDocumentationCreation(requirement: String): Boolean {
        return (requirement.contains("generate") || requirement.contains("create")) &&
               (requirement.contains("documentation") || requirement.contains("docs")) &&
               !requirement.contains("class") && !requirement.contains("api endpoint")
    }
    
    private fun isRefactoring(requirement: String): Boolean {
        return requirement.contains("refactor") || 
               (requirement.contains("improve") && requirement.contains("structure")) ||
               (requirement.contains("delete") && requirement.contains("create"))
    }
    
    private fun isImprovement(requirement: String): Boolean {
        return requirement.contains("improve") || requirement.contains("better") ||
               requirement.contains("enhance") || requirement.contains("make it better")
    }
    
    private fun isOptimization(requirement: String): Boolean {
        return requirement.contains("optimize") || requirement.contains("performance") ||
               requirement.contains("faster") || requirement.contains("concurrent")
    }
    
    private fun isBugFix(requirement: String): Boolean {
        return requirement.contains("fix") || requirement.contains("bug") ||
               requirement.contains("error") || requirement.contains("issue")
    }
    
    private fun isFeatureAddition(requirement: String): Boolean {
        return requirement.contains("add") && 
               (requirement.contains("feature") || requirement.contains("functionality"))
    }
    
    private fun isFeatureRemoval(requirement: String): Boolean {
        return requirement.contains("remove") && 
               (requirement.contains("feature") || requirement.contains("functionality"))
    }
    
    private fun isDependencyUpdate(requirement: String): Boolean {
        return requirement.contains("update") && 
               (requirement.contains("dependency") || requirement.contains("version"))
    }
    
    // Parameter extraction methods
    private fun extractClassParameters(requirement: String, parameters: MutableMap<String, String>) {
        // Extract class name
        val className = extractClassName(requirement)
        if (className != null) {
            parameters["className"] = className
        }
        
        // Extract class type
        when {
            requirement.contains("data class", ignoreCase = true) -> parameters["classType"] = "data"
            requirement.contains("entity", ignoreCase = true) -> parameters["classType"] = "entity"
            requirement.contains("interface", ignoreCase = true) -> parameters["classType"] = "interface"
            requirement.contains("service", ignoreCase = true) -> parameters["classType"] = "service"
            else -> parameters["classType"] = "class"
        }
        
        // Extract properties
        val properties = extractProperties(requirement)
        if (properties.isNotEmpty()) {
            parameters["properties"] = properties.joinToString(",")
        }
    }
    
    private fun extractApiParameters(requirement: String, parameters: MutableMap<String, String>) {
        // Extract entity name
        val entity = extractEntityName(requirement)
        if (entity != null) {
            parameters["entity"] = entity
        }
        
        // Extract API type
        when {
            requirement.contains("rest", ignoreCase = true) -> parameters["apiType"] = "REST"
            requirement.contains("graphql", ignoreCase = true) -> parameters["apiType"] = "GraphQL"
            else -> parameters["apiType"] = "REST"
        }
        
        // Extract operations
        val operations = mutableListOf<String>()
        if (requirement.contains("crud", ignoreCase = true)) operations.add("CRUD")
        if (requirement.contains("mutations", ignoreCase = true)) operations.add("mutations")
        if (requirement.contains("queries", ignoreCase = true)) operations.add("queries")
        
        if (operations.isNotEmpty()) {
            parameters["operations"] = operations.joinToString(",")
        }
    }
    
    private fun extractConfigParameters(requirement: String, parameters: MutableMap<String, String>) {
        // Extract config type
        when {
            requirement.contains("database", ignoreCase = true) -> parameters["configType"] = "database"
            requirement.contains("security", ignoreCase = true) -> parameters["configType"] = "security"
            requirement.contains("logging", ignoreCase = true) -> parameters["configType"] = "logging"
            else -> parameters["configType"] = "application"
        }
        
        // Extract settings
        val settings = mutableListOf<String>()
        if (requirement.contains("connection", ignoreCase = true)) settings.add("connection")
        if (requirement.contains("authentication", ignoreCase = true)) settings.add("authentication")
        if (requirement.contains("logging", ignoreCase = true)) settings.add("logging")
        
        if (settings.isNotEmpty()) {
            parameters["settings"] = settings.joinToString(",")
        }
    }
    
    private fun extractTestParameters(requirement: String, parameters: MutableMap<String, String>) {
        // Extract target class
        val targetClass = extractTargetClass(requirement)
        if (targetClass != null) {
            parameters["targetClass"] = targetClass
        }
        
        // Extract test type
        when {
            requirement.contains("unit", ignoreCase = true) -> parameters["testType"] = "unit"
            requirement.contains("integration", ignoreCase = true) -> parameters["testType"] = "integration"
            requirement.contains("e2e", ignoreCase = true) -> parameters["testType"] = "e2e"
            else -> parameters["testType"] = "unit"
        }
        
        // Extract coverage
        when {
            requirement.contains("all methods", ignoreCase = true) -> parameters["coverage"] = "all"
            requirement.contains("complete", ignoreCase = true) -> parameters["coverage"] = "complete"
            else -> parameters["coverage"] = "basic"
        }
    }
    
    private fun extractServiceParameters(requirement: String, parameters: MutableMap<String, String>) {
        // Extract framework
        when {
            requirement.contains("spring boot", ignoreCase = true) -> parameters["framework"] = "Spring Boot"
            requirement.contains("ktor", ignoreCase = true) -> parameters["framework"] = "Ktor"
            else -> parameters["framework"] = "Spring Boot"
        }
        
        // Extract service type
        when {
            requirement.contains("microservice", ignoreCase = true) -> parameters["serviceType"] = "microservice"
            requirement.contains("web service", ignoreCase = true) -> parameters["serviceType"] = "web service"
            else -> parameters["serviceType"] = "service"
        }
        
        // Extract technologies
        val technologies = mutableListOf<String>()
        if (requirement.contains("jpa", ignoreCase = true)) technologies.add("JPA")
        if (requirement.contains("postgresql", ignoreCase = true)) technologies.add("PostgreSQL")
        if (requirement.contains("mysql", ignoreCase = true)) technologies.add("MySQL")
        if (requirement.contains("redis", ignoreCase = true)) technologies.add("Redis")
        
        if (technologies.isNotEmpty()) {
            parameters["technologies"] = technologies.joinToString(",")
        }
    }
    
    private fun extractSystemParameters(requirement: String, parameters: MutableMap<String, String>) {
        // Extract entities
        val entities = extractEntities(requirement)
        if (entities.isNotEmpty()) {
            parameters["entities"] = entities.joinToString(",")
        }
        
        // Extract features
        val features = mutableListOf<String>()
        if (requirement.contains("rest api", ignoreCase = true)) features.add("REST APIs")
        if (requirement.contains("database", ignoreCase = true)) features.add("database integration")
        if (requirement.contains("authentication", ignoreCase = true)) features.add("authentication")
        if (requirement.contains("authorization", ignoreCase = true)) features.add("authorization")
        
        if (features.isNotEmpty()) {
            parameters["features"] = features.joinToString(",")
        }
    }
    
    private fun extractFormParameters(requirement: String, parameters: MutableMap<String, String>) {
        // Extract entity
        val entity = extractEntityName(requirement)
        if (entity != null) {
            parameters["entity"] = entity.replaceFirstChar { it.uppercase() } // Capitalize for forms
        }
        
        // Extract form type
        when {
            requirement.contains("registration", ignoreCase = true) -> parameters["formType"] = "registration"
            requirement.contains("login", ignoreCase = true) -> parameters["formType"] = "login"
            requirement.contains("contact", ignoreCase = true) -> parameters["formType"] = "contact"
            else -> parameters["formType"] = "form"
        }
        
        // Extract validations
        val validations = mutableListOf<String>()
        if (requirement.contains("email validation", ignoreCase = true)) validations.add("email validation")
        if (requirement.contains("password strength", ignoreCase = true)) validations.add("password strength")
        if (requirement.contains("required fields", ignoreCase = true)) validations.add("required fields")
        
        if (validations.isNotEmpty()) {
            parameters["validations"] = validations.joinToString(",")
        }
    }
    
    private fun extractDocumentationParameters(requirement: String, parameters: MutableMap<String, String>) {
        // Extract doc type
        when {
            requirement.contains("api", ignoreCase = true) -> parameters["docType"] = "API"
            requirement.contains("user", ignoreCase = true) -> parameters["docType"] = "User"
            requirement.contains("technical", ignoreCase = true) -> parameters["docType"] = "Technical"
            else -> parameters["docType"] = "General"
        }
        
        // Extract format
        when {
            requirement.contains("openapi", ignoreCase = true) -> parameters["format"] = "OpenAPI 3.0"
            requirement.contains("swagger", ignoreCase = true) -> parameters["format"] = "Swagger"
            requirement.contains("markdown", ignoreCase = true) -> parameters["format"] = "Markdown"
            else -> parameters["format"] = "Markdown"
        }
        
        // Extract scope
        when {
            requirement.contains("rest endpoints", ignoreCase = true) -> parameters["scope"] = "REST endpoints"
            requirement.contains("all endpoints", ignoreCase = true) -> parameters["scope"] = "all endpoints"
            else -> parameters["scope"] = "general"
        }
    }
    
    private fun extractRefactorParameters(requirement: String, parameters: MutableMap<String, String>) {
        // Extract target file/class
        val targetFile = extractTargetFile(requirement)
        if (targetFile != null) {
            parameters["targetFile"] = targetFile
        }
        
        val targetClass = extractTargetClass(requirement)
        if (targetClass != null) {
            parameters["targetClass"] = targetClass
        }
        
        // Extract operations
        val operations = mutableListOf<String>()
        if (requirement.contains("delete", ignoreCase = true)) operations.add("delete")
        if (requirement.contains("create", ignoreCase = true)) operations.add("create")
        if (requirement.contains("move", ignoreCase = true)) operations.add("move")
        if (requirement.contains("rename", ignoreCase = true)) operations.add("rename")
        
        if (operations.isNotEmpty()) {
            parameters["operations"] = operations.joinToString(",")
        }
        
        // Extract improvements
        val improvements = mutableListOf<String>()
        if (requirement.contains("better structure", ignoreCase = true)) improvements.add("better structure")
        if (requirement.contains("performance", ignoreCase = true)) improvements.add("performance")
        if (requirement.contains("readability", ignoreCase = true)) improvements.add("readability")
        
        if (improvements.isNotEmpty()) {
            parameters["improvements"] = improvements.joinToString(",")
        }
    }
    
    private fun extractOptimizationParameters(requirement: String, parameters: MutableMap<String, String>) {
        // Extract target class
        val targetClass = extractTargetClass(requirement)
        if (targetClass != null) {
            parameters["targetClass"] = targetClass
        }
        
        // Extract optimization target
        when {
            requirement.contains("database queries", ignoreCase = true) -> parameters["optimizationTarget"] = "database queries"
            requirement.contains("memory", ignoreCase = true) -> parameters["optimizationTarget"] = "memory usage"
            requirement.contains("cpu", ignoreCase = true) -> parameters["optimizationTarget"] = "CPU usage"
            else -> parameters["optimizationTarget"] = "performance"
        }
        
        // Extract performance goal
        val performanceGoal = extractPerformanceGoal(requirement)
        if (performanceGoal != null) {
            parameters["performanceGoal"] = performanceGoal
        }
    }
    
    private fun extractImprovementParameters(requirement: String, parameters: MutableMap<String, String>) {
        when {
            requirement.contains("make it better", ignoreCase = true) -> parameters["scope"] = "general"
            requirement.contains("improve", ignoreCase = true) -> parameters["scope"] = "improvement"
            else -> parameters["scope"] = "general"
        }
    }
    
    // Helper methods for extraction
    private fun extractClassName(requirement: String): String? {
        // Look for patterns like "User class", "Product entity", etc.
        val words = requirement.split(" ")

        // First try to find class name before keywords
        for (i in words.indices) {
            val word = words[i].lowercase()
            if (word in listOf("class", "entity", "model", "interface", "service")) {
                if (i > 0) {
                    val className = words[i - 1]
                    // Skip adjectives like "simple", "data", etc.
                    if (className.lowercase() !in listOf("simple", "data", "new", "basic")) {
                        return className.replaceFirstChar { it.uppercase() }
                    } else if (i > 1) {
                        // Look one word further back
                        val prevClassName = words[i - 2]
                        if (prevClassName.lowercase() !in listOf("a", "the", "create", "generate")) {
                            return prevClassName.replaceFirstChar { it.uppercase() }
                        }
                    }
                }
            }
        }

        // Look for common entity names that should be capitalized
        val commonEntities = listOf("user", "product", "order", "customer", "item", "account", "person")
        for (entity in commonEntities) {
            if (requirement.contains(entity, ignoreCase = true)) {
                return entity.replaceFirstChar { it.uppercase() }
            }
        }

        // Look for capitalized words that might be class names
        val capitalizedWords = words.filter {
            it.firstOrNull()?.isUpperCase() == true &&
            it.lowercase() !in listOf("create", "generate")
        }
        return capitalizedWords.firstOrNull()
    }
    
    private fun extractEntityName(requirement: String): String? {
        // Look for common entity patterns
        val entityPatterns = listOf("user", "product", "order", "customer", "item", "account")
        val requirementLower = requirement.lowercase()

        return entityPatterns.find { requirementLower.contains(it) } // Return lowercase
    }
    
    private fun extractProperties(requirement: String): List<String> {
        val properties = mutableListOf<String>()
        val commonProperties = listOf("id", "name", "email", "age", "price", "description", "title", "content")
        val requirementLower = requirement.lowercase()
        
        commonProperties.forEach { prop ->
            if (requirementLower.contains(prop)) {
                properties.add(prop)
            }
        }
        
        return properties
    }
    
    private fun extractTargetClass(requirement: String): String? {
        // Look for patterns like "UserService class", "UserController", etc.
        val classPattern = Regex("""(\w+(?:Service|Controller|Repository|Manager|Handler))""", RegexOption.IGNORE_CASE)
        return classPattern.find(requirement)?.value
    }
    
    private fun extractTargetFile(requirement: String): String? {
        // Look for file patterns like "UserService.kt"
        val filePattern = Regex("""(\w+\.\w+)""")
        return filePattern.find(requirement)?.value
    }
    
    private fun extractEntities(requirement: String): List<String> {
        val entities = mutableListOf<String>()
        val entityPatterns = listOf("User", "Product", "Order", "Customer", "Item", "Account", "Category")
        
        entityPatterns.forEach { entity ->
            if (requirement.contains(entity, ignoreCase = true)) {
                entities.add(entity)
            }
        }
        
        return entities
    }
    
    private fun extractPerformanceGoal(requirement: String): String? {
        // Look for performance goals like "1000+ concurrent users", "sub-second response"
        val goalPattern = Regex("""(\d+\+?\s*(?:concurrent\s+)?users?|\d+\s*(?:ms|seconds?)|sub-second)""", RegexOption.IGNORE_CASE)
        return goalPattern.find(requirement)?.value
    }
}
