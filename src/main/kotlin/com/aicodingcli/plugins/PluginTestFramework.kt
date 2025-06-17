package com.aicodingcli.plugins

import com.aicodingcli.ai.*
import com.aicodingcli.config.ConfigManager
import com.aicodingcli.history.HistoryManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.util.concurrent.ConcurrentHashMap

/**
 * Test framework for plugin development and testing
 */
class PluginTestFramework {
    
    /**
     * Create a test plugin context with mock services
     */
    fun createTestContext(
        pluginMetadata: PluginMetadata = createTestPluginMetadata(),
        enableDebugLogging: Boolean = true
    ): TestPluginContext {
        val mockConfigManager = MockConfigManager()
        val mockHistoryManager = MockHistoryManager()
        val mockAiServiceFactory = MockAiServiceFactory()
        val logger = DefaultPluginLogger(pluginMetadata.id, enableDebugLogging)

        return TestPluginContext(
            mockConfigManager = mockConfigManager,
            mockHistoryManager = mockHistoryManager,
            mockAiServiceFactory = mockAiServiceFactory,
            logger = logger,
            pluginMetadata = pluginMetadata
        )
    }
    
    /**
     * Create a mock AI service for testing
     */
    fun mockAiService(
        provider: AiProvider,
        responses: List<String> = listOf("Mock response"),
        shouldFail: Boolean = false
    ): AiService {
        return MockAiService(provider, responses, shouldFail)
    }
    
    /**
     * Simulate command execution for testing
     */
    suspend fun simulateCommand(
        plugin: CommandPlugin,
        commandName: String,
        args: Array<String> = emptyArray(),
        options: Map<String, String> = emptyMap()
    ): CommandResult {
        val context = createTestContext(plugin.metadata)
        plugin.initialize(context)
        
        val command = plugin.getCommand(commandName)
            ?: return CommandResult.failure("Command not found: $commandName")
        
        val commandArgs = CommandArgs(
            args = args.toList(),
            options = options,
            rawArgs = args
        )
        
        return command.handler(commandArgs, context)
    }
    
    /**
     * Test plugin loading and validation
     */
    fun testPluginValidation(plugin: Plugin): PluginTestResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            // Test metadata
            if (plugin.metadata.id.isBlank()) {
                errors.add("Plugin ID cannot be blank")
            }
            
            if (plugin.metadata.name.isBlank()) {
                errors.add("Plugin name cannot be blank")
            }
            
            if (plugin.metadata.version.isBlank()) {
                errors.add("Plugin version cannot be blank")
            }
            
            // Test initialization
            val context = createTestContext(plugin.metadata)
            plugin.initialize(context)
            
            // Test plugin-specific functionality
            when (plugin) {
                is CommandPlugin -> {
                    if (plugin.commands.isEmpty()) {
                        warnings.add("Command plugin has no commands")
                    }
                    
                    plugin.commands.forEach { command ->
                        if (command.name.isBlank()) {
                            errors.add("Command name cannot be blank")
                        }
                        if (command.description.isBlank()) {
                            warnings.add("Command '${command.name}' has no description")
                        }
                    }
                }
                
                is AiServicePlugin -> {
                    try {
                        val testConfig = AiServiceConfig(
                            provider = plugin.supportedProvider,
                            apiKey = "test-key",
                            model = "test-model",
                            baseUrl = "https://test.example.com"
                        )
                        plugin.createAiService(testConfig)
                    } catch (e: Exception) {
                        errors.add("Failed to create AI service: ${e.message}")
                    }
                }
            }
            
            // Test shutdown
            plugin.shutdown()
            
        } catch (e: Exception) {
            errors.add("Plugin test failed: ${e.message}")
        }
        
        return PluginTestResult(
            success = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Create test plugin metadata
     */
    private fun createTestPluginMetadata(): PluginMetadata {
        return PluginMetadata(
            id = "test-plugin",
            name = "Test Plugin",
            version = "1.0.0",
            description = "Test plugin for framework testing",
            author = "Test Framework",
            mainClass = "com.test.TestPlugin"
        )
    }
}

/**
 * Test plugin context implementation
 */
