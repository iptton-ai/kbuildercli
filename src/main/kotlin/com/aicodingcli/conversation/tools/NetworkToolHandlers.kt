package com.aicodingcli.conversation.tools

import com.aicodingcli.conversation.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder

/**
 * Handler for web-search tool
 */
class WebSearchHandler : BaseToolHandler("web-search") {
    
    override suspend fun executeInternal(parameters: Map<String, String>, workingDirectory: String): ToolResult {
        val query = parameters["query"]!!
        val numResults = parameters["num_results"]?.toIntOrNull() ?: 5
        
        // Mock implementation - in real scenario this would use actual search API
        val results = (1..numResults).map { i ->
            "Result $i: Mock search result for '$query'\n" +
            "URL: https://example.com/result$i\n" +
            "Snippet: This is a mock search result snippet for the query '$query'.\n"
        }.joinToString("\n")
        
        return ToolResult.success(
            output = results,
            metadata = mapOf(
                "query" to query,
                "num_results" to numResults.toString(),
                "mock" to "true"
            )
        )
    }
    
    override fun validateParameters(parameters: Map<String, String>): ValidationResult {
        val errors = checkRequiredParameters(parameters, listOf("query")).toMutableList()
        
        val numResults = parameters["num_results"]
        if (numResults != null) {
            try {
                val num = numResults.toInt()
                if (num < 1 || num > 10) {
                    errors.add("num_results must be between 1 and 10")
                }
            } catch (e: NumberFormatException) {
                errors.add("num_results must be a valid number")
            }
        }
        
        return if (errors.isEmpty()) ValidationResult.valid() else ValidationResult.invalid(errors)
    }
    
    override fun getMetadata(): ToolMetadata {
        return ToolMetadata(
            name = toolName,
            description = "Search the web for information",
            parameters = listOf(
                ToolParameter("query", "string", "The search query to send", required = true),
                ToolParameter("num_results", "integer", "Number of results to return (1-10)", required = false)
            ),
            category = "network"
        )
    }
}

/**
 * Handler for web-fetch tool
 */
class WebFetchHandler : BaseToolHandler("web-fetch") {
    
    override suspend fun executeInternal(parameters: Map<String, String>, workingDirectory: String): ToolResult {
        val url = parameters["url"]!!
        
        return withContext(Dispatchers.IO) {
            try {
                // Validate URL
                URL(url)
                
                // Mock implementation - in real scenario this would fetch actual web content
                val mockContent = """
                    # Mock Web Content
                    
                    This is mock content fetched from: $url
                    
                    ## Sample Content
                    - Mock paragraph 1
                    - Mock paragraph 2
                    - Mock paragraph 3
                    
                    **Note**: This is a mock implementation for testing purposes.
                """.trimIndent()
                
                ToolResult.success(
                    output = mockContent,
                    metadata = mapOf(
                        "url" to url,
                        "content_length" to mockContent.length.toString(),
                        "mock" to "true"
                    )
                )
            } catch (e: Exception) {
                ToolResult.failure("Failed to fetch URL: ${e.message}")
            }
        }
    }
    
    override fun validateParameters(parameters: Map<String, String>): ValidationResult {
        val errors = checkRequiredParameters(parameters, listOf("url")).toMutableList()
        
        val url = parameters["url"]
        if (url != null) {
            try {
                URL(url)
            } catch (e: Exception) {
                errors.add("Invalid URL format: $url")
            }
        }
        
        return if (errors.isEmpty()) ValidationResult.valid() else ValidationResult.invalid(errors)
    }
    
    override fun getMetadata(): ToolMetadata {
        return ToolMetadata(
            name = toolName,
            description = "Fetches data from a webpage and converts it into Markdown",
            parameters = listOf(
                ToolParameter("url", "string", "The URL to fetch", required = true)
            ),
            category = "network"
        )
    }
}

/**
 * Handler for open-browser tool
 */
class OpenBrowserHandler : BaseToolHandler("open-browser") {
    
    override suspend fun executeInternal(parameters: Map<String, String>, workingDirectory: String): ToolResult {
        val url = parameters["url"]!!
        
        return withContext(Dispatchers.IO) {
            try {
                // Validate URL
                URL(url)
                
                // Mock implementation - in real scenario this would open actual browser
                ToolResult.success(
                    output = "Browser opened successfully for URL: $url",
                    metadata = mapOf(
                        "url" to url,
                        "mock" to "true"
                    )
                )
            } catch (e: Exception) {
                ToolResult.failure("Failed to open browser: ${e.message}")
            }
        }
    }
    
    override fun validateParameters(parameters: Map<String, String>): ValidationResult {
        val errors = checkRequiredParameters(parameters, listOf("url")).toMutableList()
        
        val url = parameters["url"]
        if (url != null) {
            try {
                URL(url)
            } catch (e: Exception) {
                errors.add("Invalid URL format: $url")
            }
        }
        
        return if (errors.isEmpty()) ValidationResult.valid() else ValidationResult.invalid(errors)
    }
    
    override fun getMetadata(): ToolMetadata {
        return ToolMetadata(
            name = toolName,
            description = "Open a URL in the default browser",
            parameters = listOf(
                ToolParameter("url", "string", "The URL to open in the browser", required = true)
            ),
            category = "network"
        )
    }
}
