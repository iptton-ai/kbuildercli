package com.aicodingcli

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class AiCodingCliTest {

    @Test
    fun `should print version when --version argument is provided`() {
        // Arrange
        val cli = AiCodingCli()
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Act
            cli.run(arrayOf("--version"))

            // Assert
            val output = outputStream.toString().trim()
            assertEquals("0.1.0", output)
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should print help when --help argument is provided`() {
        // Arrange
        val cli = AiCodingCli()
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Act
            cli.run(arrayOf("--help"))

            // Assert
            val output = outputStream.toString().trim()
            val expectedHelp = """
                AI Coding CLI - A command line tool for AI-assisted coding

                Usage: ai-coding-cli [COMMAND] [OPTIONS]

                Commands:
                  test-connection    Test connection to AI service
                  ask <message>      Ask AI a question

                Options:
                  --version          Show version information
                  --help             Show this help message
            """.trimIndent()
            assertEquals(expectedHelp, output)
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should print help when no arguments provided`() {
        // Arrange
        val cli = AiCodingCli()
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Act
            cli.run(arrayOf())

            // Assert
            val output = outputStream.toString().trim()
            assertTrue(output.contains("AI Coding CLI - A command line tool for AI-assisted coding"))
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should show error for unknown command`() {
        // Arrange
        val cli = AiCodingCli()
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Act
            cli.run(arrayOf("unknown-command"))

            // Assert
            val output = outputStream.toString().trim()
            assertTrue(output.contains("Unknown command: unknown-command"))
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }
}
