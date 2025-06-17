# KBuilderTDD 技术设计文档

## 项目概述

KBuilderTDD 是一个基于 TDD 开发的 AI 编程助手 CLI 工具，采用 Kotlin 开发，支持多种 AI 服务提供商。

### 当前架构概览

```
src/main/kotlin/com/aicodingcli/
├── Main.kt                    # CLI 入口和命令处理
├── ai/                        # AI 服务抽象层
│   ├── AiService.kt          # 服务接口和工厂
│   ├── AiModels.kt           # 数据模型
│   └── providers/            # 具体服务实现
├── config/                    # 配置管理
│   ├── ConfigManager.kt      # 配置管理器
│   └── AppConfig.kt          # 配置模型
├── history/                   # 对话历史管理
│   ├── HistoryManager.kt     # 历史管理器
│   └── HistoryModels.kt      # 历史数据模型
└── http/                      # HTTP 客户端封装
    ├── AiHttpClient.kt       # HTTP 客户端
    └── HttpModels.kt         # HTTP 模型
```

## 后续功能详细设计

### 1. 代码生成和分析功能

#### 1.1 功能概述
为 CLI 工具添加专门的代码生成、分析和重构功能，使其成为真正的编程助手。

#### 1.2 核心模块设计

##### 1.2.1 代码分析模块 (`code/analysis/`)

```kotlin
// CodeAnalyzer.kt - 代码分析器接口
interface CodeAnalyzer {
    suspend fun analyzeFile(filePath: String): CodeAnalysisResult
    suspend fun analyzeProject(projectPath: String): ProjectAnalysisResult
    suspend fun detectIssues(code: String, language: ProgrammingLanguage): List<CodeIssue>
    suspend fun suggestImprovements(code: String, language: ProgrammingLanguage): List<Improvement>
}

// CodeAnalysisModels.kt - 分析结果模型
data class CodeAnalysisResult(
    val filePath: String,
    val language: ProgrammingLanguage,
    val metrics: CodeMetrics,
    val issues: List<CodeIssue>,
    val suggestions: List<Improvement>,
    val dependencies: List<Dependency>
)

data class CodeMetrics(
    val linesOfCode: Int,
    val cyclomaticComplexity: Int,
    val maintainabilityIndex: Double,
    val testCoverage: Double?,
    val duplicatedLines: Int
)

data class CodeIssue(
    val type: IssueType,
    val severity: IssueSeverity,
    val message: String,
    val line: Int?,
    val column: Int?,
    val suggestion: String?
)

enum class IssueType {
    SYNTAX_ERROR, LOGIC_ERROR, PERFORMANCE, SECURITY, 
    CODE_SMELL, NAMING_CONVENTION, UNUSED_CODE
}
```

##### 1.2.2 代码生成模块 (`code/generation/`)

```kotlin
// CodeGenerator.kt - 代码生成器接口
interface CodeGenerator {
    suspend fun generateClass(spec: ClassGenerationSpec): GeneratedCode
    suspend fun generateFunction(spec: FunctionGenerationSpec): GeneratedCode
    suspend fun generateTests(spec: TestGenerationSpec): GeneratedCode
    suspend fun generateDocumentation(code: String, language: ProgrammingLanguage): GeneratedCode
}

// CodeGenerationModels.kt - 生成规格模型
data class ClassGenerationSpec(
    val className: String,
    val language: ProgrammingLanguage,
    val packageName: String?,
    val properties: List<PropertySpec>,
    val methods: List<MethodSpec>,
    val interfaces: List<String> = emptyList(),
    val annotations: List<String> = emptyList(),
    val designPatterns: List<DesignPattern> = emptyList()
)

data class FunctionGenerationSpec(
    val functionName: String,
    val language: ProgrammingLanguage,
    val parameters: List<ParameterSpec>,
    val returnType: String,
    val description: String,
    val requirements: List<String> = emptyList()
)

data class TestGenerationSpec(
    val targetCode: String,
    val language: ProgrammingLanguage,
    val testFramework: TestFramework,
    val testTypes: List<TestType> = listOf(TestType.UNIT),
    val coverageTarget: Double = 0.8
)

enum class TestType {
    UNIT, INTEGRATION, E2E, PERFORMANCE, SECURITY
}
```

