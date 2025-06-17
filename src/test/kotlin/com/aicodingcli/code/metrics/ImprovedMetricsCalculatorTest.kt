package com.aicodingcli.code.metrics

import com.aicodingcli.code.common.ProgrammingLanguage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ImprovedMetricsCalculatorTest {

    private val calculator = MetricsCalculator()

    @Test
    fun `should calculate accurate cyclomatic complexity for simple method`() {
        val simpleCode = """
            class SimpleClass {
                fun simpleMethod(): String {
                    return "Hello"
                }
            }
        """.trimIndent()

        val metrics = calculator.calculateMetrics(simpleCode, ProgrammingLanguage.KOTLIN)
        
        // Simple method should have complexity of 1
        assertEquals(1, metrics.cyclomaticComplexity)
    }

    @Test
    fun `should calculate accurate cyclomatic complexity for method with conditions`() {
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
        
        // Should count: 2 if statements + 3 when branches = 6 total complexity
        assertEquals(6, metrics.cyclomaticComplexity)
    }

    @Test
    fun `should calculate accurate cyclomatic complexity for loops`() {
        val loopCode = """
            class LoopClass {
                fun processItems(items: List<String>): List<String> {
                    val result = mutableListOf<String>()
                    
                    for (item in items) {
                        if (item.isNotEmpty()) {
                            result.add(item.uppercase())
                        }
                    }
                    
                    while (result.size > 10) {
                        result.removeAt(result.size - 1)
                    }
                    
                    return result
                }
            }
        """.trimIndent()

        val metrics = calculator.calculateMetrics(loopCode, ProgrammingLanguage.KOTLIN)
        
        // Should count: 1 for + 1 if + 1 while = 4 total complexity
        assertEquals(4, metrics.cyclomaticComplexity)
    }

    @Test
    fun `should detect actual duplicate lines accurately`() {
        val duplicateCode = """
            class DuplicateClass {
                fun method1() {
                    println("Hello World")
                    val x = 42
                    println("Debug info")
                }
                
                fun method2() {
                    println("Hello World")  // Duplicate
                    val y = 24
                    println("Debug info")   // Duplicate
                }
                
                fun method3() {
                    println("Hello World")  // Duplicate
                    val z = 84
                }
            }
        """.trimIndent()

        val metrics = calculator.calculateMetrics(duplicateCode, ProgrammingLanguage.KOTLIN)
        
        // Should detect 2 duplicate lines: "println("Hello World")" appears 3 times
        // and "println("Debug info")" appears 2 times
        assertTrue(metrics.duplicatedLines >= 2)
    }

    @Test
    fun `should calculate maintainability index based on complexity and size`() {
        val simpleCode = """
            class SimpleClass {
                fun add(a: Int, b: Int): Int = a + b
            }
        """.trimIndent()

        val complexCode = """
            class ComplexClass {
                fun complexMethod(input: String): String {
                    if (input.isEmpty()) {
                        if (input.isBlank()) {
                            return "blank"
                        } else {
                            return "empty"
                        }
                    } else {
                        if (input.length > 100) {
                            if (input.contains("error")) {
                                return "error"
                            } else {
                                return "long"
                            }
                        } else {
                            return "normal"
                        }
                    }
                }
            }
        """.trimIndent()

        val simpleMetrics = calculator.calculateMetrics(simpleCode, ProgrammingLanguage.KOTLIN)
        val complexMetrics = calculator.calculateMetrics(complexCode, ProgrammingLanguage.KOTLIN)
        
        // Simple code should have higher maintainability than complex code
        assertTrue(simpleMetrics.maintainabilityIndex > complexMetrics.maintainabilityIndex)
        
        // Both should be within reasonable range (0-100)
        assertTrue(simpleMetrics.maintainabilityIndex in 0.0..100.0)
        assertTrue(complexMetrics.maintainabilityIndex in 0.0..100.0)
    }

    @Test
    fun `should count lines of code excluding comments and empty lines`() {
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
        
        // Should count only actual code lines, not comments or empty lines
        // Expected: class declaration, 2 method declarations, 4 statements = 7 lines
        assertEquals(7, metrics.linesOfCode)
    }

    @Test
    fun `should handle edge cases gracefully`() {
        val emptyCode = ""
        val onlyComments = """
            // Just comments
            /* Block comment */
        """.trimIndent()
        
        val emptyMetrics = calculator.calculateMetrics(emptyCode, ProgrammingLanguage.KOTLIN)
        val commentMetrics = calculator.calculateMetrics(onlyComments, ProgrammingLanguage.KOTLIN)
        
        assertEquals(0, emptyMetrics.linesOfCode)
        assertEquals(1, emptyMetrics.cyclomaticComplexity) // Base complexity
        assertEquals(0, commentMetrics.linesOfCode)
        assertEquals(1, commentMetrics.cyclomaticComplexity) // Base complexity
    }

    @Test
    fun `should calculate complexity for different programming languages`() {
        val kotlinCode = """
            fun kotlinMethod(x: Int): String {
                return if (x > 0) "positive" else "negative"
            }
        """.trimIndent()

        val javaCode = """
            public String javaMethod(int x) {
                if (x > 0) {
                    return "positive";
                } else {
                    return "negative";
                }
            }
        """.trimIndent()

        val kotlinMetrics = calculator.calculateMetrics(kotlinCode, ProgrammingLanguage.KOTLIN)
        val javaMetrics = calculator.calculateMetrics(javaCode, ProgrammingLanguage.JAVA)
        
        // Both should detect the if-else condition (complexity = 2)
        assertEquals(2, kotlinMetrics.cyclomaticComplexity)
        assertEquals(2, javaMetrics.cyclomaticComplexity)
    }
}
