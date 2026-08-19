package dev.gaphunter.envvarmissingcompanion.parse

import org.junit.Assert.assertEquals
import org.junit.Test

class EnvFileParserTest {

    @Test
    fun `parses simple KEY equals VALUE lines`() {
        val text = """
            PORT=3000
            DEBUG=true
        """.trimIndent()
        assertEquals(setOf("PORT", "DEBUG"), EnvFileParser.parseKeys(text))
    }

    @Test
    fun `ignores blank lines and comments`() {
        val text = """
            # This is a comment
            PORT=3000

            # Another comment
            DEBUG=true
        """.trimIndent()
        assertEquals(setOf("PORT", "DEBUG"), EnvFileParser.parseKeys(text))
    }

    @Test
    fun `strips leading export keyword`() {
        val text = "export DATABASE_URL=postgres://localhost/db"
        assertEquals(setOf("DATABASE_URL"), EnvFileParser.parseKeys(text))
    }

    @Test
    fun `key with no value is still recognized`() {
        val text = "API_KEY="
        assertEquals(setOf("API_KEY"), EnvFileParser.parseKeys(text))
    }

    @Test
    fun `empty file produces empty set`() {
        assertEquals(emptySet<String>(), EnvFileParser.parseKeys(""))
    }

    @Test
    fun `duplicate keys are deduplicated`() {
        val text = """
            PORT=3000
            PORT=4000
        """.trimIndent()
        assertEquals(setOf("PORT"), EnvFileParser.parseKeys(text))
    }
}