##### 1.2.3 代码重构模块 (`code/refactoring/`)

```kotlin
// CodeRefactorer.kt - 重构器接口
interface CodeRefactorer {
    suspend fun extractMethod(spec: ExtractMethodSpec): RefactoringResult
    suspend fun renameSymbol(spec: RenameSpec): RefactoringResult
    suspend fun moveClass(spec: MoveClassSpec): RefactoringResult
    suspend fun optimizeImports(code: String, language: ProgrammingLanguage): RefactoringResult
    suspend fun applyDesignPattern(spec: DesignPatternSpec): RefactoringResult
}

data class RefactoringResult(
    val success: Boolean,
    val modifiedFiles: List<ModifiedFile>,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)

data class ModifiedFile(
    val filePath: String,
    val originalContent: String,
    val modifiedContent: String,
    val changes: List<CodeChange>
)
```

#### 1.3 CLI 命令扩展

```bash
# 代码分析命令
ai-coding-cli analyze file <file-path> [--language <lang>] [--output <format>]
ai-coding-cli analyze project <project-path> [--exclude <patterns>]
ai-coding-cli analyze issues <file-path> [--severity <level>]

# 代码生成命令
ai-coding-cli generate class --name <name> --language <lang> [--properties <props>]
ai-coding-cli generate function --name <name> --params <params> --return <type>
ai-coding-cli generate tests <file-path> [--framework <framework>] [--coverage <target>]
ai-coding-cli generate docs <file-path> [--format <format>]

# 代码重构命令
ai-coding-cli refactor extract-method <file-path> --start <line> --end <line> --name <name>
ai-coding-cli refactor rename <file-path> --symbol <old-name> --new-name <new-name>
ai-coding-cli refactor optimize-imports <file-path>
ai-coding-cli refactor apply-pattern <file-path> --pattern <pattern-name>
```

#### 1.4 实现策略

1. **阶段一：基础分析功能**
   - 实现文件级代码分析
   - 支持 Kotlin、Java、Python、JavaScript
   - 基础代码度量和问题检测

2. **阶段二：代码生成功能**
   - 实现类和函数生成
   - 集成 AI 提示工程优化
   - 支持多种编程语言模板

3. **阶段三：高级功能**
   - 项目级分析
   - 智能重构建议
   - 测试生成和覆盖率分析

### 2. 插件系统设计

#### 2.1 功能概述
设计一个灵活的插件系统，允许第三方开发者扩展 CLI 工具的功能。

#### 2.2 插件架构设计

##### 2.2.1 插件接口定义 (`plugins/`)

```kotlin
// Plugin.kt - 插件基础接口
interface Plugin {
    val metadata: PluginMetadata
    fun initialize(context: PluginContext)
    fun shutdown()
}

// PluginMetadata.kt - 插件元数据
data class PluginMetadata(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val author: String,
    val dependencies: List<PluginDependency> = emptyList(),
    val permissions: List<PluginPermission> = emptyList()
)

// PluginContext.kt - 插件上下文
interface PluginContext {
    val configManager: ConfigManager
    val historyManager: HistoryManager
    val aiServiceFactory: AiServiceFactory
    val logger: PluginLogger
    
    fun registerCommand(command: PluginCommand)
    fun registerEventHandler(handler: PluginEventHandler)
    fun getSharedData(key: String): Any?
    fun setSharedData(key: String, value: Any)
}
```

##### 2.2.2 命令插件接口

