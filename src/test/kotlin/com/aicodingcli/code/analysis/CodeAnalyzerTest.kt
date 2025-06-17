package com.aicodingcli.code.analysis

import com.aicodingcli.code.common.ProgrammingLanguage
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CodeAnalyzerTest {

    @TempDir
    lateinit var tempDir: File

    private val analyzer = DefaultCodeAnalyzer()

    @Test
    fun `should analyze simple kotlin file`() = runBlocking {
        val kotlinFile = createTempFile("SimpleClass.kt", """
            package com.example
            
            class SimpleClass {
                fun simpleMethod(): String {
                    return "Hello, World!"
                }
            }
        """.trimIndent())
        
        val result = analyzer.analyzeFile(kotlinFile.absolutePath)
        
        assertEquals(kotlinFile.absolutePath, result.filePath)
        assertEquals(ProgrammingLanguage.KOTLIN, result.language)
        assertNotNull(result.metrics)
        assertTrue(result.metrics.linesOfCode > 0)
        assertTrue(result.metrics.cyclomaticComplexity >= 1)
    }

    @Test
    fun `should detect issues in problematic code`() = runBlocking {
        val problematicFile = createTempFile("ProblematicClass.kt", """
            class badClassName {
                fun VeryLongMethodNameThatViolatesNamingConventions() {
                    var x = 1
                    var y = 2
                    var z = 3
                    // Unused variables
                }
            }
        """.trimIndent())
        
        val result = analyzer.analyzeFile(problematicFile.absolutePath)
        
        assertTrue(result.issues.isNotEmpty())
        assertTrue(result.issues.any { it.type == IssueType.NAMING_CONVENTION })
    }

    @Test
    fun `should suggest improvements for complex code`() = runBlocking {
        val complexFile = createTempFile("ComplexClass.kt", """
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
        """.trimIndent())
        
        val result = analyzer.analyzeFile(complexFile.absolutePath)
        
        assertTrue(result.suggestions.isNotEmpty())
        assertTrue(result.metrics.cyclomaticComplexity > 3)
    }

    @Test
    fun `should throw exception for non-existent file`() {
        assertThrows<IllegalArgumentException> {
            runBlocking {
                analyzer.analyzeFile("/non/existent/file.kt")
            }
        }
    }

    @Test
    fun `should analyze project directory`() = runBlocking {
        // Create multiple files in temp directory
        createTempFile("Class1.kt", "class Class1")
        createTempFile("Class2.kt", "class Class2")
        
        val result = analyzer.analyzeProject(tempDir.absolutePath)
        
        assertEquals(tempDir.absolutePath, result.projectPath)
        assertTrue(result.fileResults.size >= 2)
        assertNotNull(result.overallMetrics)
    }

    @Test
    fun `should detect specific issue types`() = runBlocking {
        val codeWithIssues = """
            class TestClass {
                fun method1() {
                    val unusedVariable = "test"
                    println("Hello")
                }
                
                fun method2() {
                    // Duplicate logic
                    println("Hello")
                }
            }
        """.trimIndent()
        
        val issues = analyzer.detectIssues(codeWithIssues, ProgrammingLanguage.KOTLIN)
        
        assertTrue(issues.isNotEmpty())
        // Should detect unused variable
        assertTrue(issues.any { it.type == IssueType.UNUSED_CODE })
    }

    @Test
    fun `should suggest improvements for specific code patterns`() = runBlocking {
        val improvableCode = """
            class StringProcessor {
                fun processStrings(strings: List<String>): String {
                    var result = ""
                    for (str in strings) {
                        result += str + " "
                    }
                    return result
                }
            }
        """.trimIndent()
        
        val suggestions = analyzer.suggestImprovements(improvableCode, ProgrammingLanguage.KOTLIN)
        
        assertTrue(suggestions.isNotEmpty())
        // Should suggest using StringBuilder
        assertTrue(suggestions.any { it.type == ImprovementType.PERFORMANCE })
    }

    private fun createTempFile(name: String, content: String): File {
        val file = File(tempDir, name)
        file.writeText(content)
        return file
    }
}