class TestPluginContext(
    private val mockConfigManager: MockConfigManager,
    private val mockHistoryManager: MockHistoryManager,
    private val mockAiServiceFactory: MockAiServiceFactory,
    override val logger: PluginLogger,
    private val pluginMetadata: PluginMetadata
) : PluginContext {

    override val configManager: ConfigManager get() = throw NotImplementedError("Use mock methods directly")
    override val historyManager: HistoryManager get() = throw NotImplementedError("Use mock methods directly")
    override val aiServiceFactory: AiServiceFactory get() = throw NotImplementedError("Use mock methods directly")
    
    private val registeredCommands = mutableListOf<PluginCommand>()
    private val registeredEventHandlers = mutableListOf<PluginEventHandler>()
    private val sharedData = ConcurrentHashMap<String, Any>()
    
    override fun registerCommand(command: Any) {
        val pluginCommand = command as PluginCommand
        registeredCommands.add(pluginCommand)
        logger.debug("Registered test command: ${pluginCommand.name}")
    }
    
    override fun registerEventHandler(handler: Any) {
        val eventHandler = handler as PluginEventHandler
        registeredEventHandlers.add(eventHandler)
        logger.debug("Registered test event handler for: ${eventHandler.eventTypes}")
    }
    
    override fun getSharedData(key: String): Any? = sharedData[key]
    
    override fun setSharedData(key: String, value: Any) {
        sharedData[key] = value
    }
    
    override fun hasPermission(permission: PluginPermission): Boolean = true
    
    fun getRegisteredCommands(): List<PluginCommand> = registeredCommands.toList()
    fun getRegisteredEventHandlers(): List<PluginEventHandler> = registeredEventHandlers.toList()
}

/**
 * Mock configuration manager for testing
 */
class MockConfigManager {
    private val mockConfig = com.aicodingcli.config.AppConfig(
        defaultProvider = AiProvider.OPENAI,
        providers = mapOf(
            AiProvider.OPENAI to AiServiceConfig(
                provider = AiProvider.OPENAI,
                apiKey = "test-key",
                model = "gpt-3.5-turbo",
                baseUrl = "https://api.openai.com/v1"
            )
        )
    )

    suspend fun loadConfig() = mockConfig
    suspend fun saveConfig(config: com.aicodingcli.config.AppConfig) {}
    suspend fun getCurrentProviderConfig() = mockConfig.providers[AiProvider.OPENAI]!!
}

/**
 * Mock history manager for testing
 */
class MockHistoryManager {
    fun createConversation(
        title: String,
        provider: AiProvider,
        model: String
    ) = com.aicodingcli.history.ConversationSession(
        title = title,
        provider = provider,
        model = model
    )
}

/**
 * Mock AI service factory for testing
 */
class MockAiServiceFactory {
    fun createService(config: AiServiceConfig): AiService {
        return MockAiService(config.provider)
    }
}

/**
 * Mock AI service for testing
 */
class MockAiService(
    private val provider: AiProvider,
    private val responses: List<String> = listOf("Mock response"),
    private val shouldFail: Boolean = false
) : AiService {
    
    override val config = AiServiceConfig(
        provider = provider,
        apiKey = "test-key",
        model = "test-model",
        baseUrl = "https://test.example.com"
    )
    
    override suspend fun chat(request: AiRequest): AiResponse {
        if (shouldFail) {
            throw RuntimeException("Mock AI service failure")
        }
        
        return AiResponse(
            content = responses.firstOrNull() ?: "Mock response",
            model = request.model,
            usage = TokenUsage(10, 20, 30),
            finishReason = FinishReason.STOP
        )
    }
    
    override suspend fun streamChat(request: AiRequest): Flow<AiStreamChunk> {
        if (shouldFail) {
            throw RuntimeException("Mock AI service failure")
        }
        
        return flowOf(
            AiStreamChunk("Mock ", null),
            AiStreamChunk("streaming ", null),
            AiStreamChunk("response", FinishReason.STOP)
        )
    }
    
    override suspend fun testConnection(): Boolean = !shouldFail
}

/**
 * Result of plugin testing
 */
data class PluginTestResult(
    val success: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)
