package com.aicodingcli

fun main(args: Array<String>) {
    val cli = AiCodingCli()
    cli.run(args)
}

class AiCodingCli {
    fun run(args: Array<String>) {
        if (args.isNotEmpty() && args[0] == "--version") {
            println("0.1.0")
        }
    }
}
