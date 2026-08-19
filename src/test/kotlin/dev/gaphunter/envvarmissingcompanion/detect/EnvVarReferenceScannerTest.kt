package dev.gaphunter.envvarmissingcompanion.detect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvVarReferenceScannerTest {

    @Test
    fun `finds process env dot access`() {
        val refs = EnvVarReferenceScanner.scan("""const port = process.env.PORT;""")
        assertEquals(listOf("PORT"), refs.map { it.name })
        assertEquals(false, refs.single().hasExplicitDefault)
    }

    @Test
    fun `finds process env double-quoted bracket access`() {
        val refs = EnvVarReferenceScanner.scan("""const port = process.env["PORT"];""")
        assertEquals(listOf("PORT"), refs.map { it.name })
    }

    @Test
    fun `finds process env single-quoted bracket access`() {
        val refs = EnvVarReferenceScanner.scan("""const port = process.env['PORT'];""")
        assertEquals(listOf("PORT"), refs.map { it.name })
    }

    @Test
    fun `finds python os-environ double-quoted bracket access`() {
        val refs = EnvVarReferenceScanner.scan("""port = os.environ["PORT"]""")
        assertEquals(listOf("PORT"), refs.map { it.name })
    }

    @Test
    fun `finds python os-environ single-quoted bracket access`() {
        val refs = EnvVarReferenceScanner.scan("""port = os.environ['PORT']""")
        assertEquals(listOf("PORT"), refs.map { it.name })
    }

    @Test
    fun `finds python os-environ-get without default`() {
        val refs = EnvVarReferenceScanner.scan("""debug = os.environ.get("DEBUG")""")
        assertEquals(listOf("DEBUG"), refs.map { it.name })
        assertEquals(false, refs.single().hasExplicitDefault)
    }

    @Test
    fun `finds python os-environ-get with default`() {
        val refs = EnvVarReferenceScanner.scan("""debug = os.environ.get("DEBUG", "false")""")
        assertEquals(listOf("DEBUG"), refs.map { it.name })
        assertEquals(true, refs.single().hasExplicitDefault)
    }

    @Test
    fun `finds python os-getenv without default`() {
        val refs = EnvVarReferenceScanner.scan("""timeout = os.getenv("TIMEOUT")""")
        assertEquals(listOf("TIMEOUT"), refs.map { it.name })
        assertEquals(false, refs.single().hasExplicitDefault)
    }

    @Test
    fun `finds python os-getenv with numeric default`() {
        val refs = EnvVarReferenceScanner.scan("""timeout = os.getenv("TIMEOUT", 30)""")
        assertEquals(listOf("TIMEOUT"), refs.map { it.name })
        assertEquals(true, refs.single().hasExplicitDefault)
    }

    @Test
    fun `detects js double-pipe default on same line`() {
        val refs = EnvVarReferenceScanner.scan("""const port = process.env.PORT || 3000;""")
        assertEquals(true, refs.single().hasExplicitDefault)
    }

    @Test
    fun `detects js nullish-coalescing default on same line`() {
        val refs = EnvVarReferenceScanner.scan("""const port = process.env.PORT ?? 3000;""")
        assertEquals(true, refs.single().hasExplicitDefault)
    }

    @Test
    fun `js bracket access with default`() {
        val refs = EnvVarReferenceScanner.scan("""const port = process.env["PORT"] || "3000";""")
        assertEquals(true, refs.single().hasExplicitDefault)
    }

    @Test
    fun `no default when nothing follows on the line`() {
        val refs = EnvVarReferenceScanner.scan("const port = process.env.PORT;\nconsole.log(port);")
        assertEquals(false, refs.single().hasExplicitDefault)
    }

    @Test
    fun `plain mention of process-env without real access does not match`() {
        val refs = EnvVarReferenceScanner.scan("""// Configuration is read from process.env at startup""")
        assertTrue(refs.isEmpty())
    }

    @Test
    fun `plain mention of os-environ without real access does not match`() {
        val refs = EnvVarReferenceScanner.scan("""# os.environ holds all the environment variables""")
        assertTrue(refs.isEmpty())
    }

    @Test
    fun `bare word PORT elsewhere in a string does not match`() {
        val refs = EnvVarReferenceScanner.scan("""const label = "The PORT setting controls the listener";""")
        assertTrue(refs.isEmpty())
    }

    @Test
    fun `finds multiple distinct references in the same file`() {
        val text = """
            const port = process.env.PORT;
            const host = process.env["HOST"];
            const debug = os.environ.get("DEBUG", "false")
        """.trimIndent()
        val refs = EnvVarReferenceScanner.scan(text)
        assertEquals(listOf("PORT", "HOST", "DEBUG"), refs.map { it.name })
    }

    @Test
    fun `offsets point at the real variable name text`() {
        val text = """const port = process.env.PORT;"""
        val ref = EnvVarReferenceScanner.scan(text).single()
        assertEquals("PORT", text.substring(ref.nameStartOffset, ref.nameEndOffset))
    }

    @Test
    fun `offsets point at the real variable name text for bracket access`() {
        val text = """const port = process.env["PORT"];"""
        val ref = EnvVarReferenceScanner.scan(text).single()
        assertEquals("PORT", text.substring(ref.nameStartOffset, ref.nameEndOffset))
    }

    @Test
    fun `does not match unrelated dotted property access`() {
        val refs = EnvVarReferenceScanner.scan("""const value = someOther.env.PORT;""")
        assertTrue(refs.isEmpty())
    }

    @Test
    fun `does not match os-environ-get without quotes as first arg`() {
        // A dynamic key (variable, not a literal) can't be cross-checked
        // against a static .env file -- correctly produces no match.
        val refs = EnvVarReferenceScanner.scan("""val = os.environ.get(some_variable)""")
        assertTrue(refs.isEmpty())
    }
}
