package com.aicodingcli.code.analysis

import com.aicodingcli.code.common.ProgrammingLanguage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue

class CodeAnalysisModelsTest {

    @Test
    fun `should create code metrics with valid values`() {
        val metrics = CodeMetrics(
            linesOfCode = 100,
            cyclomaticComplexity = 5,
            maintainabilityIndex = 85.5,
            testCoverage = 90.0,
            duplicatedLines = 10
        )
        
        assertEquals(100, metrics.linesOfCode)
        assertEquals(5, metrics.cyclomaticComplexity)
        assertEquals(85.5, metrics.maintainabilityIndex)
        assertEquals(90.0, metrics.testCoverage)
        assertEquals(10, metrics.duplicatedLines)
    }

    @Test
    fun `should create code issue with all properties`() {
        val issue = CodeIssue(
            type = IssueType.CODE_SMELL,
            severity = IssueSeverity.MEDIUM,
            message = "Method too long",
            line = 42,
            column = 10,
            suggestion = "Consider breaking this method into smaller methods"
        )
        
        assertEquals(IssueType.CODE_SMELL, issue.type)
        assertEquals(IssueSeverity.MEDIUM, issue.severity)
        assertEquals("Method too long", issue.message)
        assertEquals(42, issue.line)
        assertEquals(10, issue.column)
        assertEquals("Consider breaking this method into smaller methods", issue.suggestion)
    }

    @Test
    fun `should create code analysis result`() {
        val metrics = CodeMetrics(50, 3, 90.0, 85.0, 0)
        val issues = listOf(
            CodeIssue(IssueType.NAMING_CONVENTION, IssueSeverity.LOW, "Variable name should be camelCase", 10, 5, null)
        )
        val suggestions = listOf(
            Improvement(ImprovementType.PERFORMANCE, "Use StringBuilder for string concatenation", 15)
        )
        
        val result = CodeAnalysisResult(
            filePath = "src/main/kotlin/Test.kt",
            language = ProgrammingLanguage.KOTLIN,
            metrics = metrics,
            issues = issues,
            suggestions = suggestions,
            dependencies = emptyList()
        )
        
        assertEquals("src/main/kotlin/Test.kt", result.filePath)
        assertEquals(ProgrammingLanguage.KOTLIN, result.language)
        assertEquals(metrics, result.metrics)
        assertEquals(1, result.issues.size)
        assertEquals(1, result.suggestions.size)
        assertTrue(result.dependencies.isEmpty())
    }

    @Test
    fun `should create improvement suggestion`() {
        val improvement = Improvement(
            type = ImprovementType.MAINTAINABILITY,
            description = "Extract this logic into a separate method",
            line = 25,
            priority = ImprovementPriority.HIGH
        )
        
        assertEquals(ImprovementType.MAINTAINABILITY, improvement.type)
        assertEquals("Extract this logic into a separate method", improvement.description)
        assertEquals(25, improvement.line)
        assertEquals(ImprovementPriority.HIGH, improvement.priority)
    }

    @Test
    fun `should create dependency information`() {
        val dependency = Dependency(
            name = "kotlinx.coroutines",
            version = "1.6.4",
            type = DependencyType.EXTERNAL,
            scope = DependencyScope.COMPILE
        )
        
        assertEquals("kotlinx.coroutines", dependency.name)
        assertEquals("1.6.4", dependency.version)
        assertEquals(DependencyType.EXTERNAL, dependency.type)
        assertEquals(DependencyScope.COMPILE, dependency.scope)
    }
}
