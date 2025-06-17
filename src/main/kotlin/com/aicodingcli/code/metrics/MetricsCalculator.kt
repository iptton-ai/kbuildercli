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
        return lines.count { line ->
            val trimmed = line.trim()
            trimmed.isNotEmpty() && 
            !trimmed.startsWith("//") && 
            !trimmed.startsWith("/*") && 
            !trimmed.startsWith("*")
        }
    }

    private fun calculateCyclomaticComplexity(code: String): Int {
        val complexityKeywords = listOf(
            "if", "else if", "while", "for", "when", "catch", 
            "&&", "||", "?:", "?."
        )
        
        var complexity = 1 // Base complexity
        
        complexityKeywords.forEach { keyword ->
            complexity += countOccurrences(code, keyword)
        }
        
        // Count case statements in when expressions
        complexity += countOccurrences(code, "->") - countOccurrences(code, "fun")
        
        return maxOf(complexity, 1)
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
        // Simplified duplicate detection
        val lines = code.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("//") }
        
        val lineGroups = lines.groupBy { it }
        return lineGroups.values.sumOf { group ->
            if (group.size > 1) group.size - 1 else 0
        }
    }

    private fun countOccurrences(text: String, pattern: String): Int {
        return text.split(pattern).size - 1
    }
}
