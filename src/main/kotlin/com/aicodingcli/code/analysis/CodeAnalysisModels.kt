package com.aicodingcli.code.analysis

import com.aicodingcli.code.common.ProgrammingLanguage

/**
 * Code metrics for analysis results
 */
data class CodeMetrics(
    val linesOfCode: Int,
    val cyclomaticComplexity: Int,
    val maintainabilityIndex: Double,
    val testCoverage: Double?,
    val duplicatedLines: Int
)

/**
 * Types of code issues
 */
enum class IssueType {
    SYNTAX_ERROR,
    LOGIC_ERROR,
    PERFORMANCE,
    SECURITY,
    CODE_SMELL,
    NAMING_CONVENTION,
    UNUSED_CODE
}

/**
 * Severity levels for issues
 */
enum class IssueSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Code issue detected during analysis
 */
data class CodeIssue(
    val type: IssueType,
    val severity: IssueSeverity,
    val message: String,
    val line: Int?,
    val column: Int?,
    val suggestion: String?
)

/**
 * Types of improvements
 */
enum class ImprovementType {
    PERFORMANCE,
    MAINTAINABILITY,
    READABILITY,
    SECURITY,
    TESTING
}

/**
 * Priority levels for improvements
 */
enum class ImprovementPriority {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Improvement suggestion
 */
data class Improvement(
    val type: ImprovementType,
    val description: String,
    val line: Int?,
    val priority: ImprovementPriority = ImprovementPriority.MEDIUM
)

/**
 * Types of dependencies
 */
enum class DependencyType {
    INTERNAL,
    EXTERNAL,
    SYSTEM
}

/**
 * Dependency scopes
 */
enum class DependencyScope {
    COMPILE,
    RUNTIME,
    TEST,
    PROVIDED
}

/**
 * Dependency information
 */
data class Dependency(
    val name: String,
    val version: String?,
    val type: DependencyType,
    val scope: DependencyScope
)

/**
 * Complete code analysis result for a single file
 */
data class CodeAnalysisResult(
    val filePath: String,
    val language: ProgrammingLanguage,
    val metrics: CodeMetrics,
    val issues: List<CodeIssue>,
    val suggestions: List<Improvement>,
    val dependencies: List<Dependency>
)

/**
 * Project-level analysis result
 */
data class ProjectAnalysisResult(
    val projectPath: String,
    val fileResults: List<CodeAnalysisResult>,
    val overallMetrics: CodeMetrics,
    val summary: AnalysisSummary
)

/**
 * Summary of analysis results
 */
data class AnalysisSummary(
    val totalFiles: Int,
    val totalIssues: Int,
    val criticalIssues: Int,
    val averageComplexity: Double,
    val overallMaintainabilityIndex: Double
)
