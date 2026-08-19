package dev.gaphunter.envvarmissingcompanion.detect

/**
 * One real access to an environment variable found in source text --
 * `process.env.PORT`, `os.environ["DEBUG"]`, `os.getenv("TIMEOUT", 30)`,
 * etc. Never a bare textual mention (a string literal used as
 * documentation, a comment) -- see [EnvVarReferenceScanner] for the
 * real-access-only matching rules.
 */
data class EnvVarReference(
    /** The variable name exactly as written, e.g. "PORT". */
    val name: String,
    /** 0-based offset into the scanned text where [name] itself starts. */
    val nameStartOffset: Int,
    /** 0-based offset into the scanned text where [name] itself ends (exclusive). */
    val nameEndOffset: Int,
    /**
     * True when the access itself supplies an explicit fallback value in
     * the same expression -- `process.env.PORT || 3000`,
     * `os.environ.get("DEBUG", "false")`, `os.getenv("TIMEOUT", 30)`.
     * A default makes the missing-from-.env case less urgent (the code
     * still runs), so callers use this to pick a weaker severity instead
     * of filtering the finding out entirely -- see README "Why built
     * this way" for the reasoning.
     */
    val hasExplicitDefault: Boolean,
)
