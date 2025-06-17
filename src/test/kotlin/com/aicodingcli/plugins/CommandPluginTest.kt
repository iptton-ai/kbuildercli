package com.aicodingcli.plugins

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

class CommandPluginTest {
    
    private lateinit var testFramework: PluginTestFramework
    private lateinit var testPlugin: TestCommandPlugin
    
    @BeforeEach
    fun setUp() {
        testFramework = PluginTestFramework()
        testPlugin = TestCommandPlugin()
    }
    
    @Test
    fun `should create command plugin with correct metadata`() {
        assertEquals("test-command-plugin", testPlugin.metadata.id)
        assertEquals("Test Command Plugin", testPlugin.metadata.name)
        assertEquals("1.0.0", testPlugin.metadata.version)
        assertEquals("A test command plugin", testPlugin.metadata.description)
        assertEquals("Test Author", testPlugin.metadata.author)
    }
    
    @Test
    fun `should provide commands`() {
        assertEquals(2, testPlugin.commands.size)
        
        val helloCommand = testPlugin.getCommand("hello")
        assertNotNull(helloCommand)
        assertEquals("hello", helloCommand?.name)
        assertEquals("Say hello", helloCommand?.description)
        
        val echoCommand = testPlugin.getCommand("echo")
        assertNotNull(echoCommand)
        assertEquals("echo", echoCommand?.name)
        assertEquals("Echo a message", echoCommand?.description)
    }
    
    @Test
    fun `should check if command exists`() {
        assertTrue(testPlugin.hasCommand("hello"))
        assertTrue(testPlugin.hasCommand("echo"))
        assertFalse(testPlugin.hasCommand("nonexistent"))
    }
    
    @Test
    fun `should initialize and register commands`() {
        val context = testFramework.createTestContext(testPlugin.metadata) as TestPluginContext
        
        testPlugin.initialize(context)
        
        val registeredCommands = context.getRegisteredCommands()
        assertEquals(2, registeredCommands.size)
        
        val commandNames = registeredCommands.map { it.name }
        assertTrue(commandNames.contains("hello"))
        assertTrue(commandNames.contains("echo"))
    }
    
    @Test
    fun `should execute hello command successfully`() = runBlocking {
        val result = testFramework.simulateCommand(testPlugin, "hello")
        
        assertTrue(result.success)
        assertEquals("Hello, World!", result.message)
        assertEquals(0, result.exitCode)
    }
    
    @Test
    fun `should execute echo command with arguments`() = runBlocking {
        val result = testFramework.simulateCommand(
            testPlugin, 
            "echo", 
            arrayOf("test", "message")
        )
        
        assertTrue(result.success)
        assertEquals("Echo: test message", result.message)
        assertEquals(0, result.exitCode)
    }
    
    @Test
    fun `should execute echo command with options`() = runBlocking {
        val result = testFramework.simulateCommand(
            testPlugin, 
            "echo", 
            arrayOf("test"),
            mapOf("uppercase" to "true")
        )
        
        assertTrue(result.success)
        assertEquals("Echo: TEST", result.message)
        assertEquals(0, result.exitCode)
    }
    
    @Test
    fun `should handle command not found`() = runBlocking {
        val result = testFramework.simulateCommand(testPlugin, "nonexistent")
        
        assertFalse(result.success)
        assertEquals("Command not found: nonexistent", result.message)
        assertEquals(1, result.exitCode)
    }
    
    @Test
    fun `should shutdown plugin correctly`() {
        val context = testFramework.createTestContext(testPlugin.metadata)
        
        testPlugin.initialize(context)
        // Note: isInitialized() is protected, so we can't test it directly
        // Instead, we test that shutdown doesn't throw an exception
        testPlugin.shutdown()
    }
    
    @Test
    fun `should validate plugin successfully`() {
        val result = testFramework.testPluginValidation(testPlugin)
        
        assertTrue(result.success)
        assertTrue(result.errors.isEmpty())
    }
    
