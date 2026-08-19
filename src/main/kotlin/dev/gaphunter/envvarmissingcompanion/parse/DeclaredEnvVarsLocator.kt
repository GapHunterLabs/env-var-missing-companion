package dev.gaphunter.envvarmissingcompanion.parse

import com.intellij.openapi.vfs.VirtualFile

/**
 * Finds the real `.env`/`.env.example`/`.env.local`/etc. file(s) of a
 * project and extracts the union of every declared variable name across
 * all of them -- v0.1 deliberately does not distinguish which specific
 * `.env*` file declares which variable (that's the multi-environment
 * "which vars are required in .env.dev vs .env.prod" feature staged for
 * a possible Pro tier, see README "Why built this way" / CHANGELOG).
 *
 * Search scope: the project's content roots, and -- if the currently
 * open file lives in a different directory than any content root's own
 * `.env*` files -- that file's own parent directory too, so a monorepo
 * subproject with its own `.env` next to a nested `package.json` still
 * gets picked up.
 */
object DeclaredEnvVarsLocator {

    /** Matches `.env`, `.env.example`, `.env.local`, `.env.production`, etc. -- never `.environment` or unrelated dotfiles. */
    private val ENV_FILE_NAME = Regex("""^\.env(\..+)?$""")

    fun isEnvFile(fileName: String): Boolean = ENV_FILE_NAME.matches(fileName)

    /**
     * @param searchDirectories candidate directories to scan (non-recursive,
     *   top-level files only -- e.g. project content roots plus the open
     *   file's own parent directory).
     */
    fun findEnvFiles(searchDirectories: Collection<VirtualFile>): List<VirtualFile> {
        val seen = LinkedHashSet<VirtualFile>()
        for (dir in searchDirectories) {
            if (!dir.isValid || !dir.isDirectory) continue
            for (child in dir.children) {
                if (!child.isDirectory && isEnvFile(child.name)) {
                    seen.add(child)
                }
            }
        }
        return seen.toList()
    }

    /** Union of every declared variable name across all given `.env*` files. */
    fun declaredVarNames(envFiles: Collection<VirtualFile>): Set<String> {
        val declared = mutableSetOf<String>()
        for (file in envFiles) {
            if (!file.isValid) continue
            val text = runCatching { String(file.contentsToByteArray(), file.charset) }.getOrNull() ?: continue
            declared += EnvFileParser.parseKeys(text)
        }
        return declared
    }
}
