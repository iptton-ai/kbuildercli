package com.aicodingcli.plugins

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Base specification for plugin generation
 */
sealed class PluginSpec {
    abstract val pluginId: String
    abstract val pluginName: String
    abstract val version: String
    abstract val description: String
    abstract val author: String
    abstract val packageName: String
}

/**
 * Specification for command plugin generation
 */
data class CommandPluginSpec(
    override val pluginId: String,
    override val pluginName: String,
    override val version: String = "1.0.0",
    override val description: String,
    override val author: String,
    override val packageName: String,
    val commandName: String,
    val commandDescription: String
) : PluginSpec()

/**
 * Specification for AI service plugin generation
 */
data class AiServicePluginSpec(
    override val pluginId: String,
    override val pluginName: String,
    override val version: String = "1.0.0",
    override val description: String,
    override val author: String,
    override val packageName: String,
    val providerName: String,
    val defaultBaseUrl: String,
    val defaultModel: String,
    val requiresApiKey: Boolean = true
) : PluginSpec()

/**
 * Specification for event plugin generation
 */
data class EventPluginSpec(
    override val pluginId: String,
    override val pluginName: String,
    override val version: String = "1.0.0",
    override val description: String,
    override val author: String,
    override val packageName: String,
    val eventTypes: List<String>
) : PluginSpec()

/**
 * Generated plugin template
 */
data class PluginTemplate(
    val files: Map<String, String>,
    val buildScript: String,
    val readme: String
)

/**
 * Generator for creating plugin templates and scaffolding
 */
class PluginTemplateGenerator {
    
    /**
     * Generate a command plugin template
     */
    fun generateCommandPlugin(spec: CommandPluginSpec): PluginTemplate {
        val packagePath = spec.packageName.replace(".", "/")
        val className = "${spec.pluginName.toCamelCase()}Plugin"
        
        val files = mutableMapOf<String, String>()
        
        // Main plugin class
        files["src/main/kotlin/$packagePath/$className.kt"] = generateCommandPluginClass(spec, className)
        
        // Plugin metadata
        files["src/main/resources/plugin.json"] = generatePluginMetadata(spec)
        
        // Build script
        files["build.gradle.kts"] = generateBuildScript(spec)
        
        // README
        files["README.md"] = generateReadme(spec)
        
        // Example test
        files["src/test/kotlin/$packagePath/${className}Test.kt"] = generateCommandPluginTest(spec, className)
        
        return PluginTemplate(
            files = files,
            buildScript = files["build.gradle.kts"]!!,
            readme = files["README.md"]!!
        )
    }
    
    /**
     * Generate an AI service plugin template
     */
    fun generateAiServicePlugin(spec: AiServicePluginSpec): PluginTemplate {
        val packagePath = spec.packageName.replace(".", "/")
        val className = "${spec.providerName.toCamelCase()}ServicePlugin"
        val serviceClassName = "${spec.providerName.toCamelCase()}Service"
        
        val files = mutableMapOf<String, String>()
        
        // Main plugin class
        files["src/main/kotlin/$packagePath/$className.kt"] = generateAiServicePluginClass(spec, className, serviceClassName)
        
        // AI service implementation
        files["src/main/kotlin/$packagePath/$serviceClassName.kt"] = generateAiServiceClass(spec, serviceClassName)
        
        // Plugin metadata
        files["src/main/resources/plugin.json"] = generatePluginMetadata(spec)
        
        // Build script
        files["build.gradle.kts"] = generateBuildScript(spec)
        
        // README
        files["README.md"] = generateReadme(spec)
        
        // Example test
        files["src/test/kotlin/$packagePath/${className}Test.kt"] = generateAiServicePluginTest(spec, className)
        
        return PluginTemplate(
            files = files,
            buildScript = files["build.gradle.kts"]!!,
            readme = files["README.md"]!!
        )
    }
    
    /**
     * Generate an event plugin template
     */
    fun generateEventPlugin(spec: EventPluginSpec): PluginTemplate {
        val packagePath = spec.packageName.replace(".", "/")
        val className = "${spec.pluginName.toCamelCase()}Plugin"
        
        val files = mutableMapOf<String, String>()
        
        // Main plugin class
        files["src/main/kotlin/$packagePath/$className.kt"] = generateEventPluginClass(spec, className)
        
        // Plugin metadata
        files["src/main/resources/plugin.json"] = generatePluginMetadata(spec)
        
        // Build script
        files["build.gradle.kts"] = generateBuildScript(spec)
        
        // README
        files["README.md"] = generateReadme(spec)
        
        // Example test
        files["src/test/kotlin/$packagePath/${className}Test.kt"] = generateEventPluginTest(spec, className)
        
        return PluginTemplate(
            files = files,
            buildScript = files["build.gradle.kts"]!!,
            readme = files["README.md"]!!
        )
    }
    
