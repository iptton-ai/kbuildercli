package com.aicodingcli

fun main(args: Array<String>) {
    val cli = AiCodingCli()
    cli.run(args)
}

class AiCodingCli {
    companion object {
        const val VERSION = "0.1.0"
        const val HELP_TEXT = """AI Coding CLI - A command line tool for AI-assisted coding

Usage: ai-coding-cli [OPTIONS]

Options:
  --version    Show version information
  --help       Show this help message"""
    }

    fun run(args: Array<String>) {
        when {
            args.isNotEmpty() && args[0] == "--version" -> printVersion()
            args.isNotEmpty() && args[0] == "--help" -> printHelp()
        }
    }

    private fun printVersion() {
        println(VERSION)
    }

    private fun printHelp() {
        println(HELP_TEXT)
    }
}
