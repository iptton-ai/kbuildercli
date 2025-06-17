package com.aicodingcli.code.common

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgrammingLanguageTest {

    @Test
    fun `should have kotlin language with correct properties`() {
        val kotlin = ProgrammingLanguage.KOTLIN
        
        assertEquals("kotlin", kotlin.name.lowercase())
        assertEquals("kt", kotlin.fileExtension)
        assertTrue(kotlin.supportsClasses)
        assertTrue(kotlin.supportsInterfaces)
    }

    @Test
    fun `should have java language with correct properties`() {
        val java = ProgrammingLanguage.JAVA
        
        assertEquals("java", java.name.lowercase())
        assertEquals("java", java.fileExtension)
        assertTrue(java.supportsClasses)
        assertTrue(java.supportsInterfaces)
    }

    @Test
    fun `should detect language from file extension`() {
        assertEquals(ProgrammingLanguage.KOTLIN, ProgrammingLanguage.fromFileExtension("kt"))
        assertEquals(ProgrammingLanguage.JAVA, ProgrammingLanguage.fromFileExtension("java"))
        assertEquals(ProgrammingLanguage.PYTHON, ProgrammingLanguage.fromFileExtension("py"))
    }

    @Test
    fun `should throw exception for unsupported file extension`() {
        assertThrows<IllegalArgumentException> {
            ProgrammingLanguage.fromFileExtension("xyz")
        }
    }

    @Test
    fun `should detect language from file path`() {
        assertEquals(ProgrammingLanguage.KOTLIN, ProgrammingLanguage.fromFilePath("src/main/kotlin/Test.kt"))
        assertEquals(ProgrammingLanguage.JAVA, ProgrammingLanguage.fromFilePath("/path/to/Test.java"))
        assertEquals(ProgrammingLanguage.PYTHON, ProgrammingLanguage.fromFilePath("script.py"))
    }
}