```kotlin
// CommandPlugin.kt - 命令插件接口
interface CommandPlugin : Plugin {
    val commands: List<PluginCommand>
}

// PluginCommand.kt - 插件命令定义
data class PluginCommand(
    val name: String,
    val description: String,
    val usage: String,
    val options: List<CommandOption> = emptyList(),
    val handler: suspend (args: CommandArgs, context: PluginContext) -> CommandResult
)

data class CommandOption(
    val name: String,
    val shortName: String?,
    val description: String,
    val required: Boolean = false,
    val hasValue: Boolean = true
)

data class CommandResult(
    val success: Boolean,
    val message: String?,
    val data: Any? = null
)
```

##### 2.2.3 AI 服务插件接口

```kotlin
// AiServicePlugin.kt - AI 服务插件接口
interface AiServicePlugin : Plugin {
    fun createAiService(config: AiServiceConfig): AiService
    val supportedProvider: AiProvider
}

// 示例：自定义 AI 服务插件
class CustomAiServicePlugin : AiServicePlugin {
    override val metadata = PluginMetadata(
        id = "custom-ai-service",
        name = "Custom AI Service",
        version = "1.0.0",
        description = "Custom AI service integration",
        author = "Developer"
    )
    
    override val supportedProvider = AiProvider.CUSTOM
    
    override fun createAiService(config: AiServiceConfig): AiService {
        return CustomAiService(config)
    }
}
```

#### 2.3 插件管理系统

##### 2.3.1 插件管理器

```kotlin
// PluginManager.kt - 插件管理器
class PluginManager(
    private val pluginDir: String = System.getProperty("user.home") + "/.aicodingcli/plugins"
) {
    private val loadedPlugins = mutableMapOf<String, Plugin>()
    private val pluginClassLoaders = mutableMapOf<String, ClassLoader>()
    
    suspend fun loadPlugin(pluginPath: String): Plugin
    suspend fun unloadPlugin(pluginId: String): Boolean
    suspend fun reloadPlugin(pluginId: String): Plugin
    fun getLoadedPlugins(): List<Plugin>
    fun getPlugin(pluginId: String): Plugin?
    
    suspend fun installPlugin(pluginArchive: String): Boolean
    suspend fun uninstallPlugin(pluginId: String): Boolean
    suspend fun updatePlugin(pluginId: String): Boolean
    
    fun validatePlugin(pluginPath: String): PluginValidationResult
}

// PluginRegistry.kt - 插件注册表
class PluginRegistry {
    private val commandPlugins = mutableMapOf<String, CommandPlugin>()
    private val aiServicePlugins = mutableMapOf<AiProvider, AiServicePlugin>()
    private val eventHandlers = mutableListOf<PluginEventHandler>()
    
    fun registerCommandPlugin(plugin: CommandPlugin)
    fun registerAiServicePlugin(plugin: AiServicePlugin)
    fun registerEventHandler(handler: PluginEventHandler)
    
    fun getCommandPlugin(commandName: String): CommandPlugin?
    fun getAiServicePlugin(provider: AiProvider): AiServicePlugin?
}
```

#### 2.4 插件 CLI 命令

```bash
# 插件管理命令
ai-coding-cli plugin list [--enabled] [--disabled]
ai-coding-cli plugin install <plugin-path-or-url>
ai-coding-cli plugin uninstall <plugin-id>
ai-coding-cli plugin enable <plugin-id>
ai-coding-cli plugin disable <plugin-id>
ai-coding-cli plugin update <plugin-id>
ai-coding-cli plugin info <plugin-id>

# 插件开发命令
ai-coding-cli plugin create <plugin-name> [--template <template>]
ai-coding-cli plugin validate <plugin-path>
ai-coding-cli plugin package <plugin-dir>
```

#### 2.5 插件开发工具包

##### 2.5.1 插件模板生成器

```kotlin
// PluginTemplateGenerator.kt
class PluginTemplateGenerator {
    fun generateCommandPlugin(spec: CommandPluginSpec): PluginTemplate
    fun generateAiServicePlugin(spec: AiServicePluginSpec): PluginTemplate
    fun generateEventPlugin(spec: EventPluginSpec): PluginTemplate
}

data class PluginTemplate(
    val files: Map<String, String>,
    val buildScript: String,
    val readme: String
)
```

