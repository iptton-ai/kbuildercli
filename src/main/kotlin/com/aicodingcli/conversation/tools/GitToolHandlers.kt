package com.aicodingcli.conversation.tools

import com.aicodingcli.conversation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Handler for github-api tool
 */
class GitHubApiHandler : BaseToolHandler("github-api") {
    
    override suspend fun executeInternal(parameters: Map<String, String>, workingDirectory: String): ToolResult {
        val path = parameters["path"]!!
        val method = parameters["method"] ?: "GET"
        val summary = parameters["summary"]
        
        return withContext(Dispatchers.IO) {
            try {
                // Mock implementation - in real scenario this would make actual GitHub API calls
                val mockResponse = when {
                    path.contains("/repos/") && path.contains("/issues") -> {
                        """
                        issues:
                        - number: 1
                          title: "Sample Issue"
                          state: "open"
                          body: "This is a sample issue"
                        - number: 2
                          title: "Another Issue"
                          state: "closed"
                          body: "This is another sample issue"
                        """.trimIndent()
                    }
                    path.contains("/repos/") && path.contains("/pulls") -> {
                        """
                        pulls:
                        - number: 1
                          title: "Sample Pull Request"
                          state: "open"
                          body: "This is a sample pull request"
                        """.trimIndent()
                    }
                    path.contains("/repos/") && path.contains("/commits") -> {
                        """
                        commits:
                        - sha: "abc123"
                          message: "Sample commit message"
                          author: "developer@example.com"
                        """.trimIndent()
                    }
                    else -> {
                        """
                        response:
                          message: "Mock GitHub API response for $method $path"
                          status: "success"
                        """.trimIndent()
                    }
                }
                
                ToolResult.success(
                    output = mockResponse,
                    metadata = mapOf(
                        "path" to path,
                        "method" to method,
                        "mock" to "true",
                        "summary" to (summary ?: "GitHub API call")
                    )
                )
            } catch (e: Exception) {
                ToolResult.failure("GitHub API call failed: ${e.message}")
            }
        }
    }
    
    override fun validateParameters(parameters: Map<String, String>): ValidationResult {
        val errors = checkRequiredParameters(parameters, listOf("path")).toMutableList()
        
        val method = parameters["method"]
        if (method != null && method !in listOf("GET", "POST", "PATCH", "PUT")) {
            errors.add("method must be one of: GET, POST, PATCH, PUT")
        }
        
        return if (errors.isEmpty()) ValidationResult.valid() else ValidationResult.invalid(errors)
    }
    
    override fun getMetadata(): ToolMetadata {
        return ToolMetadata(
            name = toolName,
            description = "Make GitHub API calls",
            parameters = listOf(
                ToolParameter("path", "string", "GitHub API path", required = true),
                ToolParameter("method", "string", "HTTP method (GET, POST, PATCH, PUT)", required = false),
                ToolParameter("summary", "string", "Summary of what this API call will do", required = false),
                ToolParameter("data", "object", "Data to send as query params or JSON body", required = false),
                ToolParameter("details", "boolean", "Include all fields in response", required = false)
            ),
            category = "git"
        )
    }
}

/**
 * Handler for git operations (mock implementation)
 */
class GitOperationsHandler : BaseToolHandler("git-operations") {
    
    override suspend fun executeInternal(parameters: Map<String, String>, workingDirectory: String): ToolResult {
        val operation = parameters["operation"]!!
        val args = parameters["args"] ?: ""
        
        return withContext(Dispatchers.IO) {
            try {
                val workingDir = File(workingDirectory)
                
                // Mock implementation - in real scenario this would execute actual git commands
                val mockOutput = when (operation) {
                    "status" -> {
                        """
                        On branch main
                        Your branch is up to date with 'origin/main'.
                        
                        Changes not staged for commit:
                          modified:   src/main/kotlin/Example.kt
                        
                        Untracked files:
                          src/main/kotlin/NewFile.kt
                        """.trimIndent()
                    }
                    "add" -> "Mock: Added files to staging area: $args"
                    "commit" -> "Mock: Committed changes with message: $args"
                    "push" -> "Mock: Pushed changes to remote repository"
                    "pull" -> "Mock: Pulled latest changes from remote repository"
                    "branch" -> {
                        """
                        * main
                          feature-branch
                          develop
                        """.trimIndent()
                    }
                    "log" -> {
                        """
                        commit abc123def456 (HEAD -> main)
                        Author: Developer <dev@example.com>
                        Date: Mon Jan 1 12:00:00 2024 +0000
                        
                            Sample commit message
                        """.trimIndent()
                    }
                    else -> "Mock: Executed git $operation $args"
                }
                
                ToolResult.success(
                    output = mockOutput,
                    metadata = mapOf(
                        "operation" to operation,
                        "args" to args,
                        "working_directory" to workingDirectory,
                        "mock" to "true"
                    )
                )
            } catch (e: Exception) {
                ToolResult.failure("Git operation failed: ${e.message}")
            }
        }
    }
    
    override fun validateParameters(parameters: Map<String, String>): ValidationResult {
        val errors = checkRequiredParameters(parameters, listOf("operation")).toMutableList()
        
        val operation = parameters["operation"]
        val validOperations = listOf("status", "add", "commit", "push", "pull", "branch", "log", "diff", "checkout")
        if (operation != null && operation !in validOperations) {
            errors.add("operation must be one of: ${validOperations.joinToString(", ")}")
        }
        
        return if (errors.isEmpty()) ValidationResult.valid() else ValidationResult.invalid(errors)
    }
    
    override fun getMetadata(): ToolMetadata {
        return ToolMetadata(
            name = toolName,
            description = "Execute Git operations in the repository",
            parameters = listOf(
                ToolParameter("operation", "string", "Git operation to perform", required = true),
                ToolParameter("args", "string", "Arguments for the git operation", required = false)
            ),
            category = "git"
        )
    }
}
