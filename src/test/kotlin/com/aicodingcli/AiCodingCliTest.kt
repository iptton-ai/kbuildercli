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
                  config <subcommand> Manage configuration settings

                Options:
                  --version          Show version information
                  --help             Show this help message
                  --provider <name>  Use specific AI provider (openai, claude, ollama)
                  --model <name>     Use specific model for the AI provider
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

    @Test
    fun `should handle model parameter correctly`() {
        // Arrange
        val cli = AiCodingCli()
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Act - This will fail due to invalid API key, but we can check the output format
            cli.run(arrayOf("test-connection", "--provider", "ollama", "--model", "llama3.2"))

            // Assert
            val output = outputStream.toString().trim()
            // Should show the model being used
            assertTrue(output.contains("Using model: llama3.2") || output.contains("Connection to OLLAMA"))
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should show config help when config command has no arguments`() {
        // Arrange
        val cli = AiCodingCli()
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Act
            cli.run(arrayOf("config"))

            // Assert
            val output = outputStream.toString().trim()
            assertTrue(output.contains("Configuration Management Commands"))
            assertTrue(output.contains("config set"))
            assertTrue(output.contains("config get"))
            assertTrue(output.contains("config list"))
            assertTrue(output.contains("config provider"))
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should handle config list command`() {
        // Arrange
        val cli = AiCodingCli()
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Act
            cli.run(arrayOf("config", "list"))

            // Assert
            val output = outputStream.toString().trim()
            assertTrue(output.contains("Current Configuration"))
            assertTrue(output.contains("Default Provider"))
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should handle invalid config subcommand`() {
        // Arrange
        val cli = AiCodingCli()
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Act
            cli.run(arrayOf("config", "invalid"))

            // Assert
            val output = outputStream.toString().trim()
            assertTrue(output.contains("Unknown config subcommand: invalid"))
            assertTrue(output.contains("Configuration Management Commands"))
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should handle config set with insufficient arguments`() {
        // Arrange
        val cli = AiCodingCli()
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Act
            cli.run(arrayOf("config", "set", "key"))

            // Assert
            val output = outputStream.toString().trim()
            assertTrue(output.contains("Usage: config set <key> <value>"))
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should handle config get with no arguments`() {
        // Arrange
        val cli = AiCodingCli()
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Act
            cli.run(arrayOf("config", "get"))

            // Assert
            val output = outputStream.toString().trim()
            assertTrue(output.contains("Usage: config get <key>"))
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should handle config provider with no arguments`() {
        // Arrange
        val cli = AiCodingCli()
        val outputStream = ByteArrayOutputStream()
        val originalOut = System.out
        System.setOut(PrintStream(outputStream))

        try {
            // Act
            cli.run(arrayOf("config", "provider"))

            // Assert
            val output = outputStream.toString().trim()
            assertTrue(output.contains("Usage: config provider <name>"))
            assertTrue(output.contains("Available providers: openai, claude, ollama"))
        } finally {
            // Restore original System.out
            System.setOut(originalOut)
        }
    }
}
