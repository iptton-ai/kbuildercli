package com.aicodingcli.code.quality

import com.aicodingcli.code.analysis.ImprovementType
import com.aicodingcli.code.common.ProgrammingLanguage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

class QualityAnalyzerTest {

    private val analyzer = QualityAnalyzer()

    @Test
    fun `should suggest improvements for complex nested conditions`() {
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
        
        val suggestions = analyzer.suggestImprovements(complexCode, ProgrammingLanguage.KOTLIN)
        
        println("Number of suggestions: ${suggestions.size}")
        suggestions.forEach { println("- ${it.type}: ${it.description}") }
        
        assertTrue(suggestions.isNotEmpty())
        assertTrue(suggestions.any { it.type == ImprovementType.MAINTAINABILITY })
    }
}