    @Test
    fun `should create command result helpers`() {
        val successResult = CommandResult.success("Success message", "data")
        assertTrue(successResult.success)
        assertEquals("Success message", successResult.message)
        assertEquals("data", successResult.data)
        assertEquals(0, successResult.exitCode)
        
        val failureResult = CommandResult.failure("Failure message", 2)
        assertFalse(failureResult.success)
        assertEquals("Failure message", failureResult.message)
        assertNull(failureResult.data)
        assertEquals(2, failureResult.exitCode)
        
        val errorResult = CommandResult.error("Error message")
        assertFalse(errorResult.success)
        assertEquals("Error message", errorResult.message)
        assertNull(errorResult.data)
        assertEquals(1, errorResult.exitCode)
        
        val exception = RuntimeException("Test exception")
        val errorWithExceptionResult = CommandResult.error("Error occurred", exception)
        assertFalse(errorWithExceptionResult.success)
        assertEquals("Error occurred: Test exception", errorWithExceptionResult.message)
        assertEquals(exception, errorWithExceptionResult.data)
        assertEquals(1, errorWithExceptionResult.exitCode)
    }
    
    @Test
    fun `should handle command arguments correctly`() {
        val args = CommandArgs(
            args = listOf("arg1", "arg2", "arg3"),
            options = mapOf("option1" to "value1", "flag" to null),
            rawArgs = arrayOf("arg1", "arg2", "arg3", "--option1", "value1", "--flag")
        )
        
        assertEquals("arg1", args.getArg(0))
        assertEquals("arg2", args.getArg(1))
        assertEquals("arg3", args.getArg(2))
        assertNull(args.getArg(3))
        
        assertEquals("value1", args.getOption("option1"))
        assertNull(args.getOption("flag"))
        assertNull(args.getOption("nonexistent"))
        
        assertTrue(args.hasOption("option1"))
        assertTrue(args.hasOption("flag"))
        assertFalse(args.hasOption("nonexistent"))
        
        assertEquals("value1", args.getOptionOrDefault("option1", "default"))
        assertEquals("default", args.getOptionOrDefault("nonexistent", "default"))
    }
    
    @Test
    fun `should create command options correctly`() {
        val option = CommandOption(
            name = "output",
            shortName = "o",
            description = "Output format",
            required = true,
            hasValue = true,
            defaultValue = "text"
        )
        
        assertEquals("output", option.name)
        assertEquals("o", option.shortName)
        assertEquals("Output format", option.description)
        assertTrue(option.required)
        assertTrue(option.hasValue)
        assertEquals("text", option.defaultValue)
    }
}

/**
 * Test implementation of CommandPlugin for testing purposes
 */
class TestCommandPlugin : BaseCommandPlugin() {
    
    override val metadata = PluginMetadata(
        id = "test-command-plugin",
        name = "Test Command Plugin",
        version = "1.0.0",
        description = "A test command plugin",
        author = "Test Author",
        mainClass = "com.aicodingcli.plugins.TestCommandPlugin"
    )
    
    override val commands = listOf(
        createCommand(
            name = "hello",
            description = "Say hello",
            usage = "hello"
        ) { _, _ ->
            CommandResult.success("Hello, World!")
        },
        
        createCommand(
            name = "echo",
            description = "Echo a message",
            usage = "echo <message> [--uppercase]",
            options = listOf(
                CommandOption(
                    name = "uppercase",
                    shortName = "u",
                    description = "Convert to uppercase",
                    required = false,
                    hasValue = false
                )
            )
        ) { args, _ ->
            val message = args.args.joinToString(" ")
            val uppercase = args.hasOption("uppercase")
            
            val result = if (uppercase) {
                "Echo: ${message.uppercase()}"
            } else {
                "Echo: $message"
            }
            
            CommandResult.success(result)
        }
    )
}