    /**
     * Create plugin project structure on disk
     */
    fun createPluginProject(template: PluginTemplate, outputDir: String) {
        val projectDir = File(outputDir)
        if (!projectDir.exists()) {
            projectDir.mkdirs()
        }
        
        template.files.forEach { (filePath, content) ->
            val file = File(projectDir, filePath)
            file.parentFile.mkdirs()
            file.writeText(content)
        }
        
        println("✅ Plugin project created at: ${projectDir.absolutePath}")
        println("📁 Files created:")
        template.files.keys.sorted().forEach { filePath ->
            println("  - $filePath")
        }
    }
    
    private fun generateCommandPluginClass(spec: CommandPluginSpec, className: String): String {
        return """
package ${spec.packageName}

import com.aicodingcli.plugins.*

/**
 * ${spec.description}
 */
class $className : BaseCommandPlugin() {
    
    override val metadata = PluginMetadata(
        id = "${spec.pluginId}",
        name = "${spec.pluginName}",
        version = "${spec.version}",
        description = "${spec.description}",
        author = "${spec.author}",
        mainClass = "${spec.packageName}.$className"
    )
    
    override val commands = listOf(
        createCommand(
            name = "${spec.commandName}",
            description = "${spec.commandDescription}",
            usage = "${spec.commandName} [options]",
            options = listOf(
                CommandOption(
                    name = "output",
                    shortName = "o",
                    description = "Output format",
                    required = false,
                    defaultValue = "text"
                )
            )
        ) { args, context ->
            // TODO: Implement your command logic here
            val output = args.getOptionOrDefault("output", "text")
            
            context.logger.info("Executing ${spec.commandName} command with output format: ${'$'}output")
            
            CommandResult.success("${spec.commandName} executed successfully!")
        }
    )
}
        """.trimIndent()
    }
    
    private fun generateAiServicePluginClass(spec: AiServicePluginSpec, className: String, serviceClassName: String): String {
        return """
package ${spec.packageName}

import com.aicodingcli.plugins.*
import com.aicodingcli.ai.*

/**
 * ${spec.description}
 */
class $className : BaseAiServicePlugin() {
    
    override val metadata = PluginMetadata(
        id = "${spec.pluginId}",
        name = "${spec.pluginName}",
        version = "${spec.version}",
        description = "${spec.description}",
        author = "${spec.author}",
        mainClass = "${spec.packageName}.$className"
    )
    
    override val supportedProvider = AiProvider.${spec.providerName.uppercase()}
    
    override fun createAiService(config: AiServiceConfig): AiService {
        return $serviceClassName(config)
    }
    
    override fun getSupportedModels(): List<String> {
        return listOf(
            // TODO: Add supported models for ${spec.providerName}
            "model-1",
            "model-2"
        )
    }
    
    override fun getDefaultBaseUrl(): String {
        return "${spec.defaultBaseUrl}"
    }
    
    override fun getDefaultModel(): String {
        return "${spec.defaultModel}"
    }
    
    override fun requiresApiKey(): Boolean {
        return ${spec.requiresApiKey}
    }
}
        """.trimIndent()
    }
    
    private fun generateAiServiceClass(spec: AiServicePluginSpec, serviceClassName: String): String {
        return """
package ${spec.packageName}

import com.aicodingcli.ai.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * AI service implementation for ${spec.providerName}
 */
class $serviceClassName(override val config: AiServiceConfig) : BaseAiService(config) {
    
    override suspend fun chat(request: AiRequest): AiResponse {
        validateRequest(request)
        
        // TODO: Implement actual API call to ${spec.providerName}
        // This is a placeholder implementation
        
        return AiResponse(
            content = "Response from ${spec.providerName}: ${'$'}{request.messages.last().content}",
            model = request.model,
            usage = TokenUsage(
                promptTokens = 10,
                completionTokens = 20,
                totalTokens = 30
            ),
            finishReason = FinishReason.STOP
        )
    }
    
    override suspend fun streamChat(request: AiRequest): Flow<AiStreamChunk> {
        validateRequest(request)
        
        // TODO: Implement actual streaming API call to ${spec.providerName}
        // This is a placeholder implementation
        
        return flowOf(
            AiStreamChunk("Response ", null),
            AiStreamChunk("from ", null),
            AiStreamChunk("${spec.providerName}", FinishReason.STOP)
        )
    }
    
    override suspend fun testConnection(): Boolean {
        return try {
            // TODO: Implement actual connection test to ${spec.providerName}
            // This is a placeholder implementation
            true
        } catch (e: Exception) {
            false
        }
    }
}
        """.trimIndent()
    }
    
