package com.aicodingcli

import com.aicodingcli.ai.*
import com.aicodingcli.config.ConfigManager
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) {
    val cli = AiCodingCli()
    cli.run(args)
}

class AiCodingCli {
    companion object {
        const val VERSION = "0.1.0"
        const val HELP_TEXT = """AI Coding CLI - A command line tool for AI-assisted coding

Usage: ai-coding-cli [COMMAND] [OPTIONS]

Commands:
  test-connection    Test connection to AI service
  ask <message>      Ask AI a question
  config <subcommand> Manage configuration settings

Options:
  --version          Show version information
  --help             Show this help message
  --provider <name>  Use specific AI provider (openai, claude, ollama)
  --model <name>     Use specific model for the AI provider"""
    }

    private val configManager = ConfigManager()

    fun run(args: Array<String>) {
        val (command, options) = parseArgs(args)

        when {
            args.isEmpty() -> printHelp()
            command == "--version" -> printVersion()
            command == "--help" -> printHelp()
            command == "test-connection" -> testConnection(options.provider, options.model)
            command == "ask" && options.message.isNotEmpty() -> askQuestion(options.message, options.provider, options.model)
            command == "config" -> handleConfigCommand(args.drop(1).toTypedArray())
            else -> {
                println("Unknown command: $command")
                printHelp()
            }
        }
    }

    private data class CommandOptions(
        val provider: AiProvider? = null,
        val model: String? = null,
        val message: String = ""
    )

    private fun parseArgs(args: Array<String>): Pair<String, CommandOptions> {
        if (args.isEmpty()) return "" to CommandOptions()

        var command = args[0]
        var provider: AiProvider? = null
        var model: String? = null
        var message = ""

        var i = 1
        while (i < args.size) {
            when (args[i]) {
                "--provider" -> {
                    if (i + 1 < args.size) {
                        provider = when (args[i + 1].lowercase()) {
                            "openai" -> AiProvider.OPENAI
                            "claude" -> AiProvider.CLAUDE
                            "ollama" -> AiProvider.OLLAMA
                            else -> {
                                println("Unknown provider: ${args[i + 1]}")
                                return command to CommandOptions()
                            }
                        }
                        i += 2
                    } else {
                        println("--provider requires a value")
                        return command to CommandOptions()
                    }
                }
                "--model" -> {
                    if (i + 1 < args.size) {
                        model = args[i + 1]
                        i += 2
                    } else {
                        println("--model requires a value")
                        return command to CommandOptions()
                    }
                }
                else -> {
                    if (command == "ask") {
                        message = args.drop(i).joinToString(" ")
                        break
                    }
                    i++
                }
            }
        }

        return command to CommandOptions(provider, model, message)
    }

    private fun printVersion() {
        println(VERSION)
    }

    private fun printHelp() {
        println(HELP_TEXT)
    }

    private fun testConnection(provider: AiProvider? = null, model: String? = null) {
        runBlocking {
            try {
                val config = if (provider != null) {
                    var providerConfig = getProviderConfig(provider)
                    if (model != null) {
                        providerConfig = providerConfig.copy(model = model)
                    }
                    providerConfig
                } else {
                    configManager.getCurrentProviderConfig()
                }
                val service = AiServiceFactory.createService(config)
                val result = service.testConnection()

                if (result) {
                    println("✅ Connection to ${config.provider} successful!")
                    if (model != null) {
                        println("   Using model: $model")
                    }
                } else {
                    println("❌ Connection to ${config.provider} failed!")
                }
            } catch (e: Exception) {
                println("❌ Error testing connection: ${e.message}")
            }
        }
    }

