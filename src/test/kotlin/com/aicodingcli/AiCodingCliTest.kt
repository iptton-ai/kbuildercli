package com.aicodingcli

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
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

                Usage: ai-coding-cli [OPTIONS]

                Options:
                  --version    Show version information
                  --help       Show this help message
            """.trimIndent()
            assertEquals(expectedHelp, output)
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }
}
