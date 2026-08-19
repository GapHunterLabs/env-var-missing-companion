package dev.gaphunter.envvarmissingcompanion.quickfix

import com.intellij.openapi.vfs.VirtualFile
import java.nio.charset.StandardCharsets

/**
 * Real quick-fix side effect: append `VARNAME=` to the project's
 * `.env.example`, creating it (with a generated-by-plugin header comment)
 * if none exists yet. Must run inside a write action / write command --
 * callers (the quick-fix itself) are responsible for that, this object
 * only does the file I/O. `setBinaryContent` is the same pattern already
 * proven for real file edits in `spreadsheet-companion`'s `XlsxViewerEditor`.
 */
object EnvExampleWriter {

    private const val GENERATED_HEADER =
        "# Created by Env Var Missing Companion -- add real values below.\n"

    /**
     * @param targetDirectory where `.env.example` should live (created here
     *   if it doesn't exist, and if [existingEnvExample] is null).
     * @param existingEnvExample the project's real `.env.example`, if one
     *   was already found by [dev.gaphunter.envvarmissingcompanion.parse.DeclaredEnvVarsLocator]
     *   -- appended to directly when present, so we never create a second,
     *   competing `.env.example` next to one that already exists elsewhere
     *   in the project.
     */
    fun addVariable(
        targetDirectory: VirtualFile,
        existingEnvExample: VirtualFile?,
        varName: String,
    ) {
        val file = existingEnvExample
            ?: targetDirectory.findChild(".env.example")
            ?: targetDirectory.createChildData(EnvExampleWriter, ".env.example")

        val currentText = if (file.length > 0) {
            String(file.contentsToByteArray(), StandardCharsets.UTF_8)
        } else {
            ""
        }

        val alreadyDeclared = currentText.lineSequence()
            .map { it.trim() }
            .any { it == varName || it.startsWith("$varName=") }
        if (alreadyDeclared) return

        val needsHeader = currentText.isBlank()
        val needsLeadingNewline = currentText.isNotEmpty() && !currentText.endsWith("\n")

        val newText = buildString {
            append(currentText)
            if (needsHeader) append(GENERATED_HEADER)
            if (needsLeadingNewline) append('\n')
            append(varName).append("=\n")
        }

        file.setBinaryContent(newText.toByteArray(StandardCharsets.UTF_8))
    }
}