    private fun askQuestion(question: String, provider: AiProvider? = null, model: String? = null) {
        runBlocking {
            try {
                val config = if (provider != null) {
                    var providerConfig = getProviderConfig(provider)
                    if (model != null) {
                        providerConfig = providerConfig.copy(model = model)
                    }
                    providerConfig
                } else {
                    configManager.getCurrentProviderConfig()
                }
                val service = AiServiceFactory.createService(config)

                val request = AiRequest(
                    messages = listOf(
                        AiMessage(role = MessageRole.USER, content = question)
                    ),
                    model = config.model,
                    temperature = config.temperature,
                    maxTokens = config.maxTokens
                )

                println("🤖 Asking ${config.provider}...")
                if (model != null) {
                    println("   Using model: $model")
                }
                val response = service.chat(request)
                println("\n${response.content}")
                println("\n📊 Usage: ${response.usage.totalTokens} tokens")

            } catch (e: Exception) {
                println("❌ Error asking question: ${e.message}")
            }
        }
    }

    private suspend fun getProviderConfig(provider: AiProvider): AiServiceConfig {
        val config = configManager.loadConfig()
        return config.providers[provider]
            ?: createDefaultProviderConfig(provider)
    }

    private fun createDefaultProviderConfig(provider: AiProvider): AiServiceConfig {
        return when (provider) {
            AiProvider.OPENAI -> AiServiceConfig(
                provider = AiProvider.OPENAI,
                apiKey = "your-openai-api-key",
                model = "gpt-3.5-turbo",
                baseUrl = "https://api.openai.com/v1",
                temperature = 0.7f,
                maxTokens = 1000
            )
            AiProvider.CLAUDE -> AiServiceConfig(
                provider = AiProvider.CLAUDE,
                apiKey = "your-claude-api-key",
                model = "claude-3-sonnet-20240229",
                baseUrl = "https://api.anthropic.com/v1",
                temperature = 0.7f,
                maxTokens = 1000
            )

            AiProvider.OLLAMA -> AiServiceConfig(
                provider = AiProvider.OLLAMA,
                apiKey = "not-required", // Ollama doesn't require API key but config validation needs non-empty string
                model = "llama2",
                baseUrl = "http://localhost:11434",
                temperature = 0.7f,
                maxTokens = 1000
            )
        }
    }

    private fun handleConfigCommand(args: Array<String>) {
        if (args.isEmpty()) {
            printConfigHelp()
            return
        }

        when (args[0]) {
            "set" -> handleConfigSet(args.drop(1).toTypedArray())
            "get" -> handleConfigGet(args.drop(1).toTypedArray())
            "list" -> handleConfigList()
            "provider" -> handleConfigProvider(args.drop(1).toTypedArray())
            else -> {
                println("Unknown config subcommand: ${args[0]}")
                printConfigHelp()
            }
        }
    }

    private fun printConfigHelp() {
        println("""
            Configuration Management Commands:

            config set <key> <value>    Set a configuration value
            config get <key>            Get a configuration value
            config list                 List all configuration
            config provider <name>      Set default AI provider

            Examples:
            config set openai.api_key sk-...
            config set claude.api_key sk-ant-...
            config get openai.api_key
            config provider ollama
            config list
        """.trimIndent())
    }

    private fun handleConfigSet(args: Array<String>) {
        if (args.size < 2) {
            println("Usage: config set <key> <value>")
            return
        }

        val key = args[0]
        val value = args.drop(1).joinToString(" ")

        runBlocking {
            try {
                setConfigValue(key, value)
                println("✅ Configuration updated: $key")
            } catch (e: Exception) {
                println("❌ Error setting configuration: ${e.message}")
            }
        }
    }

    private fun handleConfigGet(args: Array<String>) {
        if (args.isEmpty()) {
            println("Usage: config get <key>")
            return
        }

        val key = args[0]

        runBlocking {
            try {
                val value = getConfigValue(key)
                if (value != null) {
                    // Mask sensitive values
                    val displayValue = if (key.contains("api_key") || key.contains("apikey")) {
                        maskApiKey(value)
                    } else {
                        value
                    }
                    println("$key = $displayValue")
                } else {
                    println("Configuration key '$key' not found")
                }
            } catch (e: Exception) {
                println("❌ Error getting configuration: ${e.message}")
            }
        }
    }

