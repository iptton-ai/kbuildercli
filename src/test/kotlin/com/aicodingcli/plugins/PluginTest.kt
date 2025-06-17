package com.aicodingcli.plugins

import com.aicodingcli.ai.AiProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class PluginTest {
    
    private lateinit var testFramework: PluginTestFramework
    
    @BeforeEach
    fun setUp() {
        testFramework = PluginTestFramework()
    }
    
    @Test
    fun `should create plugin metadata correctly`() {
        val metadata = PluginMetadata(
            id = "test-plugin",
            name = "Test Plugin",
            version = "1.0.0",
            description = "A test plugin",
            author = "Test Author",
            mainClass = "com.test.TestPlugin"
        )
        
        assertEquals("test-plugin", metadata.id)
        assertEquals("Test Plugin", metadata.name)
        assertEquals("1.0.0", metadata.version)
        assertEquals("A test plugin", metadata.description)
        assertEquals("Test Author", metadata.author)
        assertEquals("com.test.TestPlugin", metadata.mainClass)
        assertTrue(metadata.dependencies.isEmpty())
        assertTrue(metadata.permissions.isEmpty())
    }
    
    @Test
    fun `should create plugin metadata with dependencies and permissions`() {
        val dependency = PluginDependency(
            id = "required-plugin",
            version = ">=1.0.0",
            optional = false
        )
        
        val permission = PluginPermission.FileSystemPermission(
            allowedPaths = listOf("/tmp"),
            readOnly = true
        )
        
        val metadata = PluginMetadata(
            id = "test-plugin",
            name = "Test Plugin",
            version = "1.0.0",
            description = "A test plugin",
            author = "Test Author",
            mainClass = "com.test.TestPlugin",
            dependencies = listOf(dependency),
            permissions = listOf(permission)
        )
        
        assertEquals(1, metadata.dependencies.size)
        assertEquals("required-plugin", metadata.dependencies[0].id)
        assertEquals(">=1.0.0", metadata.dependencies[0].version)
        assertFalse(metadata.dependencies[0].optional)
        
        assertEquals(1, metadata.permissions.size)
        assertTrue(metadata.permissions[0] is PluginPermission.FileSystemPermission)
        val fsPermission = metadata.permissions[0] as PluginPermission.FileSystemPermission
        assertEquals(listOf("/tmp"), fsPermission.allowedPaths)
        assertTrue(fsPermission.readOnly)
    }
    
    @Test
    fun `should create different types of permissions`() {
        val fsPermission = PluginPermission.FileSystemPermission(
            allowedPaths = listOf("/tmp", "/var/log"),
            readOnly = false
        )
        
        val networkPermission = PluginPermission.NetworkPermission(
            allowedHosts = listOf("api.example.com", "*.github.com")
        )
        
        val systemPermission = PluginPermission.SystemPermission(
            allowedCommands = listOf("git", "npm")
        )
        
        val configPermission = PluginPermission.ConfigPermission
        val historyPermission = PluginPermission.HistoryPermission
        
        assertTrue(fsPermission is PluginPermission.FileSystemPermission)
        assertTrue(networkPermission is PluginPermission.NetworkPermission)
        assertTrue(systemPermission is PluginPermission.SystemPermission)
        assertTrue(configPermission is PluginPermission.ConfigPermission)
        assertTrue(historyPermission is PluginPermission.HistoryPermission)
    }
    
    @Test
    fun `should create plugin context correctly`() {
        val metadata = PluginMetadata(
            id = "test-plugin",
            name = "Test Plugin",
            version = "1.0.0",
            description = "A test plugin",
            author = "Test Author",
            mainClass = "com.test.TestPlugin"
        )

        val context = testFramework.createTestContext(metadata)

        // Note: The mock implementations throw NotImplementedError for the interface methods
        // but the context itself should be created successfully
        assertNotNull(context.logger)
        assertTrue(context.hasPermission(PluginPermission.ConfigPermission))
    }
    
    @Test
    fun `should handle shared data in context`() {
        val context = testFramework.createTestContext()
        
        assertNull(context.getSharedData("test-key"))
        
        context.setSharedData("test-key", "test-value")
        assertEquals("test-value", context.getSharedData("test-key"))
        
        context.setSharedData("number-key", 42)
        assertEquals(42, context.getSharedData("number-key"))
    }
    
    @Test
    fun `should create plugin validation result`() {
        val validResult = PluginValidationResult(
            isValid = true,
            errors = emptyList(),
            warnings = listOf("Minor warning")
        )
        
        assertTrue(validResult.isValid)
        assertTrue(validResult.errors.isEmpty())
        assertEquals(1, validResult.warnings.size)
        assertEquals("Minor warning", validResult.warnings[0])
        
        val invalidResult = PluginValidationResult(
            isValid = false,
            errors = listOf("Critical error"),
            warnings = emptyList()
        )
        
        assertFalse(invalidResult.isValid)
        assertEquals(1, invalidResult.errors.size)
        assertEquals("Critical error", invalidResult.errors[0])
        assertTrue(invalidResult.warnings.isEmpty())
    }
    
    @Test
    fun `should create plugin load exception`() {
        val exception = PluginLoadException("Failed to load plugin")
        assertEquals("Failed to load plugin", exception.message)
        assertNull(exception.cause)
        
        val cause = RuntimeException("Root cause")
        val exceptionWithCause = PluginLoadException("Failed to load plugin", cause)
        assertEquals("Failed to load plugin", exceptionWithCause.message)
        assertEquals(cause, exceptionWithCause.cause)
    }
    
    @Test
    fun `should create plugin execution exception`() {
        val exception = PluginExecutionException("Failed to execute plugin")
        assertEquals("Failed to execute plugin", exception.message)
        assertNull(exception.cause)
        
        val cause = RuntimeException("Root cause")
        val exceptionWithCause = PluginExecutionException("Failed to execute plugin", cause)
        assertEquals("Failed to execute plugin", exceptionWithCause.message)
        assertEquals(cause, exceptionWithCause.cause)
    }
    
    @Test
    fun `should handle plugin states`() {
        val states = PluginState.values()
        
        assertTrue(states.contains(PluginState.UNLOADED))
        assertTrue(states.contains(PluginState.LOADED))
        assertTrue(states.contains(PluginState.INITIALIZED))
        assertTrue(states.contains(PluginState.RUNNING))
        assertTrue(states.contains(PluginState.STOPPED))
        assertTrue(states.contains(PluginState.ERROR))
    }
}
