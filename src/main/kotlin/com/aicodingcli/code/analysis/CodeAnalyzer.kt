package com.aicodingcli.code.analysis

import com.aicodingcli.code.common.ProgrammingLanguage
import java.io.File

/**
 * Interface for code analysis functionality
 */
interface CodeAnalyzer {
    /**
     * Analyze a single file
     */
    suspend fun analyzeFile(filePath: String): CodeAnalysisResult

    /**
     * Analyze an entire project
     */
    suspend fun analyzeProject(projectPath: String): ProjectAnalysisResult

    /**
     * Detect issues in code
     */
    suspend fun detectIssues(code: String, language: ProgrammingLanguage): List<CodeIssue>

    /**
     * Suggest improvements for code
     */
    suspend fun suggestImprovements(code: String, language: ProgrammingLanguage): List<Improvement>
}

/**
 * Default implementation of CodeAnalyzer
 */
class DefaultCodeAnalyzer : CodeAnalyzer {

    override suspend fun analyzeFile(filePath: String): CodeAnalysisResult {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: $filePath")
        }

        val content = file.readText()
        val language = ProgrammingLanguage.fromFilePath(filePath)
        
        val metrics = calculateMetrics(content, language)
        val issues = detectIssues(content, language)
        val suggestions = suggestImprovements(content, language)
        val dependencies = extractDependencies(content, language)

        return CodeAnalysisResult(
            filePath = filePath,
            language = language,
            metrics = metrics,
            issues = issues,
            suggestions = suggestions,
            dependencies = dependencies
        )
    }

    override suspend fun analyzeProject(projectPath: String): ProjectAnalysisResult {
        val projectDir = File(projectPath)
        if (!projectDir.exists() || !projectDir.isDirectory) {
            throw IllegalArgumentException("Project directory does not exist: $projectPath")
        }

        val sourceFiles = findSourceFiles(projectDir)
        val fileResults = sourceFiles.map { analyzeFile(it.absolutePath) }
        
        val overallMetrics = calculateOverallMetrics(fileResults)
        val summary = createSummary(fileResults)

        return ProjectAnalysisResult(
            projectPath = projectPath,
            fileResults = fileResults,
            overallMetrics = overallMetrics,
            summary = summary
        )
    }

    override suspend fun detectIssues(code: String, language: ProgrammingLanguage): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()

        // Basic naming convention checks
        if (language == ProgrammingLanguage.KOTLIN || language == ProgrammingLanguage.JAVA) {
            // Check for bad class names
            if (code.contains(Regex("class\\s+[a-z]"))) {
                issues.add(CodeIssue(
                    type = IssueType.NAMING_CONVENTION,
                    severity = IssueSeverity.MEDIUM,
                    message = "Class names should start with uppercase letter",
                    line = findLineNumber(code, "class\\s+[a-z]"),
                    column = null,
                    suggestion = "Use PascalCase for class names"
                ))
            }

            // Check for unused variables (simple pattern)
            if (code.contains(Regex("val\\s+\\w+\\s*=.*\\n(?!.*\\1)"))) {
                issues.add(CodeIssue(
                    type = IssueType.UNUSED_CODE,
                    severity = IssueSeverity.LOW,
                    message = "Unused variable detected",
                    line = null,
                    column = null,
                    suggestion = "Remove unused variables"
                ))
            }
        }

        return issues
    }

    override suspend fun suggestImprovements(code: String, language: ProgrammingLanguage): List<Improvement> {
        val suggestions = mutableListOf<Improvement>()

        // Check for string concatenation in loops
        if (code.contains(Regex("\\+\\s*=\\s*.*\\+\\s*"))) {
            suggestions.add(Improvement(
                type = ImprovementType.PERFORMANCE,
                description = "Consider using StringBuilder for string concatenation",
                line = findLineNumber(code, "\\+\\s*=\\s*.*\\+\\s*"),
                priority = ImprovementPriority.MEDIUM
            ))
        }

        // Check for complex nested conditions
        val nestedIfCount = code.split("if").size - 1
        if (nestedIfCount >= 3) {
            suggestions.add(Improvement(
                type = ImprovementType.MAINTAINABILITY,
                description = "Consider extracting complex conditional logic into separate methods",
                line = null,
                priority = ImprovementPriority.HIGH
            ))
        }

        return suggestions
    }

    private fun calculateMetrics(code: String, language: ProgrammingLanguage): CodeMetrics {
        val lines = code.lines()
        val linesOfCode = lines.filter { it.trim().isNotEmpty() && !it.trim().startsWith("//") }.size
        
        // Simple cyclomatic complexity calculation
        val complexityKeywords = listOf("if", "else", "while", "for", "when", "catch", "&&", "||")
        val cyclomaticComplexity = complexityKeywords.sumOf { keyword ->
            code.split(keyword).size - 1
        } + 1 // Base complexity

        // Mock maintainability index calculation
        val maintainabilityIndex = when {
            cyclomaticComplexity <= 5 -> 90.0
            cyclomaticComplexity <= 10 -> 75.0
            else -> 60.0
        }

        return CodeMetrics(
            linesOfCode = linesOfCode,
            cyclomaticComplexity = cyclomaticComplexity,
            maintainabilityIndex = maintainabilityIndex,
            testCoverage = null, // Would need external tool integration
            duplicatedLines = 0 // Simplified for now
        )
    }

    private fun extractDependencies(code: String, language: ProgrammingLanguage): List<Dependency> {
        // Simplified dependency extraction
        return emptyList()
    }

    private fun findSourceFiles(directory: File): List<File> {
        val supportedExtensions = setOf("kt", "java", "py", "js", "ts")
        return directory.walkTopDown()
            .filter { it.isFile && it.extension in supportedExtensions }
            .toList()
    }

    private fun calculateOverallMetrics(fileResults: List<CodeAnalysisResult>): CodeMetrics {
        if (fileResults.isEmpty()) {
            return CodeMetrics(0, 0, 0.0, null, 0)
        }

        val totalLoc = fileResults.sumOf { it.metrics.linesOfCode }
        val avgComplexity = fileResults.map { it.metrics.cyclomaticComplexity }.average().toInt()
        val avgMaintainability = fileResults.map { it.metrics.maintainabilityIndex }.average()

        return CodeMetrics(
            linesOfCode = totalLoc,
            cyclomaticComplexity = avgComplexity,
            maintainabilityIndex = avgMaintainability,
            testCoverage = null,
            duplicatedLines = fileResults.sumOf { it.metrics.duplicatedLines }
        )
    }

    private fun createSummary(fileResults: List<CodeAnalysisResult>): AnalysisSummary {
        val totalIssues = fileResults.sumOf { it.issues.size }
        val criticalIssues = fileResults.sumOf { result ->
            result.issues.count { it.severity == IssueSeverity.CRITICAL }
        }
        val avgComplexity = fileResults.map { it.metrics.cyclomaticComplexity }.average()
        val avgMaintainability = fileResults.map { it.metrics.maintainabilityIndex }.average()

        return AnalysisSummary(
            totalFiles = fileResults.size,
            totalIssues = totalIssues,
            criticalIssues = criticalIssues,
            averageComplexity = avgComplexity,
            overallMaintainabilityIndex = avgMaintainability
        )
    }

    private fun findLineNumber(code: String, pattern: String): Int? {
        val regex = Regex(pattern)
        val lines = code.lines()
        lines.forEachIndexed { index, line ->
            if (regex.containsMatchIn(line)) {
                return index + 1
            }
        }
        return null
    }
}
