package com.aicodingcli.code

import com.aicodingcli.code.analysis.DefaultCodeAnalyzer
import com.aicodingcli.code.common.ProgrammingLanguage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DebugTest {

    @TempDir
    lateinit var tempDir: File

    private val analyzer = DefaultCodeAnalyzer()

    @Test
    fun `debug complex code analysis`() = runBlocking {
        val complexCode = """
            class ComplexClass {
                fun complexMethod(input: String): String {
                    if (input.isNotEmpty()) {
                        if (input.length > 10) {
                            if (input.contains("test")) {
                                return input.uppercase()
                            } else {
                                return input.lowercase()
                            }
                        } else {
                            return input
                        }
                    } else {
                        return "empty"
                    }
                }
            }
        """.trimIndent()
        
        val complexFile = File(tempDir, "ComplexClass.kt")
        complexFile.writeText(complexCode)
        
        val result = analyzer.analyzeFile(complexFile.absolutePath)
        
        println("=== DEBUG INFO ===")
        println("File path: ${result.filePath}")
        println("Language: ${result.language}")
        println("Lines of code: ${result.metrics.linesOfCode}")
        println("Cyclomatic complexity: ${result.metrics.cyclomaticComplexity}")
        println("Maintainability index: ${result.metrics.maintainabilityIndex}")
        println("Number of issues: ${result.issues.size}")
        println("Number of suggestions: ${result.suggestions.size}")
        
        println("\nIssues:")
        result.issues.forEach { println("- ${it.type}: ${it.message}") }
        
        println("\nSuggestions:")
        result.suggestions.forEach { println("- ${it.type}: ${it.description}") }
        
        // Test direct method calls
        println("\n=== DIRECT METHOD CALLS ===")
        val directSuggestions = analyzer.suggestImprovements(complexCode, ProgrammingLanguage.KOTLIN)
        println("Direct suggestions count: ${directSuggestions.size}")
        directSuggestions.forEach { println("- ${it.type}: ${it.description}") }
    }
}