##### 2.5.2 插件测试框架

```kotlin
// PluginTestFramework.kt
class PluginTestFramework {
    fun createTestContext(): PluginContext
    fun mockAiService(provider: AiProvider): AiService
    fun simulateCommand(command: String, args: Array<String>): CommandResult
}
```

### 3. 实现优先级和里程碑

#### 里程碑 1：代码分析基础功能（预计 2-3 周）
- [ ] 实现基础代码分析框架
- [ ] 支持 Kotlin 和 Java 文件分析
- [ ] 基础代码度量计算
- [ ] CLI 命令集成

#### 里程碑 2：代码生成功能（预计 2-3 周）
- [ ] 实现代码生成框架
- [ ] 类和函数生成功能
- [ ] AI 提示优化
- [ ] 多语言模板支持

#### 里程碑 3：插件系统核心（预计 3-4 周）
- [ ] 插件接口设计和实现
- [ ] 插件管理器开发
- [ ] 基础插件加载机制
- [ ] 插件 CLI 命令

#### 里程碑 4：高级功能和优化（预计 2-3 周）
- [ ] 代码重构功能
- [ ] 插件开发工具包
- [ ] 性能优化
- [ ] 文档和示例

### 4. 技术考虑

#### 4.1 安全性
- 插件沙箱机制
- 权限控制系统
- 代码签名验证

#### 4.2 性能
- 异步处理
- 缓存机制
- 增量分析

#### 4.3 扩展性
- 模块化设计
- 事件驱动架构
- 配置热重载

#### 4.4 测试策略
- 每个模块独立的单元测试
- 插件系统集成测试
- 端到端功能测试
- 性能基准测试

## 详细实现方案

### 5. 代码分析功能实现细节

#### 5.1 语言解析器设计

```kotlin
// LanguageParser.kt - 语言解析器接口
interface LanguageParser {
    fun parseFile(filePath: String): ParseResult
    fun parseCode(code: String): ParseResult
    val supportedLanguage: ProgrammingLanguage
}

// ParseResult.kt - 解析结果
data class ParseResult(
    val ast: AbstractSyntaxTree,
    val symbols: List<Symbol>,
    val imports: List<Import>,
    val errors: List<ParseError>
)

// KotlinParser.kt - Kotlin 解析器实现
class KotlinParser : LanguageParser {
    override val supportedLanguage = ProgrammingLanguage.KOTLIN

    override fun parseFile(filePath: String): ParseResult {
        // 使用 Kotlin 编译器 API 解析
        // 实现 AST 构建和符号提取
    }
}
```

#### 5.2 代码度量计算

```kotlin
// MetricsCalculator.kt - 度量计算器
class MetricsCalculator {
    fun calculateComplexity(ast: AbstractSyntaxTree): Int {
        // 计算圈复杂度
    }

    fun calculateMaintainabilityIndex(metrics: CodeMetrics): Double {
        // 计算可维护性指数
    }

    fun detectDuplication(code: String): List<DuplicatedBlock> {
        // 检测重复代码块
    }
}

// QualityAnalyzer.kt - 质量分析器
class QualityAnalyzer {
    fun analyzeNaming(symbols: List<Symbol>): List<NamingIssue>
    fun analyzeComplexity(ast: AbstractSyntaxTree): List<ComplexityIssue>
    fun analyzeDesignPatterns(ast: AbstractSyntaxTree): List<PatternSuggestion>
}
```

### 6. 代码生成功能实现细节

#### 6.1 模板引擎设计

