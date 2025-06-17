package com.aicodingcli

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class AnalyzeCommandTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `should handle analyze file command`() {
        // Create a test file
        val testFile = File(tempDir, "TestClass.kt")
        testFile.writeText("""
            class TestClass {
                fun simpleMethod(): String {
                    return "Hello, World!"
                }
            }
        """.trimIndent())

        // Capture output
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Run the CLI command
            val cli = AiCodingCli()
            cli.run(arrayOf("analyze", "file", testFile.absolutePath))

            val output = outputStream.toString()
            
            // Verify output contains expected elements
            assertTrue(output.contains("Code Analysis Results"))
            assertTrue(output.contains("Language: Kotlin"))
            assertTrue(output.contains("Lines of Code:"))
            assertTrue(output.contains("Cyclomatic Complexity:"))
            assertTrue(output.contains("Maintainability Index:"))
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should handle analyze metrics command`() {
        // Create a test file
        val testFile = File(tempDir, "SimpleClass.kt")
        testFile.writeText("""
            class SimpleClass {
                fun add(a: Int, b: Int): Int {
                    return a + b
                }
            }
        """.trimIndent())

        // Capture output
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Run the CLI command
            val cli = AiCodingCli()
            cli.run(arrayOf("analyze", "metrics", testFile.absolutePath))

            val output = outputStream.toString()
            
            // Verify output contains metrics
            assertTrue(output.contains("Code Metrics:"))
            assertTrue(output.contains("Lines of Code:"))
            assertTrue(output.contains("Cyclomatic Complexity:"))
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should handle analyze issues command`() {
        // Create a test file with issues
        val testFile = File(tempDir, "ProblematicClass.kt")
        testFile.writeText("""
            class badClassName {
                fun method() {
                    val unusedVariable = "test"
                    println("Hello")
                }
            }
        """.trimIndent())

        // Capture output
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Run the CLI command
            val cli = AiCodingCli()
            cli.run(arrayOf("analyze", "issues", testFile.absolutePath))

            val output = outputStream.toString()
            
            // Verify output contains issues or "No issues found"
            assertTrue(output.contains("Code Issues") || output.contains("No issues found"))
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should handle analyze project command`() {
        // Create multiple test files
        val file1 = File(tempDir, "Class1.kt")
        file1.writeText("class Class1")
        
        val file2 = File(tempDir, "Class2.kt")
        file2.writeText("class Class2")

        // Capture output
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Run the CLI command
            val cli = AiCodingCli()
            cli.run(arrayOf("analyze", "project", tempDir.absolutePath))

            val output = outputStream.toString()
            
            // Verify output contains project analysis
            assertTrue(output.contains("Project Analysis Results"))
            assertTrue(output.contains("Files Analyzed:"))
            assertTrue(output.contains("Overall Metrics:"))
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should handle analyze help command`() {
        // Capture output
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Run the CLI command
            val cli = AiCodingCli()
            cli.run(arrayOf("analyze"))

            val output = outputStream.toString()
            
            // Verify help is displayed
            assertTrue(output.contains("Code Analysis Commands:"))
            assertTrue(output.contains("analyze file"))
            assertTrue(output.contains("analyze project"))
            assertTrue(output.contains("analyze metrics"))
            assertTrue(output.contains("analyze issues"))
        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should handle json format output`() {
        // Create a test file
        val testFile = File(tempDir, "JsonTest.kt")
        testFile.writeText("""
            class JsonTest {
                fun test(): String = "test"
            }
        """.trimIndent())

        // Capture output
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Run the CLI command with JSON format
            val cli = AiCodingCli()
            cli.run(arrayOf("analyze", "metrics", testFile.absolutePath, "--format", "json"))

            val output = outputStream.toString()
            
            // Verify JSON output
            assertTrue(output.contains("{"))
            assertTrue(output.contains("linesOfCode"))
            assertTrue(output.contains("cyclomaticComplexity"))
            assertTrue(output.contains("}"))
        } finally {
            System.setOut(originalOut)
        }
    }
}
