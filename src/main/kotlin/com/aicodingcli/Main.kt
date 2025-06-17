package com.aicodingcli

fun main(args: Array<String>) {
    val cli = AiCodingCli()
    cli.run(args)
}

class AiCodingCli {
    companion object {
        const val VERSION = "0.1.0"
    }

    fun run(args: Array<String>) {
        when {
            args.isNotEmpty() && args[0] == "--version" -> printVersion()
        }
    }

    private fun printVersion() {
        println(VERSION)
    }
}
