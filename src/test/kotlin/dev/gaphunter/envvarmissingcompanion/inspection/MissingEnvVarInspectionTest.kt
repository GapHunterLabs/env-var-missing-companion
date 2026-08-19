package dev.gaphunter.envvarmissingcompanion.inspection

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * End-to-end: real PSI + real inspection registration + real quick-fix
 * application via `myFixture`, not a direct unit call into
 * [MissingEnvVarInspection]'s internals -- the scanning/matching logic
 * itself is already covered exhaustively by
 * [dev.gaphunter.envvarmissingcompanion.detect.EnvVarReferenceScannerTest]
 * and [dev.gaphunter.envvarmissingcompanion.parse.EnvFileParserTest].
 * This confirms the inspection is actually *wired up* end to end: it
 * fires real warnings and its quick-fix really writes to the real
 * `.env.example` file on disk (via the in-memory test VFS).
 */
class MissingEnvVarInspectionTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(MissingEnvVarInspection::class.java)
    }

    fun `test declared variable produces no warning`() {
        myFixture.addFileToProject(".env.example", "PORT=\n")
        myFixture.configureByText("index.js", "const port = process.env.PORT;")

        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("PORT") == true })
    }

    fun `test undeclared variable produces a warning`() {
        myFixture.addFileToProject(".env.example", "OTHER_VAR=\n")
        myFixture.configureByText("index.js", "const port = process.env.PORT;")

        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("PORT") == true })
    }

    fun `test quick-fix adds the missing variable to the real env-example file`() {
        val envExample = myFixture.addFileToProject(".env.example", "EXISTING=already-here\n")
        myFixture.configureByText("index.js", "const port = process.env.PORT;")

        myFixture.doHighlighting()
        val fix = myFixture.getAllQuickFixes().singleOrNull { it.text.contains("PORT") }
        assertNotNull("expected a quick-fix mentioning PORT", fix)
        myFixture.launchAction(fix as IntentionAction)

        val newText = String(envExample.virtualFile.contentsToByteArray())
        assertTrue(newText.contains("EXISTING=already-here"))
        assertTrue(newText.contains("PORT="))
    }

    fun `test quick-fix creates env-example from scratch when project has none`() {
        myFixture.configureByText("index.js", "const port = process.env.PORT;")

        myFixture.doHighlighting()
        val fix = myFixture.getAllQuickFixes().single { it.text.contains("PORT") }
        myFixture.launchAction(fix)

        val created = myFixture.findFileInTempDir(".env.example")
        assertNotNull("expected .env.example to be created", created)
        val newText = String(created!!.contentsToByteArray())
        assertTrue(newText.contains("PORT="))
    }

    fun `test variable with js double-pipe default still warns but as weak warning`() {
        myFixture.configureByText("index.js", "const port = process.env.PORT || 3000;")

        val highlights = myFixture.doHighlighting()
        val match = highlights.firstOrNull { it.description?.contains("PORT") == true }
        assertNotNull(match)
        assertTrue(match!!.description.contains("default"))
    }

    fun `test variable with python getenv default still gets a quick-fix`() {
        myFixture.configureByText("settings.py", "timeout = os.getenv(\"TIMEOUT\", 30)")

        myFixture.doHighlighting()
        val fix = myFixture.getAllQuickFixes().singleOrNull { it.text.contains("TIMEOUT") }
        assertNotNull("expected a quick-fix even for a variable with a default", fix)
    }

    fun `test plain mention in a comment does not trigger a warning`() {
        myFixture.configureByText("index.js", "// Config values come from process.env at runtime")

        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("not declared") == true })
    }

    fun `test env file itself is never scanned as source`() {
        myFixture.configureByText(".env.example", "process.env.NOT_A_REAL_REFERENCE\n")

        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.none { it.description?.contains("not declared") == true })
    }

    fun `test python os-environ bracket access without default warns`() {
        myFixture.addFileToProject(".env", "OTHER=1\n")
        myFixture.configureByText("app.py", "debug = os.environ[\"DEBUG\"]")

        val highlights = myFixture.doHighlighting()
        val match = highlights.firstOrNull { it.description?.contains("DEBUG") == true }
        assertNotNull(match)
        assertFalse(match!!.description.contains("default"))
    }

    fun `test project with no env files at all still reports undeclared vars honestly`() {
        myFixture.configureByText("index.js", "const secret = process.env.API_SECRET;")

        val highlights = myFixture.doHighlighting()
        assertTrue(highlights.any { it.description?.contains("API_SECRET") == true })
    }
}
