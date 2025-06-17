package com.aicodingcli.code

import com.aicodingcli.code.analysis.DefaultCodeAnalyzer
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectAnalysisDebugTest {

    @TempDir
    lateinit var tempDir: File

    private val analyzer = DefaultCodeAnalyzer()

    @Test
    fun `debug project analysis error`() = runBlocking {
        // Create multiple test files
        val file1 = File(tempDir, "Class1.kt")
        file1.writeText("""
            class Class1 {
                fun method1(): String = "test"
            }
        """.trimIndent())
        
        val file2 = File(tempDir, "Class2.kt")
        file2.writeText("""
            class Class2 {
                fun method2(): Int = 42
            }
        """.trimIndent())

        try {
            val result = analyzer.analyzeProject(tempDir.absolutePath)
            
            println("=== PROJECT ANALYSIS DEBUG ===")
            println("Project path: ${result.projectPath}")
            println("Files analyzed: ${result.fileResults.size}")
            
            result.fileResults.forEach { fileResult ->
                println("File: ${fileResult.filePath}")
                println("  LOC: ${fileResult.metrics.linesOfCode}")
                println("  Complexity: ${fileResult.metrics.cyclomaticComplexity}")
                println("  Maintainability: ${fileResult.metrics.maintainabilityIndex}")
            }
            
            println("Overall metrics:")
            println("  Total LOC: ${result.overallMetrics.linesOfCode}")
            println("  Avg Complexity: ${result.overallMetrics.cyclomaticComplexity}")
            println("  Avg Maintainability: ${result.overallMetrics.maintainabilityIndex}")
            
            println("Summary:")
            println("  Total Files: ${result.summary.totalFiles}")
            println("  Average Complexity: ${result.summary.averageComplexity}")
            println("  Overall Maintainability: ${result.summary.overallMaintainabilityIndex}")
            
        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }
}
