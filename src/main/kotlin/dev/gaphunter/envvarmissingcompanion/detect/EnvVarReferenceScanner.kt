package dev.gaphunter.envvarmissingcompanion.detect

/**
 * Finds real environment-variable *accesses* in JavaScript/TypeScript and
 * Python source text -- plain-text/regex analysis, no PSI (see
 * `build.gradle.kts` for why: Python/JavaScript PSI aren't guaranteed
 * present in every IntelliJ Platform edition this catalog targets).
 *
 * Patterns covered (the two most common ecosystems for this exact
 * "reference an env var by string key" pattern -- see README "Why built
 * this way" for why Java/Kotlin are deliberately out of v0.1 scope,
 * Spring's `@Value`/`application.properties` being the dominant pattern
 * there instead of direct `System.getenv`):
 *
 * - JS/TS: `process.env.VARNAME`, `process.env["VARNAME"]`, `process.env['VARNAME']`
 * - Python: `os.environ["VARNAME"]`, `os.environ['VARNAME']`,
 *   `os.environ.get("VARNAME")`, `os.getenv("VARNAME")`
 *
 * **Real access vs. plain mention:** every pattern requires the literal
 * receiver (`process.env` / `os.environ` / `os.getenv(`) immediately
 * followed by the real access syntax (`.NAME`, `["NAME"]`, or a quoted
 * first argument) -- a bare mention of "process.env" or "os.environ" in a
 * comment or doc string that never actually indexes/calls it produces no
 * match. Known, documented limitation: a comment or string literal whose
 * text happens to *contain* the exact working syntax (e.g. a code example
 * in a README quoted inside a source file's block comment reading
 * `// e.g. process.env.PORT`) is indistinguishable from a real access
 * under plain-text scanning and will still be flagged -- full
 * comment/string-literal stripping would need a real per-language lexer,
 * out of scope for v0.1's "the file already open" surface. In practice
 * this is rare (most such examples live in actual README.md files, which
 * this inspection does not scan as source).
 */
object EnvVarReferenceScanner {

    // --- JavaScript/TypeScript: process.env.NAME ------------------------
    private val JS_DOT = Regex("""process\.env\.([A-Za-z_][A-Za-z0-9_]*)""")

    // --- JavaScript/TypeScript: process.env["NAME"] / process.env['NAME'] ---
    private val JS_BRACKET = Regex(
        """process\.env\[\s*(["'])([A-Za-z_][A-Za-z0-9_]*)\1\s*\]"""
    )

    // --- Python: os.environ["NAME"] / os.environ['NAME'] ---
    private val PY_BRACKET = Regex(
        """os\.environ\[\s*(["'])([A-Za-z_][A-Za-z0-9_]*)\1\s*\]"""
    )

    // --- Python: os.environ.get("NAME"[, default]) ---
    private val PY_ENVIRON_GET = Regex(
        """os\.environ\.get\(\s*(["'])([A-Za-z_][A-Za-z0-9_]*)\1\s*(,[^)]*)?\)"""
    )

    // --- Python: os.getenv("NAME"[, default]) ---
    private val PY_GETENV = Regex(
        """os\.getenv\(\s*(["'])([A-Za-z_][A-Za-z0-9_]*)\1\s*(,[^)]*)?\)"""
    )

    /**
     * A trailing `|| <default>` / `?? <default>` right after a
     * `process.env.X` / `process.env["X"]` access, on the same line, up to
     * the next `;`, `,`, `)` at depth 0, or end of line. Kept intentionally
     * simple (line-scoped) -- a default spanning multiple lines is rare
     * enough in practice to document as a limitation rather than build a
     * real expression parser for.
     */
    private val JS_TRAILING_DEFAULT = Regex("""^\s*(\|\||\?\?)\s*\S""")

    fun scan(text: String): List<EnvVarReference> {
        val results = mutableListOf<EnvVarReference>()

        for (match in JS_DOT.findAll(text)) {
            val nameGroup = match.groups[1]!!
            results += EnvVarReference(
                name = nameGroup.value,
                nameStartOffset = nameGroup.range.first,
                nameEndOffset = nameGroup.range.last + 1,
                hasExplicitDefault = hasJsTrailingDefault(text, match.range.last + 1),
            )
        }

        for (match in JS_BRACKET.findAll(text)) {
            val nameGroup = match.groups[2]!!
            results += EnvVarReference(
                name = nameGroup.value,
                nameStartOffset = nameGroup.range.first,
                nameEndOffset = nameGroup.range.last + 1,
                hasExplicitDefault = hasJsTrailingDefault(text, match.range.last + 1),
            )
        }

        for (match in PY_BRACKET.findAll(text)) {
            val nameGroup = match.groups[2]!!
            results += EnvVarReference(
                name = nameGroup.value,
                nameStartOffset = nameGroup.range.first,
                nameEndOffset = nameGroup.range.last + 1,
                // os.environ["X"] (no .get) never carries a default -- a
                // missing key raises KeyError, so there's no "fallback"
                // syntax to look for here at all.
                hasExplicitDefault = false,
            )
        }

        for (match in PY_ENVIRON_GET.findAll(text)) {
            val nameGroup = match.groups[2]!!
            val defaultGroup = match.groups[3]
            results += EnvVarReference(
                name = nameGroup.value,
                nameStartOffset = nameGroup.range.first,
                nameEndOffset = nameGroup.range.last + 1,
                hasExplicitDefault = defaultGroup != null,
            )
        }

        for (match in PY_GETENV.findAll(text)) {
            val nameGroup = match.groups[2]!!
            val defaultGroup = match.groups[3]
            results += EnvVarReference(
                name = nameGroup.value,
                nameStartOffset = nameGroup.range.first,
                nameEndOffset = nameGroup.range.last + 1,
                hasExplicitDefault = defaultGroup != null,
            )
        }

        return results.sortedBy { it.nameStartOffset }
    }

    private fun hasJsTrailingDefault(text: String, afterMatchOffset: Int): Boolean {
        val lineEnd = text.indexOf('\n', afterMatchOffset).let { if (it == -1) text.length else it }
        val rest = text.substring(afterMatchOffset, lineEnd)
        return JS_TRAILING_DEFAULT.containsMatchIn(rest)
    }
}