    private fun generateEventPluginClass(spec: EventPluginSpec, className: String): String {
        return """
package ${spec.packageName}

import com.aicodingcli.plugins.*

/**
 * ${spec.description}
 */
class $className : Plugin {
    
    override val metadata = PluginMetadata(
        id = "${spec.pluginId}",
        name = "${spec.pluginName}",
        version = "${spec.version}",
        description = "${spec.description}",
        author = "${spec.author}",
        mainClass = "${spec.packageName}.$className"
    )
    
    private lateinit var context: PluginContext
    
    override fun initialize(context: PluginContext) {
        this.context = context
        
        // Register event handler
        context.registerEventHandler(object : PluginEventHandler {
            override val eventTypes = setOf(
                ${spec.eventTypes.joinToString(",\n                ") { "PluginEventType.$it" }}
            )
            
            override suspend fun handleEvent(event: PluginEvent) {
                when (event.type) {
                    ${spec.eventTypes.joinToString("\n                    ") { 
                        "PluginEventType.$it -> handle${it.toCamelCase()}Event(event)"
                    }}
                    else -> {
                        // Ignore other events
                    }
                }
            }
        })
        
        context.logger.info("${spec.pluginName} plugin initialized")
    }
    
    override fun shutdown() {
        context.logger.info("${spec.pluginName} plugin shut down")
    }
    
    ${spec.eventTypes.joinToString("\n    \n    ") { eventType ->
        """private suspend fun handle${eventType.toCamelCase()}Event(event: PluginEvent) {
        // TODO: Implement $eventType event handling
        context.logger.info("Handling $eventType event: ${'$'}{event.data}")
    }"""
    }}
}
        """.trimIndent()
    }
    
    private fun generatePluginMetadata(spec: PluginSpec): String {
        return """
{
  "id": "${spec.pluginId}",
  "name": "${spec.pluginName}",
  "version": "${spec.version}",
  "description": "${spec.description}",
  "author": "${spec.author}",
  "mainClass": "${spec.packageName}.${spec.pluginName.toCamelCase()}Plugin",
  "dependencies": [],
  "permissions": [
    {
      "type": "filesystem",
      "allowedPaths": ["./temp"],
      "readOnly": false
    }
  ]
}
        """.trimIndent()
    }
    
    private fun generateBuildScript(spec: PluginSpec): String {
        return """
plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "${spec.packageName}"
version = "${spec.version}"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("io.ktor:ktor-client-core:2.3.4")
    implementation("io.ktor:ktor-client-cio:2.3.4")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
    
    // Add AI Coding CLI as a dependency (you'll need to publish it to a repository)
    // implementation("com.aicodingcli:core:0.1.0")
    
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.7")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

tasks.shadowJar {
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "${spec.packageName}.${spec.pluginName.toCamelCase()}Plugin"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
        """.trimIndent()
    }
    