```kotlin
// TemplateEngine.kt - 模板引擎
interface TemplateEngine {
    fun renderTemplate(template: CodeTemplate, context: TemplateContext): String
    fun loadTemplate(templatePath: String): CodeTemplate
}

// CodeTemplate.kt - 代码模板
data class CodeTemplate(
    val name: String,
    val language: ProgrammingLanguage,
    val content: String,
    val variables: List<TemplateVariable>,
    val metadata: TemplateMetadata
)

// AI 增强的代码生成器
class AiEnhancedCodeGenerator(
    private val aiService: AiService,
    private val templateEngine: TemplateEngine
) : CodeGenerator {

    override suspend fun generateClass(spec: ClassGenerationSpec): GeneratedCode {
        // 1. 构建 AI 提示
        val prompt = buildClassGenerationPrompt(spec)

        // 2. 调用 AI 服务
        val aiResponse = aiService.chat(AiRequest(
            messages = listOf(AiMessage(MessageRole.USER, prompt)),
            model = "gpt-4",
            temperature = 0.3f
        ))

        // 3. 解析和验证生成的代码
        val generatedCode = parseAndValidateCode(aiResponse.content, spec.language)

        // 4. 应用代码格式化和优化
        return optimizeGeneratedCode(generatedCode, spec)
    }
}
```

#### 6.2 智能提示工程

```kotlin
// PromptBuilder.kt - 提示构建器
class PromptBuilder {
    fun buildClassGenerationPrompt(spec: ClassGenerationSpec): String {
        return """
        Generate a ${spec.language.name.lowercase()} class with the following specifications:

        Class Name: ${spec.className}
        Package: ${spec.packageName ?: "default"}

        Properties:
        ${spec.properties.joinToString("\n") { "- ${it.name}: ${it.type} (${it.visibility})" }}

        Methods:
        ${spec.methods.joinToString("\n") { "- ${it.name}(${it.parameters.joinToString(", ")}): ${it.returnType}" }}

        Requirements:
        - Follow ${spec.language.name} best practices
        - Include proper documentation
        - Add appropriate annotations
        - Implement design patterns: ${spec.designPatterns.joinToString(", ")}

        Please generate clean, well-documented code.
        """.trimIndent()
    }
}
```

### 7. 插件系统实现细节

#### 7.1 插件加载机制

```kotlin
// PluginLoader.kt - 插件加载器
class PluginLoader {
    fun loadPluginFromJar(jarPath: String): Plugin {
        // 1. 创建独立的类加载器
        val classLoader = URLClassLoader(arrayOf(File(jarPath).toURI().toURL()))

        // 2. 读取插件元数据
        val metadata = readPluginMetadata(classLoader)

        // 3. 验证插件依赖
        validateDependencies(metadata.dependencies)

        // 4. 实例化插件主类
        val pluginClass = classLoader.loadClass(metadata.mainClass)
        return pluginClass.getDeclaredConstructor().newInstance() as Plugin
    }

    private fun readPluginMetadata(classLoader: ClassLoader): PluginMetadata {
        // 从 plugin.json 读取元数据
        val metadataStream = classLoader.getResourceAsStream("plugin.json")
            ?: throw PluginLoadException("plugin.json not found")

        return Json.decodeFromString(metadataStream.readText())
    }
}
```

#### 7.2 插件安全机制

```kotlin
// PluginSecurityManager.kt - 插件安全管理器
class PluginSecurityManager {
    fun validatePermissions(plugin: Plugin, operation: PluginOperation): Boolean {
        val requiredPermission = operation.requiredPermission
        return plugin.metadata.permissions.contains(requiredPermission)
    }

    fun createSandbox(plugin: Plugin): PluginSandbox {
        return PluginSandbox(
            allowedPaths = plugin.metadata.permissions
                .filterIsInstance<FileSystemPermission>()
                .flatMap { it.allowedPaths },
            allowedNetworkHosts = plugin.metadata.permissions
                .filterIsInstance<NetworkPermission>()
                .flatMap { it.allowedHosts }
        )
    }
}

// PluginSandbox.kt - 插件沙箱
class PluginSandbox(
    private val allowedPaths: List<String>,
    private val allowedNetworkHosts: List<String>
) {
    fun checkFileAccess(path: String): Boolean {
        return allowedPaths.any { allowedPath ->
            path.startsWith(allowedPath)
        }
    }

    fun checkNetworkAccess(host: String): Boolean {
        return allowedNetworkHosts.contains(host) ||
               allowedNetworkHosts.contains("*")
    }
}
```

