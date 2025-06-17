package com.aicodingcli

import com.aicodingcli.ai.AiServiceFactory
import com.aicodingcli.ai.AiMessage
import com.aicodingcli.ai.AiRequest
import com.aicodingcli.ai.MessageRole
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
  --help             Show this help message"""
    }

    private val configManager = ConfigManager()

    fun run(args: Array<String>) {
        when {
            args.isEmpty() -> printHelp()
            args[0] == "--version" -> printVersion()
            args[0] == "--help" -> printHelp()
            args[0] == "test-connection" -> testConnection()
            args[0] == "ask" && args.size > 1 -> askQuestion(args.drop(1).joinToString(" "))
            else -> {
                println("Unknown command: ${args[0]}")
                printHelp()
            }
        }
    }

    private fun printVersion() {
        println(VERSION)
    }

    private fun printHelp() {
        println(HELP_TEXT)
    }

    private fun testConnection() {
        runBlocking {
            try {
                val config = configManager.getCurrentProviderConfig()
                val service = AiServiceFactory.createService(config)
                val result = service.testConnection()

                if (result) {
                    println("✅ Connection to ${config.provider} successful!")
                } else {
                    println("❌ Connection to ${config.provider} failed!")
                }
            } catch (e: Exception) {
                println("❌ Error testing connection: ${e.message}")
            }
        }
    }

    private fun askQuestion(question: String) {
        runBlocking {
            try {
                val config = configManager.getCurrentProviderConfig()
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
                val response = service.chat(request)
                println("\n${response.content}")
                println("\n📊 Usage: ${response.usage.totalTokens} tokens")

            } catch (e: Exception) {
                println("❌ Error asking question: ${e.message}")
            }
        }
    }
}
