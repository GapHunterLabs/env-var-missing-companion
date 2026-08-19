package dev.gaphunter.envvarmissingcompanion.parse

/**
 * Plain-text `KEY=VALUE` parsing, no PSI -- adapted from
 * `env-diff-companion`'s `EnvFileParser` (same catalog, same file
 * format). A blank line or a line starting with `#` (after trimming) is
 * a comment/separator, not a key. `export KEY=VALUE` (a real, common
 * shell-sourced `.env` convention) is also recognized -- the leading
 * `export ` is stripped before the key is read, so it doesn't get
 * counted as part of the key text itself.
 */
object EnvFileParser {

    fun parseKeys(text: String): Set<String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.removePrefix("export ").trim() }
            .mapNotNull { line -> line.substringBefore('=').trim().takeIf { it.isNotEmpty() } }
            .toSet()
}