### 8. 数据流和架构图

#### 8.1 代码分析流程

```
用户输入 → CLI 解析 → 文件读取 → 语言解析器 → AST 构建 →
度量计算 → 问题检测 → AI 分析增强 → 结果格式化 → 输出显示
```

#### 8.2 插件系统架构

```
CLI 核心 ← → 插件管理器 ← → 插件注册表
    ↓              ↓              ↓
配置管理器    插件加载器      安全管理器
    ↓              ↓              ↓
历史管理器    类加载器        沙箱环境
    ↓              ↓              ↓
AI 服务      插件实例        权限检查
```

### 9. 配置和部署

#### 9.1 插件配置格式

```json
{
  "plugin": {
    "id": "code-analyzer-plus",
    "name": "Advanced Code Analyzer",
    "version": "1.2.0",
    "description": "Enhanced code analysis with AI insights",
    "author": "Developer Team",
    "mainClass": "com.example.CodeAnalyzerPlugin",
    "dependencies": [
      {
        "id": "kotlin-parser",
        "version": ">=1.0.0"
      }
    ],
    "permissions": [
      {
        "type": "filesystem",
        "paths": ["./src/**", "./build/**"]
      },
      {
        "type": "network",
        "hosts": ["api.github.com"]
      }
    ],
    "commands": [
      {
        "name": "analyze-advanced",
        "description": "Advanced code analysis",
        "usage": "analyze-advanced <file-path> [options]"
      }
    ]
  }
}
```

#### 9.2 部署和分发

```kotlin
// PluginRepository.kt - 插件仓库
interface PluginRepository {
    suspend fun searchPlugins(query: String): List<PluginInfo>
    suspend fun downloadPlugin(pluginId: String, version: String): ByteArray
    suspend fun publishPlugin(plugin: PluginPackage): Boolean
    suspend fun getPluginInfo(pluginId: String): PluginInfo?
}

// 官方插件仓库实现
class OfficialPluginRepository : PluginRepository {
    private val baseUrl = "https://plugins.aicodingcli.com/api/v1"

    override suspend fun searchPlugins(query: String): List<PluginInfo> {
        // 实现插件搜索 API 调用
    }
}
```

### 10. 性能优化策略

#### 10.1 缓存机制

```kotlin
// AnalysisCache.kt - 分析结果缓存
class AnalysisCache {
    private val cache = ConcurrentHashMap<String, CachedAnalysisResult>()

    fun getCachedResult(fileHash: String): CodeAnalysisResult? {
        val cached = cache[fileHash]
        return if (cached != null && !cached.isExpired()) {
            cached.result
        } else null
    }

    fun cacheResult(fileHash: String, result: CodeAnalysisResult) {
        cache[fileHash] = CachedAnalysisResult(result, System.currentTimeMillis())
    }
}
```

#### 10.2 异步处理

```kotlin
// AsyncAnalyzer.kt - 异步分析器
class AsyncAnalyzer(
    private val analyzer: CodeAnalyzer,
    private val coroutineScope: CoroutineScope
) {
    suspend fun analyzeProjectAsync(projectPath: String): Flow<AnalysisProgress> = flow {
        val files = findSourceFiles(projectPath)
        val totalFiles = files.size
        var processedFiles = 0

        files.asFlow()
            .map { file ->
                coroutineScope.async {
                    analyzer.analyzeFile(file.absolutePath)
                }
            }
            .buffer(10) // 并发处理 10 个文件
            .collect { deferred ->
                val result = deferred.await()
                processedFiles++
                emit(AnalysisProgress(
                    processedFiles = processedFiles,
                    totalFiles = totalFiles,
                    currentFile = result.filePath,
                    result = result
                ))
            }
    }
}
```

这个技术设计文档详细规划了后续功能的实现方案。您觉得这个设计如何？是否需要我进一步细化某些特定部分，或者您希望开始实现其中的某个模块？
