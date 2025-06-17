package com.aicodingcli.code.common

/**
 * Supported programming languages for code analysis
 */
enum class ProgrammingLanguage(
    val displayName: String,
    val fileExtension: String,
    val supportsClasses: Boolean,
    val supportsInterfaces: Boolean,
    val supportsPackages: Boolean = true
) {
    KOTLIN("Kotlin", "kt", true, true, true),
    JAVA("Java", "java", true, true, true),
    PYTHON("Python", "py", true, false, true),
    JAVASCRIPT("JavaScript", "js", false, false, false),
    TYPESCRIPT("TypeScript", "ts", true, true, true);

    companion object {
        /**
         * Get programming language from file extension
         */
        fun fromFileExtension(extension: String): ProgrammingLanguage {
            return values().find { it.fileExtension == extension }
                ?: throw IllegalArgumentException("Unsupported file extension: $extension")
        }

        /**
         * Get programming language from file path
         */
        fun fromFilePath(filePath: String): ProgrammingLanguage {
            val extension = filePath.substringAfterLast('.', "")
            return fromFileExtension(extension)
        }
    }
}
