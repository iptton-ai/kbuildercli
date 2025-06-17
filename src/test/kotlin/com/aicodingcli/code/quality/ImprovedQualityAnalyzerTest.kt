package com.aicodingcli.code.quality

import com.aicodingcli.code.analysis.IssueType
import com.aicodingcli.code.analysis.IssueSeverity
import com.aicodingcli.code.analysis.ImprovementType
import com.aicodingcli.code.common.ProgrammingLanguage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ImprovedQualityAnalyzerTest {

    private val analyzer = QualityAnalyzer()

    @Test
    fun `should not report duplicate magic number issues`() {
        val codeWithMagicNumbers = """
            class Calculator {
                fun calculate(x: Int): Int {
                    val result1 = x * 10
                    val result2 = x + 10
                    val result3 = x - 10
                    return result1 + result2 + result3
                }
            }
        """.trimIndent()

        val issues = analyzer.detectIssues(codeWithMagicNumbers, ProgrammingLanguage.KOTLIN)
        
        // Should only report magic number "10" once, not three times
        val magicNumberIssues = issues.filter { it.message.contains("Magic number '10'") }
        assertEquals(1, magicNumberIssues.size, "Should report magic number '10' only once")
    }

    @Test
    fun `should detect different types of naming violations`() {
        val badNamingCode = """
            class badClassName {
                val CONSTANT_value = 42
                var snake_case_var = "test"
                
                fun BadMethodName() {
                    val x = 1  // Too short variable name
                }
                
                fun method_with_underscores() {
                    // Bad method naming
                }
            }
        """.trimIndent()

        val issues = analyzer.detectIssues(badNamingCode, ProgrammingLanguage.KOTLIN)
        
        // Should detect various naming issues
        assertTrue(issues.any { it.type == IssueType.NAMING_CONVENTION && it.message.contains("class") })
        assertTrue(issues.any { it.type == IssueType.NAMING_CONVENTION && it.message.contains("method") })
        assertTrue(issues.any { it.type == IssueType.NAMING_CONVENTION && it.message.contains("variable") })
    }

    @Test
    fun `should detect security vulnerabilities`() {
        val vulnerableCode = """
            class UserService {
                fun authenticateUser(username: String, password: String): Boolean {
                    val query = "SELECT * FROM users WHERE username = '${'$'}username' AND password = '${'$'}password'"
                    // SQL injection vulnerability
                    return executeQuery(query)
                }

                fun generateRandomNumber(): Int {
                    return Math.random().toInt()  // Weak random number generation
                }

                private fun executeQuery(query: String): Boolean = true
            }
        """.trimIndent()

        val issues = analyzer.detectIssues(vulnerableCode, ProgrammingLanguage.KOTLIN)
        
        // Should detect security issues
        assertTrue(issues.any { it.type == IssueType.SECURITY })
    }

    @Test
    fun `should detect performance issues`() {
        val performanceCode = """
            class DataProcessor {
                fun processData(items: List<String>): List<String> {
                    val result = mutableListOf<String>()
                    
                    for (item in items) {
                        if (items.contains(item.uppercase())) {  // Inefficient contains check
                            result.add(item)
                        }
                    }
                    
                    return result
                }
                
                fun concatenateStrings(strings: List<String>): String {
                    var result = ""
                    for (str in strings) {
                        result += str  // Inefficient string concatenation
                    }
                    return result
                }
            }
        """.trimIndent()

        val issues = analyzer.detectIssues(performanceCode, ProgrammingLanguage.KOTLIN)
        
        // Should detect performance issues
        assertTrue(issues.any { it.type == IssueType.PERFORMANCE })
    }

    @Test
    fun `should suggest better error handling`() {
        val poorErrorHandlingCode = """
            import java.io.File

            class FileProcessor {
                fun readFile(filename: String): String {
                    try {
                        return File(filename).readText()
                    } catch (e: Exception) {
                        return ""  // Poor error handling
                    }
                }

                fun processData(data: String?) {
                    val length = data!!.length  // Dangerous null assertion
                    println("Length: ${'$'}length")
                }
            }
        """.trimIndent()

        val suggestions = analyzer.suggestImprovements(poorErrorHandlingCode, ProgrammingLanguage.KOTLIN)
        
        // Should suggest better error handling
        assertTrue(suggestions.any { it.type == ImprovementType.SECURITY })
    }

    @Test
    fun `should detect code smells`() {
        val smellCode = """
            class LargeClass {
                var field1: String = ""
                var field2: String = ""
                var field3: String = ""
                var field4: String = ""
                var field5: String = ""
                var field6: String = ""
                var field7: String = ""
                var field8: String = ""
                var field9: String = ""
                var field10: String = ""
                
                fun longMethod() {
                    println("Line 1")
                    println("Line 2")
                    println("Line 3")
                    println("Line 4")
                    println("Line 5")
                    println("Line 6")
                    println("Line 7")
                    println("Line 8")
                    println("Line 9")
                    println("Line 10")
                    println("Line 11")
                    println("Line 12")
                    println("Line 13")
                    println("Line 14")
                    println("Line 15")
                    println("Line 16")
                    println("Line 17")
                    println("Line 18")
                    println("Line 19")
                    println("Line 20")
                    println("Line 21")
                    println("Line 22")
                    println("Line 23")
                    println("Line 24")
                    println("Line 25")
                }
            }
        """.trimIndent()

        val issues = analyzer.detectIssues(smellCode, ProgrammingLanguage.KOTLIN)
        val suggestions = analyzer.suggestImprovements(smellCode, ProgrammingLanguage.KOTLIN)
        
        // Should detect code smells
        assertTrue(issues.any { it.type == IssueType.CODE_SMELL } || 
                  suggestions.any { it.type == ImprovementType.MAINTAINABILITY })
    }

    @Test
    fun `should provide specific and actionable suggestions`() {
        val improvableCode = """
            data class User(val name: String?, val email: String?, val age: Int)

            class UserValidator {
                fun validateUser(user: User): Boolean {
                    if (user.name == null || user.name.isEmpty()) {
                        return false
                    }
                    if (user.email == null || user.email.isEmpty()) {
                        return false
                    }
                    if (user.age < 0 || user.age > 150) {
                        return false
                    }
                    return true
                }
            }
        """.trimIndent()

        val suggestions = analyzer.suggestImprovements(improvableCode, ProgrammingLanguage.KOTLIN)
        
        // Should provide specific suggestions
        assertTrue(suggestions.isNotEmpty())
        suggestions.forEach { suggestion ->
            assertFalse(suggestion.description.isBlank(), "Suggestion should have meaningful description")
            assertTrue(suggestion.description.length > 10, "Suggestion should be detailed enough")
        }
    }

    @Test
    fun `should handle different programming languages appropriately`() {
        val javaCode = """
            public class JavaClass {
                private String field_name;  // Snake case in Java (bad)
                
                public void methodName() {
                    String localVariable = "test";
                }
            }
        """.trimIndent()

        val pythonCode = """
            class PythonClass:
                def methodName(self):  # CamelCase in Python (bad)
                    local_variable = "test"
        """.trimIndent()

        val javaIssues = analyzer.detectIssues(javaCode, ProgrammingLanguage.JAVA)
        val pythonIssues = analyzer.detectIssues(pythonCode, ProgrammingLanguage.PYTHON)
        
        // Should apply language-specific rules
        assertTrue(javaIssues.any { it.message.contains("camelCase") || it.message.contains("naming") })
        assertTrue(pythonIssues.any { it.message.contains("snake_case") || it.message.contains("naming") })
    }
}
