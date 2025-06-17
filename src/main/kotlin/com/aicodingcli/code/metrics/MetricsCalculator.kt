package com.aicodingcli.code.metrics

import com.aicodingcli.code.analysis.CodeMetrics
import com.aicodingcli.code.common.ProgrammingLanguage

/**
 * Calculator for code metrics
 */
class MetricsCalculator {

    /**
     * Calculate metrics for given code
     */
    fun calculateMetrics(code: String, language: ProgrammingLanguage): CodeMetrics {
        val lines = code.lines()
        val linesOfCode = calculateLinesOfCode(lines)
        val cyclomaticComplexity = calculateCyclomaticComplexity(code)
        val maintainabilityIndex = calculateMaintainabilityIndex(cyclomaticComplexity, linesOfCode)
        val duplicatedLines = calculateDuplicatedLines(code)

        return CodeMetrics(
            linesOfCode = linesOfCode,
            cyclomaticComplexity = cyclomaticComplexity,
            maintainabilityIndex = maintainabilityIndex,
            testCoverage = null, // Would need external tool integration
            duplicatedLines = duplicatedLines
        )
    }

    private fun calculateLinesOfCode(lines: List<String>): Int {
        var inBlockComment = false
        var count = 0

        for (line in lines) {
            val trimmed = line.trim()

            // Skip empty lines
            if (trimmed.isEmpty()) continue

            // Handle block comments
            if (trimmed.startsWith("/*")) {
                inBlockComment = true
                // Check if comment ends on the same line
                if (trimmed.contains("*/")) {
                    inBlockComment = false
                    // Check if there's code after the comment
                    val afterComment = trimmed.substringAfter("*/").trim()
                    if (afterComment.isNotEmpty()) {
                        count++
                    }
                }
                continue
            }

            if (inBlockComment) {
                if (trimmed.contains("*/")) {
                    inBlockComment = false
                    // Check if there's code after the comment
                    val afterComment = trimmed.substringAfter("*/").trim()
                    if (afterComment.isNotEmpty()) {
                        count++
                    }
                }
                continue
            }

            // Skip single line comments
            if (trimmed.startsWith("//")) continue

            // Skip lines that are only block comment continuation
            if (trimmed.startsWith("*")) continue

            // Check if line has code before inline comment
            val beforeComment = if (trimmed.contains("//")) {
                trimmed.substringBefore("//").trim()
            } else {
                trimmed
            }

            if (beforeComment.isNotEmpty()) {
                count++
            }
        }

        return count
    }

    private fun calculateCyclomaticComplexity(code: String): Int {
        var complexity = 1 // Base complexity

        // Remove comments and strings to avoid false positives
        val cleanCode = removeCommentsAndStrings(code)

        // Count decision points more accurately
        complexity += countDecisionPoints(cleanCode)

        return maxOf(complexity, 1)
    }

    private fun countDecisionPoints(cleanCode: String): Int {
        var points = 0

        // Count if statements (but not else if as separate)
        points += countPattern(cleanCode, "\\bif\\s*\\(")

        // Count while loops
        points += countPattern(cleanCode, "\\bwhile\\s*\\(")

        // Count for loops
        points += countPattern(cleanCode, "\\bfor\\s*\\(")

        // Count when expressions by counting -> arrows, excluding function declarations and lambdas
        val whenArrows = countPattern(cleanCode, "->")
        val functionDeclarations = countPattern(cleanCode, "\\bfun\\s+\\w+")
        val lambdas = countPattern(cleanCode, "\\{.*->")
        points += maxOf(0, whenArrows - functionDeclarations - lambdas)

        // Count catch blocks
        points += countPattern(cleanCode, "\\bcatch\\s*\\(")

        // Count logical operators (but be careful not to double count)
        points += countLogicalOperators(cleanCode)

        return points
    }

    private fun countLogicalOperators(code: String): Int {
        // Count && and || but avoid counting them multiple times in the same expression
        val lines = code.lines()
        var count = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.contains("&&") || trimmed.contains("||")) {
                // Count each line with logical operators as one additional complexity point
                count += 1
            }
        }

        return count
    }

    private fun countPattern(text: String, pattern: String): Int {
        return Regex(pattern).findAll(text).count()
    }

    private fun removeCommentsAndStrings(code: String): String {
        var result = code

        // Remove single line comments
        result = result.replace(Regex("//.*"), "")

        // Remove block comments (simplified)
        result = result.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")

        // Remove string literals (simplified)
        result = result.replace(Regex("\".*?\""), "\"\"")
        result = result.replace(Regex("'.*?'"), "''")

        return result
    }

    private fun calculateMaintainabilityIndex(complexity: Int, linesOfCode: Int): Double {
        // Simplified maintainability index calculation
        // Real formula: MI = 171 - 5.2 * ln(Halstead Volume) - 0.23 * (Cyclomatic Complexity) - 16.2 * ln(Lines of Code)
        
        val complexityPenalty = complexity * 2.0
        val sizePenalty = kotlin.math.ln(linesOfCode.toDouble()) * 5.0
        
        val baseScore = 100.0
        val maintainabilityIndex = baseScore - complexityPenalty - sizePenalty
        
        return maxOf(maintainabilityIndex, 0.0).coerceAtMost(100.0)
    }

    private fun calculateDuplicatedLines(code: String): Int {
        // More accurate duplicate detection
        val lines = code.lines()
            .map { line ->
                // Normalize the line: remove leading/trailing whitespace and comments
                val trimmed = line.trim()
                when {
                    trimmed.isEmpty() -> null
                    trimmed.startsWith("//") -> null
                    trimmed.startsWith("/*") -> null
                    trimmed.startsWith("*") -> null
                    trimmed.contains("//") -> trimmed.substringBefore("//").trim()
                    else -> trimmed
                }
            }
            .filterNotNull()
            .filter { it.isNotEmpty() }

        // Group identical lines and count duplicates
        val lineGroups = lines.groupBy { it }
        var duplicateCount = 0

        for ((line, occurrences) in lineGroups) {
            if (occurrences.size > 1) {
                // Only count meaningful duplicates (not single characters or braces)
                if (line.length > 3 && !line.matches(Regex("[{}();,]*"))) {
                    duplicateCount += occurrences.size - 1
                }
            }
        }

        return duplicateCount
    }


}
