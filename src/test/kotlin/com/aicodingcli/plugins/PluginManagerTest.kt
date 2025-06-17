package com.aicodingcli.plugins

import com.aicodingcli.ai.AiServiceFactory
import com.aicodingcli.config.ConfigManager
import com.aicodingcli.history.HistoryManager
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class PluginManagerTest {
    
    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var pluginManager: PluginManager
    private lateinit var configManager: ConfigManager
    private lateinit var historyManager: HistoryManager
    
    @BeforeEach
    fun setUp() {
        configManager = ConfigManager()
        historyManager = HistoryManager()
        pluginManager = PluginManager(
            pluginDir = tempDir.toString(),
            configManager = configManager,
            historyManager = historyManager,
            aiServiceFactory = AiServiceFactory
        )
    }
    
    @Test
    fun `should create plugin manager with correct configuration`() {
        assertNotNull(pluginManager)
        assertTrue(pluginManager.getLoadedPlugins().isEmpty())
        assertNotNull(pluginManager.getRegistry())
        assertNotNull(pluginManager.getEventDispatcher())
    }
    
    @Test
    fun `should validate plugin file correctly`() {
        // Test non-existent file
        val nonExistentResult = pluginManager.validatePlugin("/non/existent/file.jar")
        assertFalse(nonExistentResult.isValid)
        assertTrue(nonExistentResult.errors.any { it.contains("does not exist") })
        
        // Test non-JAR file
        val textFile = File(tempDir.toFile(), "test.txt")
        textFile.writeText("not a jar file")
        
        val nonJarResult = pluginManager.validatePlugin(textFile.absolutePath)
        assertFalse(nonJarResult.isValid)
        assertTrue(nonJarResult.errors.any { it.contains("must be a JAR file") })
    }
    
    @Test
    fun `should get plugin state correctly`() {
        assertNull(pluginManager.getPluginState("non-existent-plugin"))
    }
    
    @Test
    fun `should get plugin correctly`() {
        assertNull(pluginManager.getPlugin("non-existent-plugin"))
    }
    
    @Test
    fun `should handle plugin registry operations`() {
        val registry = pluginManager.getRegistry()
        
        // Test empty registry
        assertTrue(registry.getAllPlugins().isEmpty())
        assertTrue(registry.getAllCommands().isEmpty())
        assertTrue(registry.getCommandPlugins().isEmpty())
        assertTrue(registry.getAiServicePlugins().isEmpty())
        
        assertNull(registry.getPlugin("non-existent"))
        assertNull(registry.getCommandPlugin("non-existent"))
        assertNull(registry.getCommand("non-existent"))
        
        assertFalse(registry.hasCommand("non-existent"))
        
        val stats = registry.getStatistics()
        assertEquals(0, stats.totalPlugins)
        assertEquals(0, stats.commandPlugins)
        assertEquals(0, stats.aiServicePlugins)
        assertEquals(0, stats.totalCommands)
        assertTrue(stats.supportedAiProviders.isEmpty())
    }
    
    @Test
    fun `should handle event dispatcher operations`() {
        val eventDispatcher = pluginManager.getEventDispatcher()
        
        // Test empty dispatcher
        assertTrue(eventDispatcher.getHandlers(PluginEventType.PLUGIN_LOADED).isEmpty())
        assertTrue(eventDispatcher.getHandlers(PluginEventType.COMMAND_EXECUTED).isEmpty())
        
        // Test event dispatch (should not throw)
        runBlocking {
            val event = PluginEvent(
                type = PluginEventType.PLUGIN_LOADED,
                source = "test",
                data = mapOf("test" to "data")
            )
            
            eventDispatcher.dispatchEvent(event)
        }
    }
    
    @Test
    fun `should create plugin discovery service`() {
        val discoveryService = PluginDiscoveryService(tempDir.toString())
        
        // Test empty directory
        val plugins = discoveryService.discoverPlugins()
        assertTrue(plugins.isEmpty())
        
        // Test non-existent directory
        val nonExistentDiscovery = PluginDiscoveryService("/non/existent/directory")
        val emptyPlugins = nonExistentDiscovery.discoverPlugins()
        assertTrue(emptyPlugins.isEmpty())
        
        // Test getting plugin info for non-existent file
        val pluginInfo = discoveryService.getPluginInfo("/non/existent/plugin.jar")
        assertNull(pluginInfo)
    }
    
    @Test
    fun `should handle plugin validation result`() {
        val validResult = PluginValidationResult(
            isValid = true,
            errors = emptyList(),
            warnings = listOf("Warning message")
        )
        
        assertTrue(validResult.isValid)
        assertTrue(validResult.errors.isEmpty())
        assertEquals(1, validResult.warnings.size)
        assertEquals("Warning message", validResult.warnings[0])
        
        val invalidResult = PluginValidationResult(
            isValid = false,
            errors = listOf("Error message"),
            warnings = emptyList()
        )
        
        assertFalse(invalidResult.isValid)
        assertEquals(1, invalidResult.errors.size)
        assertEquals("Error message", invalidResult.errors[0])
        assertTrue(invalidResult.warnings.isEmpty())
    }
    
    @Test
    fun `should handle plugin exceptions`() {
        val loadException = PluginLoadException("Load failed")
        assertEquals("Load failed", loadException.message)
        assertTrue(loadException is Exception)
        
        val executionException = PluginExecutionException("Execution failed")
        assertEquals("Execution failed", executionException.message)
        assertTrue(executionException is Exception)
    }
    
    @Test
    fun `should handle plugin info`() {
        val metadata = PluginMetadata(
            id = "test-plugin",
            name = "Test Plugin",
            version = "1.0.0",
            description = "A test plugin",
            author = "Test Author",
            mainClass = "com.test.TestPlugin"
        )
        
        val pluginInfo = PluginInfo(
            metadata = metadata,
            filePath = "/path/to/plugin.jar",
            fileSize = 1024L,
            lastModified = System.currentTimeMillis()
        )
        
        assertEquals(metadata, pluginInfo.metadata)
        assertEquals("/path/to/plugin.jar", pluginInfo.filePath)
        assertEquals(1024L, pluginInfo.fileSize)
        assertTrue(pluginInfo.lastModified > 0)
    }
    
    @Test
    fun `should handle registry statistics`() {
        val stats = PluginRegistryStatistics(
            totalPlugins = 5,
            commandPlugins = 3,
            aiServicePlugins = 2,
            totalCommands = 10,
            supportedAiProviders = listOf(com.aicodingcli.ai.AiProvider.OPENAI, com.aicodingcli.ai.AiProvider.CLAUDE)
        )
        
        assertEquals(5, stats.totalPlugins)
        assertEquals(3, stats.commandPlugins)
        assertEquals(2, stats.aiServicePlugins)
        assertEquals(10, stats.totalCommands)
        assertEquals(2, stats.supportedAiProviders.size)
        assertTrue(stats.supportedAiProviders.contains(com.aicodingcli.ai.AiProvider.OPENAI))
        assertTrue(stats.supportedAiProviders.contains(com.aicodingcli.ai.AiProvider.CLAUDE))
    }
    
    @Test
    fun `should clear registry correctly`() {
        val registry = pluginManager.getRegistry()
        
        // Add some test data (this would normally be done through plugin loading)
        // For now, just test that clear doesn't throw
        registry.clear()
        
        assertTrue(registry.getAllPlugins().isEmpty())
        assertTrue(registry.getAllCommands().isEmpty())
        assertTrue(registry.getCommandPlugins().isEmpty())
        assertTrue(registry.getAiServicePlugins().isEmpty())
    }
    
    @Test
    fun `should handle plugin operations that are not yet implemented`() {
        runBlocking {
            // Test update plugin (not implemented)
            assertThrows(NotImplementedError::class.java) {
                runBlocking {
                    pluginManager.updatePlugin("test-plugin")
                }
            }
        }
    }
    
    @Test
    fun `should handle uninstall of non-existent plugin`() = runBlocking {
        val result = pluginManager.uninstallPlugin("non-existent-plugin")
        assertFalse(result)
    }
    
    @Test
    fun `should handle unload of non-existent plugin`() = runBlocking {
        val result = pluginManager.unloadPlugin("non-existent-plugin")
        assertFalse(result)
    }
}