    private fun handleConfigList() {
        runBlocking {
            try {
                val config = configManager.loadConfig()
                println("Current Configuration:")
                println("Default Provider: ${config.defaultProvider}")
                println()

                config.providers.forEach { (provider, providerConfig) ->
                    println("[$provider]")
                    println("  api_key = ${maskApiKey(providerConfig.apiKey)}")
                    println("  model = ${providerConfig.model}")
                    println("  base_url = ${providerConfig.baseUrl ?: "default"}")
                    println("  temperature = ${providerConfig.temperature}")
                    println("  max_tokens = ${providerConfig.maxTokens}")
                    println()
                }
            } catch (e: Exception) {
                println("❌ Error listing configuration: ${e.message}")
            }
        }
    }

    private fun handleConfigProvider(args: Array<String>) {
        if (args.isEmpty()) {
            println("Usage: config provider <name>")
            println("Available providers: openai, claude, ollama")
            return
        }

        val providerName = args[0].lowercase()
        val provider = when (providerName) {
            "openai" -> AiProvider.OPENAI
            "claude" -> AiProvider.CLAUDE
            "ollama" -> AiProvider.OLLAMA
            else -> {
                println("Unknown provider: $providerName")
                println("Available providers: openai, claude, ollama")
                return
            }
        }

        runBlocking {
            try {
                configManager.setDefaultProvider(provider)
                println("✅ Default provider set to: $provider")
            } catch (e: Exception) {
                println("❌ Error setting default provider: ${e.message}")
            }
        }
    }

    private suspend fun setConfigValue(key: String, value: String) {
        val parts = key.split(".")
        if (parts.size != 2) {
            throw IllegalArgumentException("Key must be in format: provider.property (e.g., openai.api_key)")
        }

        val providerName = parts[0].lowercase()
        val property = parts[1].lowercase()

        val provider = when (providerName) {
            "openai" -> AiProvider.OPENAI
            "claude" -> AiProvider.CLAUDE
            "ollama" -> AiProvider.OLLAMA
            else -> throw IllegalArgumentException("Unknown provider: $providerName")
        }

        val config = configManager.loadConfig()
        val currentProviderConfig = config.providers[provider] ?: createDefaultProviderConfig(provider)

        val updatedConfig = when (property) {
            "api_key", "apikey" -> currentProviderConfig.copy(apiKey = value)
            "model" -> currentProviderConfig.copy(model = value)
            "base_url", "baseurl" -> currentProviderConfig.copy(baseUrl = value)
            "temperature" -> currentProviderConfig.copy(temperature = value.toFloatOrNull()
                ?: throw IllegalArgumentException("Temperature must be a number"))
            "max_tokens", "maxtokens" -> currentProviderConfig.copy(maxTokens = value.toIntOrNull()
                ?: throw IllegalArgumentException("Max tokens must be a number"))
            else -> throw IllegalArgumentException("Unknown property: $property")
        }

        configManager.updateProviderConfig(provider, updatedConfig)
    }

    private suspend fun getConfigValue(key: String): String? {
        val parts = key.split(".")
        if (parts.size != 2) {
            return null
        }

        val providerName = parts[0].lowercase()
        val property = parts[1].lowercase()

        val provider = when (providerName) {
            "openai" -> AiProvider.OPENAI
            "claude" -> AiProvider.CLAUDE
            "ollama" -> AiProvider.OLLAMA
            else -> return null
        }

        val config = configManager.loadConfig()
        val providerConfig = config.providers[provider] ?: return null

        return when (property) {
            "api_key", "apikey" -> providerConfig.apiKey
            "model" -> providerConfig.model
            "base_url", "baseurl" -> providerConfig.baseUrl
            "temperature" -> providerConfig.temperature.toString()
            "max_tokens", "maxtokens" -> providerConfig.maxTokens.toString()
            else -> null
        }
    }

    private fun maskApiKey(apiKey: String): String {
        return if (apiKey.length > 8) {
            "${apiKey.take(4)}${"*".repeat(apiKey.length - 8)}${apiKey.takeLast(4)}"
        } else {
            "*".repeat(apiKey.length)
        }
    }
}
