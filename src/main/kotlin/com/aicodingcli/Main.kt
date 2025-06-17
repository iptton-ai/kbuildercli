package com.aicodingcli

import com.aicodingcli.ai.*
import com.aicodingcli.config.ConfigManager
import com.aicodingcli.history.HistoryManager
import com.aicodingcli.history.HistorySearchCriteria
import com.aicodingcli.history.MessageTokenUsage
import com.aicodingcli.code.analysis.DefaultCodeAnalyzer
import com.aicodingcli.code.common.ProgrammingLanguage
import kotlinx.coroutines.runBlocking
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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
  history <subcommand> Manage conversation history
  analyze <subcommand> Analyze code files and projects

Options:
  --version          Show version information
  --help             Show this help message
  --provider <name>  Use specific AI provider (openai, claude, ollama)
  --model <name>     Use specific model for the AI provider
  --stream           Enable streaming response (real-time output)
  --continue <id>    Continue an existing conversation
  --new              Force start a new conversation"""
    }

    private val configManager = ConfigManager()
    private val historyManager = HistoryManager()
    private val codeAnalyzer = DefaultCodeAnalyzer()

    fun run(args: Array<String>) {
        val (command, options) = parseArgs(args)

        when {
            args.isEmpty() -> printHelp()
            command == "--version" -> printVersion()
            command == "--help" -> printHelp()
            command == "test-connection" -> testConnection(options.provider, options.model)
            command == "ask" && options.message.isNotEmpty() -> askQuestion(options.message, options.provider, options.model, options.stream, options.continueConversationId, options.forceNew)
            command == "config" -> handleConfigCommand(args.drop(1).toTypedArray())
            command == "history" -> handleHistoryCommand(args.drop(1).toTypedArray())
            command == "analyze" -> handleAnalyzeCommand(args.drop(1).toTypedArray())
            else -> {
                println("Unknown command: $command")
                printHelp()
            }
        }
    }

    private data class CommandOptions(
        val provider: AiProvider? = null,
        val model: String? = null,
        val stream: Boolean = false,
        val continueConversationId: String? = null,
        val forceNew: Boolean = false,
        val message: String = ""
    )

    private fun parseArgs(args: Array<String>): Pair<String, CommandOptions> {
        if (args.isEmpty()) return "" to CommandOptions()

        var command = args[0]
        var provider: AiProvider? = null
        var model: String? = null
        var stream = false
        var continueConversationId: String? = null
        var forceNew = false
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
                "--stream" -> {
                    stream = true
                    i++
                }
                "--continue" -> {
                    if (i + 1 < args.size) {
                        continueConversationId = args[i + 1]
                        i += 2
                    } else {
                        println("--continue requires a conversation ID")
                        return command to CommandOptions()
                    }
                }
                "--new" -> {
                    forceNew = true
                    i++
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

        return command to CommandOptions(provider, model, stream, continueConversationId, forceNew, message)
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

    private fun askQuestion(
        question: String,
        provider: AiProvider? = null,
        model: String? = null,
        stream: Boolean = false,
        continueConversationId: String? = null,
        forceNew: Boolean = false
    ) {
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

                // Determine conversation to use
                val conversation = when {
                    continueConversationId != null -> {
                        // User explicitly wants to continue a specific conversation
                        val existing = historyManager.getConversation(continueConversationId)
                        if (existing == null) {
                            println("❌ Conversation not found: $continueConversationId")
                            return@runBlocking
                        }

                        // Validate provider/model compatibility
                        if (provider != null && existing.provider != config.provider) {
                            println("⚠️  Warning: Switching provider from ${existing.provider} to ${config.provider}")
                        }
                        if (model != null && existing.model != config.model) {
                            println("⚠️  Warning: Switching model from ${existing.model} to ${config.model}")
                        }

                        existing
                    }
                    forceNew -> {
                        // User explicitly wants a new conversation
                        createNewConversation(question, config)
                    }
                    else -> {
                        // Smart conversation management: continue recent conversation if compatible
                        val recentConversations = historyManager.getAllConversations().take(5)
                        val compatibleConversation = recentConversations.find { conv ->
                            conv.provider == config.provider &&
                            conv.model == config.model &&
                            conv.messages.isNotEmpty()
                        }

                        if (compatibleConversation != null) {
                            println("🔄 Continuing conversation: ${compatibleConversation.title} (${compatibleConversation.id.take(8)})")
                            compatibleConversation
                        } else {
                            createNewConversation(question, config)
                        }
                    }
                }

                // Add user message to history
                historyManager.addMessage(
                    conversationId = conversation.id,
                    role = MessageRole.USER,
                    content = question
                )

                // Build message history for context
                val messages = buildMessageHistory(conversation, question)

                val request = AiRequest(
                    messages = messages,
                    model = config.model,
                    temperature = config.temperature,
                    maxTokens = config.maxTokens,
                    stream = stream
                )

                println("🤖 Asking ${config.provider}...")
                if (model != null) {
                    println("   Using model: $model")
                }
                if (messages.size > 1) {
                    println("   Context: ${messages.size - 1} previous messages")
                }
                if (stream) {
                    println("   Streaming mode enabled")
                    println()

                    // Handle streaming response
                    val responseBuilder = StringBuilder()
                    service.streamChat(request).collect { chunk ->
                        print(chunk.content)
                        System.out.flush()
                        responseBuilder.append(chunk.content)

                        if (chunk.finishReason != null) {
                            println("\n")
                            println("📊 Streaming response completed")

                            // Add assistant response to history
                            historyManager.addMessage(
                                conversationId = conversation.id,
                                role = MessageRole.ASSISTANT,
                                content = responseBuilder.toString()
                            )
                        }
                    }
                } else {
                    // Handle regular response
                    val response = service.chat(request)
                    println("\n${response.content}")
                    println("\n📊 Usage: ${response.usage.totalTokens} tokens")

                    // Add assistant response to history
                    historyManager.addMessage(
                        conversationId = conversation.id,
                        role = MessageRole.ASSISTANT,
                        content = response.content,
                        tokenUsage = MessageTokenUsage(
                            promptTokens = response.usage.promptTokens,
                            completionTokens = response.usage.completionTokens,
                            totalTokens = response.usage.totalTokens
                        )
                    )
                }

                println("\n💾 Conversation ID: ${conversation.id.take(8)} (${conversation.messages.size} messages)")

            } catch (e: Exception) {
                println("❌ Error asking question: ${e.message}")
            }
        }
    }

    private fun createNewConversation(question: String, config: AiServiceConfig) =
        historyManager.createConversation(
            title = generateConversationTitle(question),
            provider = config.provider,
            model = config.model
        )

    private fun buildMessageHistory(conversation: com.aicodingcli.history.ConversationSession, currentQuestion: String): List<AiMessage> {
        val messages = mutableListOf<AiMessage>()

        // Add previous messages (limit to last 10 to avoid token limits)
        val recentMessages = conversation.messages.takeLast(10)
        recentMessages.forEach { historyMessage ->
            messages.add(AiMessage(
                role = historyMessage.role,
                content = historyMessage.content
            ))
        }

        // Add current question
        messages.add(AiMessage(
            role = MessageRole.USER,
            content = currentQuestion
        ))

        return messages
    }

    private fun generateConversationTitle(question: String): String {
        // Generate a meaningful title from the question
        val words = question.split(" ").take(5)
        return if (words.size <= 5) {
            question
        } else {
            "${words.joinToString(" ")}..."
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

    private fun handleHistoryCommand(args: Array<String>) {
        if (args.isEmpty()) {
            printHistoryHelp()
            return
        }

        when (args[0]) {
            "list" -> handleHistoryList(args.drop(1).toTypedArray())
            "show" -> handleHistoryShow(args.drop(1).toTypedArray())
            "search" -> handleHistorySearch(args.drop(1).toTypedArray())
            "delete" -> handleHistoryDelete(args.drop(1).toTypedArray())
            "clear" -> handleHistoryClear()
            "stats" -> handleHistoryStats()
            else -> {
                println("Unknown history subcommand: ${args[0]}")
                printHistoryHelp()
            }
        }
    }

    private fun printHistoryHelp() {
        println("""
            Conversation History Management Commands:

            history list [--limit N]        List recent conversations
            history show <id>               Show conversation details
            history search <query>          Search conversations
            history delete <id>             Delete a conversation
            history clear                   Clear all conversations
            history stats                   Show history statistics

            Examples:
            history list --limit 10
            history show abc123
            history search "kotlin"
            history delete abc123
        """.trimIndent())
    }

    private fun handleHistoryList(args: Array<String>) {
        var limit = 20

        // Parse limit argument
        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--limit" -> {
                    if (i + 1 < args.size) {
                        limit = args[i + 1].toIntOrNull() ?: 20
                        i += 2
                    } else {
                        println("--limit requires a value")
                        return
                    }
                }
                else -> i++
            }
        }

        val conversations = historyManager.getAllConversations().take(limit)

        if (conversations.isEmpty()) {
            println("No conversation history found.")
            return
        }

        println("Recent Conversations:")
        println("=" * 60)

        conversations.forEach { conversation ->
            val date = formatTimestamp(conversation.updatedAt)
            println("ID: ${conversation.id.take(8)}")
            println("Title: ${conversation.title}")
            println("Provider: ${conversation.provider} (${conversation.model})")
            println("Updated: $date")
            println("Messages: ${conversation.messages.size}")
            println("Summary: ${conversation.getSummary()}")
            println("-" * 40)
        }
    }

    private fun handleHistoryShow(args: Array<String>) {
        if (args.isEmpty()) {
            println("Usage: history show <conversation-id>")
            return
        }

        val conversationId = args[0]
        val conversation = historyManager.getConversation(conversationId)

        if (conversation == null) {
            println("Conversation not found: $conversationId")
            return
        }

        println("Conversation: ${conversation.title}")
        println("ID: ${conversation.id}")
        println("Provider: ${conversation.provider} (${conversation.model})")
        println("Created: ${formatTimestamp(conversation.createdAt)}")
        println("Updated: ${formatTimestamp(conversation.updatedAt)}")
        println("Messages: ${conversation.messages.size}")
        println("=" * 60)

        conversation.messages.forEach { message ->
            val timestamp = formatTimestamp(message.timestamp)
            val role = when (message.role) {
                MessageRole.USER -> "👤 User"
                MessageRole.ASSISTANT -> "🤖 Assistant"
                MessageRole.SYSTEM -> "⚙️ System"
            }

            println("[$timestamp] $role:")
            println(message.content)

            message.tokenUsage?.let { usage ->
                println("📊 Tokens: ${usage.totalTokens} (prompt: ${usage.promptTokens}, completion: ${usage.completionTokens})")
            }

            println("-" * 40)
        }
    }

    private fun handleHistorySearch(args: Array<String>) {
        if (args.isEmpty()) {
            println("Usage: history search <query>")
            return
        }

        val query = args.joinToString(" ")
        val criteria = HistorySearchCriteria(query = query, limit = 20)
        val conversations = historyManager.searchConversations(criteria)

        if (conversations.isEmpty()) {
            println("No conversations found matching: $query")
            return
        }

        println("Search Results for: $query")
        println("=" * 60)

        conversations.forEach { conversation ->
            val date = formatTimestamp(conversation.updatedAt)
            println("ID: ${conversation.id.take(8)}")
            println("Title: ${conversation.title}")
            println("Provider: ${conversation.provider} (${conversation.model})")
            println("Updated: $date")
            println("Summary: ${conversation.getSummary()}")
            println("-" * 40)
        }
    }

    private fun handleHistoryDelete(args: Array<String>) {
        if (args.isEmpty()) {
            println("Usage: history delete <conversation-id>")
            return
        }

        val conversationId = args[0]
        val deleted = historyManager.deleteConversation(conversationId)

        if (deleted) {
            println("✅ Conversation deleted: $conversationId")
        } else {
            println("❌ Conversation not found: $conversationId")
        }
    }

    private fun handleHistoryClear() {
        print("Are you sure you want to clear all conversation history? (y/N): ")
        val confirmation = readlnOrNull()?.lowercase()

        if (confirmation == "y" || confirmation == "yes") {
            historyManager.clearAllConversations()
            println("✅ All conversation history cleared.")
        } else {
            println("Operation cancelled.")
        }
    }

    private fun handleHistoryStats() {
        val stats = historyManager.getStatistics()

        println("Conversation History Statistics:")
        println("=" * 40)
        println("Total Conversations: ${stats.totalConversations}")
        println("Total Messages: ${stats.totalMessages}")
        println("Total Tokens Used: ${stats.totalTokensUsed}")
        println()

        if (stats.providerBreakdown.isNotEmpty()) {
            println("Provider Breakdown:")
            stats.providerBreakdown.forEach { (provider, count) ->
                println("  $provider: $count conversations")
            }
            println()
        }

        stats.oldestConversation?.let { oldest ->
            println("Oldest Conversation: ${formatTimestamp(oldest)}")
        }

        stats.newestConversation?.let { newest ->
            println("Newest Conversation: ${formatTimestamp(newest)}")
        }
    }

    private fun formatTimestamp(epochSecond: Long): String {
        val dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(epochSecond),
            ZoneId.systemDefault()
        )
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

    private operator fun String.times(n: Int): String = this.repeat(n)

    // Code Analysis Commands
    private fun handleAnalyzeCommand(args: Array<String>) {
        if (args.isEmpty()) {
            printAnalyzeHelp()
            return
        }

        when (args[0]) {
            "file" -> handleAnalyzeFile(args.drop(1).toTypedArray())
            "project" -> handleAnalyzeProject(args.drop(1).toTypedArray())
            "metrics" -> handleAnalyzeMetrics(args.drop(1).toTypedArray())
            "issues" -> handleAnalyzeIssues(args.drop(1).toTypedArray())
            else -> {
                println("Unknown analyze subcommand: ${args[0]}")
                printAnalyzeHelp()
            }
        }
    }

    private fun printAnalyzeHelp() {
        println("""
            Code Analysis Commands:

            analyze file <file-path>        Analyze a single file
            analyze project <project-path>  Analyze an entire project
            analyze metrics <file-path>     Show code metrics for a file
            analyze issues <file-path>      Show code issues for a file

            Options:
            --format <format>               Output format (text, json) [default: text]
            --language <lang>               Force language detection (kotlin, java, python)

            Examples:
            analyze file src/main/kotlin/Main.kt
            analyze project src/main/kotlin
            analyze metrics --format json src/main/kotlin/Main.kt
            analyze issues src/main/kotlin/Main.kt
        """.trimIndent())
    }

    private fun handleAnalyzeFile(args: Array<String>) {
        if (args.isEmpty()) {
            println("Usage: analyze file <file-path> [--format <format>] [--language <lang>]")
            return
        }

        val filePath = args[0]
        val options = parseAnalyzeOptions(args.drop(1).toTypedArray())

        runBlocking {
            try {
                val result = codeAnalyzer.analyzeFile(filePath)
                displayAnalysisResult(result, options.format)
            } catch (e: Exception) {
                println("❌ Error analyzing file: ${e.message}")
            }
        }
    }

    private fun handleAnalyzeProject(args: Array<String>) {
        if (args.isEmpty()) {
            println("Usage: analyze project <project-path> [--format <format>]")
            return
        }

        val projectPath = args[0]
        val options = parseAnalyzeOptions(args.drop(1).toTypedArray())

        runBlocking {
            try {
                val result = codeAnalyzer.analyzeProject(projectPath)
                displayProjectAnalysisResult(result, options.format)
            } catch (e: Exception) {
                println("❌ Error analyzing project: ${e.message}")
            }
        }
    }

    private fun handleAnalyzeMetrics(args: Array<String>) {
        if (args.isEmpty()) {
            println("Usage: analyze metrics <file-path> [--format <format>]")
            return
        }

        val filePath = args[0]
        val options = parseAnalyzeOptions(args.drop(1).toTypedArray())

        runBlocking {
            try {
                val result = codeAnalyzer.analyzeFile(filePath)
                displayMetrics(result.metrics, options.format)
            } catch (e: Exception) {
                println("❌ Error analyzing metrics: ${e.message}")
            }
        }
    }

    private fun handleAnalyzeIssues(args: Array<String>) {
        if (args.isEmpty()) {
            println("Usage: analyze issues <file-path> [--format <format>]")
            return
        }

        val filePath = args[0]
        val options = parseAnalyzeOptions(args.drop(1).toTypedArray())

        runBlocking {
            try {
                val result = codeAnalyzer.analyzeFile(filePath)
                displayIssues(result.issues, options.format)
            } catch (e: Exception) {
                println("❌ Error analyzing issues: ${e.message}")
            }
        }
    }

    private data class AnalyzeOptions(
        val format: String = "text",
        val language: ProgrammingLanguage? = null
    )

    private fun parseAnalyzeOptions(args: Array<String>): AnalyzeOptions {
        var format = "text"
        var language: ProgrammingLanguage? = null

        var i = 0
        while (i < args.size) {
            when (args[i]) {
                "--format" -> {
                    if (i + 1 < args.size) {
                        format = args[i + 1]
                        i += 2
                    } else {
                        println("--format requires a value")
                        i++
                    }
                }
                "--language" -> {
                    if (i + 1 < args.size) {
                        language = when (args[i + 1].lowercase()) {
                            "kotlin" -> ProgrammingLanguage.KOTLIN
                            "java" -> ProgrammingLanguage.JAVA
                            "python" -> ProgrammingLanguage.PYTHON
                            "javascript" -> ProgrammingLanguage.JAVASCRIPT
                            "typescript" -> ProgrammingLanguage.TYPESCRIPT
                            else -> {
                                println("Unknown language: ${args[i + 1]}")
                                null
                            }
                        }
                        i += 2
                    } else {
                        println("--language requires a value")
                        i++
                    }
                }
                else -> i++
            }
        }

        return AnalyzeOptions(format, language)
    }

    private fun displayAnalysisResult(result: com.aicodingcli.code.analysis.CodeAnalysisResult, format: String) {
        when (format.lowercase()) {
            "json" -> displayAnalysisResultJson(result)
            else -> displayAnalysisResultText(result)
        }
    }

    private fun displayAnalysisResultText(result: com.aicodingcli.code.analysis.CodeAnalysisResult) {
        println("📊 Code Analysis Results")
        println("=" * 50)
        println("File: ${result.filePath}")
        println("Language: ${result.language.displayName}")
        println()

        // Display metrics
        println("📈 Metrics:")
        println("  Lines of Code: ${result.metrics.linesOfCode}")
        println("  Cyclomatic Complexity: ${result.metrics.cyclomaticComplexity}")
        println("  Maintainability Index: ${"%.1f".format(result.metrics.maintainabilityIndex)}")
        result.metrics.testCoverage?.let { coverage ->
            println("  Test Coverage: ${"%.1f".format(coverage)}%")
        }
        println("  Duplicated Lines: ${result.metrics.duplicatedLines}")
        println()

        // Display issues
        if (result.issues.isNotEmpty()) {
            println("⚠️  Issues (${result.issues.size}):")
            result.issues.forEach { issue ->
                val severity = when (issue.severity) {
                    com.aicodingcli.code.analysis.IssueSeverity.CRITICAL -> "🔴 CRITICAL"
                    com.aicodingcli.code.analysis.IssueSeverity.HIGH -> "🟠 HIGH"
                    com.aicodingcli.code.analysis.IssueSeverity.MEDIUM -> "🟡 MEDIUM"
                    com.aicodingcli.code.analysis.IssueSeverity.LOW -> "🟢 LOW"
                }
                val location = if (issue.line != null) " (line ${issue.line})" else ""
                println("  $severity: ${issue.message}$location")
                issue.suggestion?.let { suggestion ->
                    println("    💡 Suggestion: $suggestion")
                }
            }
            println()
        }

        // Display suggestions
        if (result.suggestions.isNotEmpty()) {
            println("💡 Improvement Suggestions (${result.suggestions.size}):")
            result.suggestions.forEach { suggestion ->
                val priority = when (suggestion.priority) {
                    com.aicodingcli.code.analysis.ImprovementPriority.HIGH -> "🔴 HIGH"
                    com.aicodingcli.code.analysis.ImprovementPriority.MEDIUM -> "🟡 MEDIUM"
                    com.aicodingcli.code.analysis.ImprovementPriority.LOW -> "🟢 LOW"
                }
                val location = if (suggestion.line != null) " (line ${suggestion.line})" else ""
                println("  $priority: ${suggestion.description}$location")
            }
            println()
        }

        // Display dependencies
        if (result.dependencies.isNotEmpty()) {
            println("📦 Dependencies (${result.dependencies.size}):")
            result.dependencies.forEach { dependency ->
                println("  ${dependency.name} (${dependency.version ?: "unknown"}) - ${dependency.type}")
            }
        }
    }

    private fun displayAnalysisResultJson(result: com.aicodingcli.code.analysis.CodeAnalysisResult) {
        // Simple JSON output - in a real implementation, you'd use a JSON library
        println("""{
  "filePath": "${result.filePath}",
  "language": "${result.language.displayName}",
  "metrics": {
    "linesOfCode": ${result.metrics.linesOfCode},
    "cyclomaticComplexity": ${result.metrics.cyclomaticComplexity},
    "maintainabilityIndex": ${result.metrics.maintainabilityIndex},
    "testCoverage": ${result.metrics.testCoverage},
    "duplicatedLines": ${result.metrics.duplicatedLines}
  },
  "issuesCount": ${result.issues.size},
  "suggestionsCount": ${result.suggestions.size},
  "dependenciesCount": ${result.dependencies.size}
}""")
    }

    private fun displayProjectAnalysisResult(result: com.aicodingcli.code.analysis.ProjectAnalysisResult, format: String) {
        when (format.lowercase()) {
            "json" -> displayProjectAnalysisResultJson(result)
            else -> displayProjectAnalysisResultText(result)
        }
    }

    private fun displayProjectAnalysisResultText(result: com.aicodingcli.code.analysis.ProjectAnalysisResult) {
        println("📊 Project Analysis Results")
        println("=" * 50)
        println("Project: ${result.projectPath}")
        println("Files Analyzed: ${result.fileResults.size}")
        println()

        // Display overall metrics
        println("📈 Overall Metrics:")
        println("  Total Lines of Code: ${result.overallMetrics.linesOfCode}")
        println("  Average Complexity: ${result.overallMetrics.cyclomaticComplexity}")
        println("  Average Maintainability: ${"%.1f".format(result.overallMetrics.maintainabilityIndex)}")
        println()

        // Display summary
        println("📋 Summary:")
        println("  Total Files: ${result.summary.totalFiles}")
        println("  Total Issues: ${result.summary.totalIssues}")
        println("  Critical Issues: ${result.summary.criticalIssues}")
        println("  Average Complexity: ${"%.1f".format(result.summary.averageComplexity)}")
        println("  Overall Maintainability: ${"%.1f".format(result.summary.overallMaintainabilityIndex)}")
        println()

        // Display top issues by file
        val filesWithIssues = result.fileResults.filter { it.issues.isNotEmpty() }
        if (filesWithIssues.isNotEmpty()) {
            println("⚠️  Files with Issues:")
            filesWithIssues.sortedByDescending { it.issues.size }.take(10).forEach { fileResult ->
                val fileName = File(fileResult.filePath).name
                println("  $fileName: ${fileResult.issues.size} issues")
            }
        }
    }

    private fun displayProjectAnalysisResultJson(result: com.aicodingcli.code.analysis.ProjectAnalysisResult) {
        println("""{
  "projectPath": "${result.projectPath}",
  "filesAnalyzed": ${result.fileResults.size},
  "overallMetrics": {
    "linesOfCode": ${result.overallMetrics.linesOfCode},
    "averageComplexity": ${result.overallMetrics.cyclomaticComplexity},
    "averageMaintainability": ${result.overallMetrics.maintainabilityIndex}
  },
  "summary": {
    "totalFiles": ${result.summary.totalFiles},
    "totalIssues": ${result.summary.totalIssues},
    "criticalIssues": ${result.summary.criticalIssues},
    "averageComplexity": ${result.summary.averageComplexity},
    "overallMaintainability": ${result.summary.overallMaintainabilityIndex}
  }
}""")
    }

    private fun displayMetrics(metrics: com.aicodingcli.code.analysis.CodeMetrics, format: String) {
        when (format.lowercase()) {
            "json" -> {
                println("""{
  "linesOfCode": ${metrics.linesOfCode},
  "cyclomaticComplexity": ${metrics.cyclomaticComplexity},
  "maintainabilityIndex": ${metrics.maintainabilityIndex},
  "testCoverage": ${metrics.testCoverage},
  "duplicatedLines": ${metrics.duplicatedLines}
}""")
            }
            else -> {
                println("📈 Code Metrics:")
                println("  Lines of Code: ${metrics.linesOfCode}")
                println("  Cyclomatic Complexity: ${metrics.cyclomaticComplexity}")
                println("  Maintainability Index: ${"%.1f".format(metrics.maintainabilityIndex)}")
                metrics.testCoverage?.let { coverage ->
                    println("  Test Coverage: ${"%.1f".format(coverage)}%")
                }
                println("  Duplicated Lines: ${metrics.duplicatedLines}")
            }
        }
    }

    private fun displayIssues(issues: List<com.aicodingcli.code.analysis.CodeIssue>, format: String) {
        when (format.lowercase()) {
            "json" -> {
                println("[")
                issues.forEachIndexed { index, issue ->
                    println("""  {
    "type": "${issue.type}",
    "severity": "${issue.severity}",
    "message": "${issue.message}",
    "line": ${issue.line},
    "column": ${issue.column},
    "suggestion": "${issue.suggestion}"
  }${if (index < issues.size - 1) "," else ""}""")
                }
                println("]")
            }
            else -> {
                if (issues.isEmpty()) {
                    println("✅ No issues found!")
                } else {
                    println("⚠️  Code Issues (${issues.size}):")
                    issues.forEach { issue ->
                        val severity = when (issue.severity) {
                            com.aicodingcli.code.analysis.IssueSeverity.CRITICAL -> "🔴 CRITICAL"
                            com.aicodingcli.code.analysis.IssueSeverity.HIGH -> "🟠 HIGH"
                            com.aicodingcli.code.analysis.IssueSeverity.MEDIUM -> "🟡 MEDIUM"
                            com.aicodingcli.code.analysis.IssueSeverity.LOW -> "🟢 LOW"
                        }
                        val location = if (issue.line != null) " (line ${issue.line})" else ""
                        println("  $severity: ${issue.message}$location")
                        issue.suggestion?.let { suggestion ->
                            println("    💡 Suggestion: $suggestion")
                        }
                    }
                }
            }
        }
    }
}
