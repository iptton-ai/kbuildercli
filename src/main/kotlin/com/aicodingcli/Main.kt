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
}
