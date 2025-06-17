package com.aicodingcli.code.analysis

import com.aicodingcli.code.common.ProgrammingLanguage
import com.aicodingcli.code.metrics.MetricsCalculator
import com.aicodingcli.code.quality.QualityAnalyzer
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

    private val metricsCalculator = MetricsCalculator()
    private val qualityAnalyzer = QualityAnalyzer()

    override suspend fun analyzeFile(filePath: String): CodeAnalysisResult {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: $filePath")
        }

        val content = file.readText()
        val language = ProgrammingLanguage.fromFilePath(filePath)

        val metrics = metricsCalculator.calculateMetrics(content, language)
        val issues = qualityAnalyzer.detectIssues(content, language)
        val suggestions = qualityAnalyzer.suggestImprovements(content, language)
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
        return qualityAnalyzer.detectIssues(code, language)
    }

    override suspend fun suggestImprovements(code: String, language: ProgrammingLanguage): List<Improvement> {
        return qualityAnalyzer.suggestImprovements(code, language)
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
        val avgComplexity = fileResults.map { it.metrics.cyclomaticComplexity.toDouble() }.average().toInt()
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
        val avgComplexity = fileResults.map { it.metrics.cyclomaticComplexity.toDouble() }.average()
        val avgMaintainability = fileResults.map { it.metrics.maintainabilityIndex }.average()

        return AnalysisSummary(
            totalFiles = fileResults.size,
            totalIssues = totalIssues,
            criticalIssues = criticalIssues,
            averageComplexity = avgComplexity,
            overallMaintainabilityIndex = avgMaintainability
        )
    }


}
