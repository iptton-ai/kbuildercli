package com.aicodingcli.conversation

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class RequirementParserTest {

    private lateinit var requirementParser: RequirementParser

    @BeforeEach
    fun setUp() {
        requirementParser = DefaultRequirementParser()
    }

    @Test
    fun `should parse simple class creation requirement`() = runTest {
        // Arrange
        val requirement = "Create a simple User data class with name and email properties"

        // Act
        val parsed = requirementParser.parse(requirement)

        // Assert
        assertNotNull(parsed)
        assertEquals(Intent.CREATE_CLASS, parsed.intent)
        assertEquals("User", parsed.parameters["className"])
        assertEquals("data", parsed.parameters["classType"])
        assertTrue(parsed.parameters["properties"]?.contains("name") == true)
        assertTrue(parsed.parameters["properties"]?.contains("email") == true)
        assertEquals("kotlin", parsed.parameters["language"])
    }

    @Test
    fun `should parse REST API requirement`() = runTest {
        // Arrange
        val requirement = "Create a REST API endpoint for user management with CRUD operations"

        // Act
        val parsed = requirementParser.parse(requirement)

        // Assert
        assertNotNull(parsed)
        assertEquals(Intent.CREATE_API, parsed.intent)
        assertEquals("user", parsed.parameters["entity"])
        assertTrue(parsed.parameters["operations"]?.contains("CRUD") == true)
        assertEquals("REST", parsed.parameters["apiType"])
    }

    @Test
    fun `should parse configuration requirement`() = runTest {
        // Arrange
        val requirement = "Create a database configuration file with connection settings"

        // Act
        val parsed = requirementParser.parse(requirement)

        // Assert
        assertNotNull(parsed)
        assertEquals(Intent.CREATE_CONFIG, parsed.intent)
        assertEquals("database", parsed.parameters["configType"])
        assertTrue(parsed.parameters["settings"]?.contains("connection") == true)
    }

    @Test
    fun `should parse test creation requirement`() = runTest {
        // Arrange
        val requirement = "Generate unit tests for the UserService class with all methods covered"

        // Act
        val parsed = requirementParser.parse(requirement)

        // Assert
        assertNotNull(parsed)
        assertEquals(Intent.CREATE_TESTS, parsed.intent)
        assertEquals("UserService", parsed.parameters["targetClass"])
        assertEquals("unit", parsed.parameters["testType"])
        assertTrue(parsed.parameters["coverage"]?.contains("all") == true)
    }

    @Test
    fun `should extract intent from simple requirement`() = runTest {
        // Arrange
        val requirement = "Create a new service class"

        // Act
        val intent = requirementParser.extractIntent(requirement)

        // Assert
        assertEquals(Intent.CREATE_CLASS, intent)
    }

    @Test
    fun `should extract intent from API requirement`() = runTest {
        // Arrange
        val requirement = "Build REST endpoints for product management"

        // Act
        val intent = requirementParser.extractIntent(requirement)

        // Assert
        assertEquals(Intent.CREATE_API, intent)
    }

    @Test
    fun `should extract intent from refactor requirement`() = runTest {
        // Arrange
        val requirement = "Refactor the existing UserController to improve performance"

        // Act
        val intent = requirementParser.extractIntent(requirement)

        // Assert
        assertEquals(Intent.REFACTOR_CODE, intent)
    }

    @Test
    fun `should extract parameters for class creation`() = runTest {
        // Arrange
        val requirement = "Create a Product entity class with id, name, price, and description fields"
        val intent = Intent.CREATE_CLASS

        // Act
        val parameters = requirementParser.extractParameters(requirement, intent)

        // Assert
        assertEquals("Product", parameters["className"])
        assertEquals("entity", parameters["classType"])
        val properties = parameters["properties"]
        assertTrue(properties?.contains("id") == true)
        assertTrue(properties?.contains("name") == true)
        assertTrue(properties?.contains("price") == true)
        assertTrue(properties?.contains("description") == true)
    }

    @Test
    fun `should extract parameters for API creation`() = runTest {
        // Arrange
        val requirement = "Create GraphQL API for order management with mutations and queries"
        val intent = Intent.CREATE_API

        // Act
        val parameters = requirementParser.extractParameters(requirement, intent)

        // Assert
        assertEquals("order", parameters["entity"])
        assertEquals("GraphQL", parameters["apiType"])
        assertTrue(parameters["operations"]?.contains("mutations") == true)
        assertTrue(parameters["operations"]?.contains("queries") == true)
    }

    @Test
    fun `should handle complex requirement with multiple entities`() = runTest {
        // Arrange
        val requirement = "Create a complete e-commerce system with User, Product, and Order entities, including REST APIs and database integration"

        // Act
        val parsed = requirementParser.parse(requirement)

        // Assert
        assertNotNull(parsed)
        assertEquals(Intent.CREATE_SYSTEM, parsed.intent)
        val entities = parsed.parameters["entities"]
        assertTrue(entities?.contains("User") == true)
        assertTrue(entities?.contains("Product") == true)
        assertTrue(entities?.contains("Order") == true)
        assertTrue(parsed.parameters["features"]?.contains("REST APIs") == true)
        assertTrue(parsed.parameters["features"]?.contains("database integration") == true)
    }

    @Test
    fun `should parse requirement with specific technology stack`() = runTest {
        // Arrange
        val requirement = "Create a Spring Boot microservice with JPA entities and PostgreSQL database"

        // Act
        val parsed = requirementParser.parse(requirement)

        // Assert
        assertNotNull(parsed)
        assertEquals(Intent.CREATE_SERVICE, parsed.intent)
        assertEquals("Spring Boot", parsed.parameters["framework"])
        assertEquals("microservice", parsed.parameters["serviceType"])
        assertTrue(parsed.parameters["technologies"]?.contains("JPA") == true)
        assertTrue(parsed.parameters["technologies"]?.contains("PostgreSQL") == true)
    }

    @Test
    fun `should handle vague requirement gracefully`() = runTest {
        // Arrange
        val requirement = "Make it better"

        // Act
        val parsed = requirementParser.parse(requirement)

        // Assert
        assertNotNull(parsed)
        assertEquals(Intent.IMPROVE_CODE, parsed.intent)
        assertTrue(parsed.parameters.isEmpty() || parsed.parameters["scope"] == "general")
    }

    @Test
    fun `should parse requirement with validation constraints`() = runTest {
        // Arrange
        val requirement = "Create a User registration form with email validation, password strength check, and required fields"

        // Act
        val parsed = requirementParser.parse(requirement)

        // Assert
        assertNotNull(parsed)
        assertEquals(Intent.CREATE_FORM, parsed.intent)
        assertEquals("User", parsed.parameters["entity"])
        assertEquals("registration", parsed.parameters["formType"])
        val validations = parsed.parameters["validations"]
        assertTrue(validations?.contains("email validation") == true)
        assertTrue(validations?.contains("password strength") == true)
        assertTrue(validations?.contains("required fields") == true)
    }

    @Test
    fun `should extract file operations from requirement`() = runTest {
        // Arrange
        val requirement = "Delete the old UserService.kt file and create a new one with better structure"

        // Act
        val parsed = requirementParser.parse(requirement)

        // Assert
        assertNotNull(parsed)
        assertEquals(Intent.REFACTOR_CODE, parsed.intent)
        assertEquals("UserService.kt", parsed.parameters["targetFile"])
        assertTrue(parsed.parameters["operations"]?.contains("delete") == true)
        assertTrue(parsed.parameters["operations"]?.contains("create") == true)
        assertTrue(parsed.parameters["improvements"]?.contains("better structure") == true)
    }

    @Test
    fun `should parse requirement with specific output format`() = runTest {
        // Arrange
        val requirement = "Generate API documentation in OpenAPI 3.0 format for all REST endpoints"

        // Act
        val parsed = requirementParser.parse(requirement)

        // Assert
        assertNotNull(parsed)
        assertEquals(Intent.CREATE_DOCUMENTATION, parsed.intent)
        assertEquals("API", parsed.parameters["docType"])
        assertEquals("OpenAPI 3.0", parsed.parameters["format"])
        assertEquals("REST endpoints", parsed.parameters["scope"])
    }

    @Test
    fun `should handle empty requirement`() = runTest {
        // Arrange
        val requirement = ""

        // Act
        val parsed = requirementParser.parse(requirement)

        // Assert
        assertNotNull(parsed)
        assertEquals(Intent.UNKNOWN, parsed.intent)
        assertTrue(parsed.parameters.isEmpty())
    }

    @Test
    fun `should parse requirement with performance requirements`() = runTest {
        // Arrange
        val requirement = "Optimize the database queries in UserRepository to handle 1000+ concurrent users"

        // Act
        val parsed = requirementParser.parse(requirement)

        // Assert
        assertNotNull(parsed)
        assertEquals(Intent.OPTIMIZE_PERFORMANCE, parsed.intent)
        assertEquals("UserRepository", parsed.parameters["targetClass"])
        assertEquals("database queries", parsed.parameters["optimizationTarget"])
        assertEquals("1000+ concurrent users", parsed.parameters["performanceGoal"])
    }
}