    private fun generateReadme(spec: PluginSpec): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        
        return """
# ${spec.pluginName}

${spec.description}

## Information

- **Plugin ID**: ${spec.pluginId}
- **Version**: ${spec.version}
- **Author**: ${spec.author}
- **Created**: $timestamp

## Building

To build the plugin:

```bash
./gradlew build
```

This will create a JAR file in `build/libs/` that can be installed in AI Coding CLI.

## Installation

1. Build the plugin (see above)
2. Install using AI Coding CLI:

```bash
ai-coding-cli plugin install build/libs/${spec.pluginId}-${spec.version}.jar
```

## Usage

${when (spec) {
    is CommandPluginSpec -> """
This plugin provides the following command:

```bash
ai-coding-cli ${spec.commandName} [options]
```

${spec.commandDescription}
"""
    is AiServicePluginSpec -> """
This plugin adds support for ${spec.providerName} AI service.

After installation, you can use it by configuring the provider:

```bash
ai-coding-cli config set ${spec.providerName.lowercase()}.api_key your-api-key
ai-coding-cli config provider ${spec.providerName.lowercase()}
```
"""
    is EventPluginSpec -> """
This plugin handles the following events:
${spec.eventTypes.joinToString("\n") { "- $it" }}

The plugin will automatically respond to these events when they occur.
"""
    else -> "See plugin documentation for usage instructions."
}}

## Development

This plugin was generated using the AI Coding CLI plugin template generator.

To modify the plugin:

1. Edit the source files in `src/main/kotlin/`
2. Update the plugin metadata in `src/main/resources/plugin.json` if needed
3. Rebuild and reinstall the plugin

## License

[Add your license information here]
        """.trimIndent()
    }
    
    private fun generateCommandPluginTest(spec: CommandPluginSpec, className: String): String {
        return """
package ${spec.packageName}

import com.aicodingcli.plugins.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import io.mockk.mockk

class ${className}Test {

    private lateinit var plugin: $className
    private lateinit var mockContext: PluginContext

    @BeforeEach
    fun setUp() {
        plugin = $className()
        mockContext = mockk<PluginContext>(relaxed = true)
    }

    @Test
    fun `should have correct metadata`() {
        assertEquals("${spec.pluginId}", plugin.metadata.id)
        assertEquals("${spec.pluginName}", plugin.metadata.name)
        assertEquals("${spec.version}", plugin.metadata.version)
        assertEquals("${spec.author}", plugin.metadata.author)
    }

    @Test
    fun `should provide ${spec.commandName} command`() {
        assertTrue(plugin.hasCommand("${spec.commandName}"))
        val command = plugin.getCommand("${spec.commandName}")
        assertNotNull(command)
        assertEquals("${spec.commandName}", command?.name)
    }

    @Test
    fun `should execute ${spec.commandName} command successfully`() = runBlocking {
        plugin.initialize(mockContext)

        val command = plugin.getCommand("${spec.commandName}")
        assertNotNull(command)

        val args = CommandArgs(
            args = emptyList(),
            options = mapOf("output" to "text"),
            rawArgs = arrayOf()
        )

        val result = command!!.handler(args, mockContext)
        assertTrue(result.success)
        assertNotNull(result.message)
    }
}
        """.trimIndent()
    }

    private fun generateAiServicePluginTest(spec: AiServicePluginSpec, className: String): String {
        return """
package ${spec.packageName}

import com.aicodingcli.plugins.*
import com.aicodingcli.ai.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import io.mockk.mockk

class ${className}Test {

    private lateinit var plugin: $className
    private lateinit var mockContext: PluginContext

    @BeforeEach
    fun setUp() {
        plugin = $className()
        mockContext = mockk<PluginContext>(relaxed = true)
    }

    @Test
    fun `should have correct metadata`() {
        assertEquals("${spec.pluginId}", plugin.metadata.id)
        assertEquals("${spec.pluginName}", plugin.metadata.name)
        assertEquals("${spec.version}", plugin.metadata.version)
        assertEquals("${spec.author}", plugin.metadata.author)
    }

    @Test
    fun `should support correct AI provider`() {
        assertEquals(AiProvider.${spec.providerName.uppercase()}, plugin.supportedProvider)
    }

    @Test
    fun `should create AI service`() {
        val config = AiServiceConfig(
            provider = AiProvider.${spec.providerName.uppercase()},
            apiKey = "test-key",
            model = "${spec.defaultModel}",
            baseUrl = "${spec.defaultBaseUrl}"
        )

        val service = plugin.createAiService(config)
        assertNotNull(service)
        assertEquals(config, service.config)
    }

    @Test
    fun `should validate configuration`() {
        val validConfig = AiServiceConfig(
            provider = AiProvider.${spec.providerName.uppercase()},
            apiKey = "test-key",
            model = "${spec.defaultModel}",
            baseUrl = "${spec.defaultBaseUrl}"
        )

        val result = plugin.validateConfig(validConfig)
        assertTrue(result.isValid)
    }

    @Test
    fun `should provide supported models`() {
        val models = plugin.getSupportedModels()
        assertNotNull(models)
        assertTrue(models.isNotEmpty())
    }
}
        """.trimIndent()
    }

    private fun generateEventPluginTest(spec: EventPluginSpec, className: String): String {
        return """
package ${spec.packageName}

import com.aicodingcli.plugins.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import io.mockk.mockk
import io.mockk.verify

class ${className}Test {

    private lateinit var plugin: $className
    private lateinit var mockContext: PluginContext

    @BeforeEach
    fun setUp() {
        plugin = $className()
        mockContext = mockk<PluginContext>(relaxed = true)
    }

    @Test
    fun `should have correct metadata`() {
        assertEquals("${spec.pluginId}", plugin.metadata.id)
        assertEquals("${spec.pluginName}", plugin.metadata.name)
        assertEquals("${spec.version}", plugin.metadata.version)
        assertEquals("${spec.author}", plugin.metadata.author)
    }

    @Test
    fun `should initialize and register event handler`() {
        plugin.initialize(mockContext)

        verify { mockContext.registerEventHandler(any()) }
    }

    @Test
    fun `should handle events correctly`() = runBlocking {
        plugin.initialize(mockContext)

        // Test would require more complex setup to verify event handling
        // This is a basic structure for event plugin testing
        assertTrue(true) // Placeholder assertion
    }
}
        """.trimIndent()
    }

    private fun String.toCamelCase(): String {
        return split("-", "_", " ")
            .joinToString("") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }
}
