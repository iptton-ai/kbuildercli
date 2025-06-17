package com.aicodingcli.code.quality

import com.aicodingcli.code.analysis.*
import com.aicodingcli.code.common.ProgrammingLanguage

/**
 * Analyzer for code quality issues and improvements
 */
class QualityAnalyzer {

    /**
     * Detect issues in code
     */
    fun detectIssues(code: String, language: ProgrammingLanguage): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        
        when (language) {
            ProgrammingLanguage.KOTLIN, ProgrammingLanguage.JAVA -> {
                issues.addAll(detectJvmLanguageIssues(code))
            }
            ProgrammingLanguage.PYTHON -> {
                issues.addAll(detectPythonIssues(code))
            }
            else -> {
                // Generic issues for other languages
                issues.addAll(detectGenericIssues(code))
            }
        }
        
        return issues
    }

    /**
     * Suggest improvements for code
     */
    fun suggestImprovements(code: String, language: ProgrammingLanguage): List<Improvement> {
        val suggestions = mutableListOf<Improvement>()
        
        // Performance improvements
        suggestions.addAll(detectPerformanceImprovements(code, language))
        
        // Maintainability improvements
        suggestions.addAll(detectMaintainabilityImprovements(code))
        
        // Readability improvements
        suggestions.addAll(detectReadabilityImprovements(code, language))
        
        return suggestions
    }

    private fun detectJvmLanguageIssues(code: String): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        
        // Check for bad class names
        val badClassNameRegex = Regex("class\\s+([a-z][a-zA-Z0-9]*)")
        badClassNameRegex.findAll(code).forEach { match ->
            issues.add(CodeIssue(
                type = IssueType.NAMING_CONVENTION,
                severity = IssueSeverity.MEDIUM,
                message = "Class name '${match.groupValues[1]}' should start with uppercase letter",
                line = findLineNumber(code, match.value),
                column = null,
                suggestion = "Use PascalCase for class names (e.g., ${match.groupValues[1].replaceFirstChar { it.uppercase() }})"
            ))
        }
        
        // Check for unused variables (simplified)
        val unusedVarRegex = Regex("(val|var)\\s+(\\w+)\\s*=")
        unusedVarRegex.findAll(code).forEach { match ->
            val varName = match.groupValues[2]
            val restOfCode = code.substring(match.range.last + 1)
            if (!restOfCode.contains(varName)) {
                issues.add(CodeIssue(
                    type = IssueType.UNUSED_CODE,
                    severity = IssueSeverity.LOW,
                    message = "Variable '$varName' is declared but never used",
                    line = findLineNumber(code, match.value),
                    column = null,
                    suggestion = "Remove unused variable or use it in the code"
                ))
            }
        }
        
        // Check for magic numbers
        val magicNumberRegex = Regex("\\b([0-9]{2,})\\b")
        magicNumberRegex.findAll(code).forEach { match ->
            val number = match.groupValues[1]
            if (number != "100" && number != "1000") { // Common acceptable numbers
                issues.add(CodeIssue(
                    type = IssueType.CODE_SMELL,
                    severity = IssueSeverity.LOW,
                    message = "Magic number '$number' should be replaced with a named constant",
                    line = findLineNumber(code, match.value),
                    column = null,
                    suggestion = "Define a constant with a meaningful name"
                ))
            }
        }
        
        return issues
    }

    private fun detectPythonIssues(code: String): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        
        // Check for snake_case violations in function names
        val badFunctionNameRegex = Regex("def\\s+([a-z][a-zA-Z0-9]*[A-Z][a-zA-Z0-9]*)")
        badFunctionNameRegex.findAll(code).forEach { match ->
            issues.add(CodeIssue(
                type = IssueType.NAMING_CONVENTION,
                severity = IssueSeverity.MEDIUM,
                message = "Function name '${match.groupValues[1]}' should use snake_case",
                line = findLineNumber(code, match.value),
                column = null,
                suggestion = "Use snake_case for function names"
            ))
        }
        
        return issues
    }

    private fun detectGenericIssues(code: String): List<CodeIssue> {
        val issues = mutableListOf<CodeIssue>()
        
        // Check for very long lines
        code.lines().forEachIndexed { index, line ->
            if (line.length > 120) {
                issues.add(CodeIssue(
                    type = IssueType.CODE_SMELL,
                    severity = IssueSeverity.LOW,
                    message = "Line is too long (${line.length} characters)",
                    line = index + 1,
                    column = null,
                    suggestion = "Break long lines into multiple lines"
                ))
            }
        }
        
        return issues
    }

    private fun detectPerformanceImprovements(code: String, language: ProgrammingLanguage): List<Improvement> {
        val suggestions = mutableListOf<Improvement>()
        
        // Check for string concatenation in loops
        if (code.contains(Regex("\\+\\s*=\\s*.*\\+\\s*"))) {
            suggestions.add(Improvement(
                type = ImprovementType.PERFORMANCE,
                description = "Consider using StringBuilder for string concatenation in loops",
                line = findLineNumber(code, "\\+\\s*=\\s*.*\\+\\s*"),
                priority = ImprovementPriority.MEDIUM
            ))
        }
        
        // Check for inefficient collection operations
        if (code.contains(Regex("list\\.contains\\(.*\\).*for"))) {
            suggestions.add(Improvement(
                type = ImprovementType.PERFORMANCE,
                description = "Consider using Set for frequent contains() operations",
                line = null,
                priority = ImprovementPriority.LOW
            ))
        }
        
        return suggestions
    }

    private fun detectMaintainabilityImprovements(code: String): List<Improvement> {
        val suggestions = mutableListOf<Improvement>()
        
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
        
        // Check for long methods (simplified)
        val methodRegex = Regex("fun\\s+\\w+\\([^)]*\\)\\s*\\{([^}]+)\\}")
        methodRegex.findAll(code).forEach { match ->
            val methodBody = match.groupValues[1]
            val methodLines = methodBody.lines().filter { it.trim().isNotEmpty() }
            if (methodLines.size > 20) {
                suggestions.add(Improvement(
                    type = ImprovementType.MAINTAINABILITY,
                    description = "Method is too long (${methodLines.size} lines). Consider breaking it into smaller methods",
                    line = findLineNumber(code, match.value),
                    priority = ImprovementPriority.MEDIUM
                ))
            }
        }
        
        return suggestions
    }

    private fun detectReadabilityImprovements(code: String, language: ProgrammingLanguage): List<Improvement> {
        val suggestions = mutableListOf<Improvement>()
        
        // Check for missing documentation
        val publicMethodRegex = when (language) {
            ProgrammingLanguage.KOTLIN, ProgrammingLanguage.JAVA -> Regex("(public\\s+)?fun\\s+\\w+")
            ProgrammingLanguage.PYTHON -> Regex("def\\s+\\w+")
            else -> Regex("function\\s+\\w+")
        }
        
        publicMethodRegex.findAll(code).forEach { match ->
            val beforeMethod = code.substring(0, match.range.first)
            val lastLines = beforeMethod.lines().takeLast(3)
            val hasDocumentation = lastLines.any { 
                it.trim().startsWith("/**") || it.trim().startsWith("\"\"\"") || it.trim().startsWith("//")
            }
            
            if (!hasDocumentation) {
                suggestions.add(Improvement(
                    type = ImprovementType.READABILITY,
                    description = "Consider adding documentation for public methods",
                    line = findLineNumber(code, match.value),
                    priority = ImprovementPriority.LOW
                ))
            }
        }
        
        return suggestions
    }

    private fun findLineNumber(code: String, pattern: String): Int? {
        val regex = if (pattern.startsWith("\\")) Regex(pattern) else Regex.escape(pattern).toRegex()
        val lines = code.lines()
        lines.forEachIndexed { index, line ->
            if (regex.containsMatchIn(line)) {
                return index + 1
            }
        }
        return null
    }
}
