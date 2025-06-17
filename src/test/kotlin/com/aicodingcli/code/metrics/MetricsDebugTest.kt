package com.aicodingcli.code.metrics

import com.aicodingcli.code.common.ProgrammingLanguage
import org.junit.jupiter.api.Test

class MetricsDebugTest {

    private val calculator = MetricsCalculator()

    @Test
    fun `debug cyclomatic complexity calculation`() {
        val conditionalCode = """
            class ConditionalClass {
                fun processInput(input: String?): String {
                    if (input == null) {
                        return "null"
                    }
                    
                    if (input.isEmpty()) {
                        return "empty"
                    }
                    
                    return when {
                        input.length > 10 -> "long"
                        input.length > 5 -> "medium"
                        else -> "short"
                    }
                }
            }
        """.trimIndent()

        val metrics = calculator.calculateMetrics(conditionalCode, ProgrammingLanguage.KOTLIN)
        
        println("=== CYCLOMATIC COMPLEXITY DEBUG ===")
        println("Code:")
        println(conditionalCode)
        println()
        println("Calculated complexity: ${metrics.cyclomaticComplexity}")
        println("Expected complexity: 6 (2 if statements + 3 when branches + 1 base)")
        println("Lines of code: ${metrics.linesOfCode}")
        println("Duplicated lines: ${metrics.duplicatedLines}")
    }

    @Test
    fun `debug lines of code calculation`() {
        val codeWithComments = """
            // This is a comment
            class TestClass {
                // Another comment
                
                fun method1() {
                    // Method comment
                    val x = 1 // Inline comment
                    return x
                }
                
                /*
                 * Block comment
                 * Multiple lines
                 */
                fun method2() {
                    val y = 2
                    return y
                }
            }
        """.trimIndent()

        val metrics = calculator.calculateMetrics(codeWithComments, ProgrammingLanguage.KOTLIN)
        
        println("=== LINES OF CODE DEBUG ===")
        println("Code:")
        println(codeWithComments)
        println()
        println("Calculated LOC: ${metrics.linesOfCode}")
        println("Expected LOC: 7 (class declaration, 2 method declarations, 4 statements)")
        
        // Let's also debug line by line
        val lines = codeWithComments.lines()
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            val isCode = when {
                trimmed.isEmpty() -> false
                trimmed.startsWith("//") -> false
                trimmed.startsWith("/*") -> false
                trimmed.startsWith("*") -> false
                else -> true
            }
            println("Line ${index + 1}: '$trimmed' -> ${if (isCode) "CODE" else "SKIP"}")
        }
    }
}
