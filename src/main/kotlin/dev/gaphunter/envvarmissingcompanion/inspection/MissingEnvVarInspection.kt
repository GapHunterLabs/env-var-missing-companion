package dev.gaphunter.envvarmissingcompanion.inspection

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dev.gaphunter.envvarmissingcompanion.detect.EnvVarReferenceScanner
import dev.gaphunter.envvarmissingcompanion.parse.DeclaredEnvVarsLocator
import dev.gaphunter.envvarmissingcompanion.quickfix.AddVariableToEnvExampleFix
import dev.gaphunter.envvarmissingcompanion.review.ReviewPrompt

/**
 * Cross-checks every real environment-variable access found in the
 * currently open file (via [EnvVarReferenceScanner]) against the
 * project's real `.env`/`.env.example`/etc. files (via
 * [DeclaredEnvVarsLocator]) and flags any variable that's used but never
 * declared anywhere.
 *
 * **Scope, deliberate (see README "Why built this way"):** only the file
 * open in the editor is scanned, not the whole project on every
 * keystroke -- same "heavy computation off the hot path" principle
 * already applied catalog-wide, just applied at the scope-selection
 * level instead of threading: a whole-project
 * scan cheap enough to run inline on every inspection pass would need
 * either a persistent index (real infrastructure, out of v0.1 scope) or
 * re-scanning every file in the project on every keystroke (the exact
 * cost this design avoids). "The file already open" is also the real,
 * common use case: a developer adding a new `process.env.X` reference
 * wants to know *right there* if it's undeclared.
 *
 * Runs via [checkFile] (whole-file text scan) rather than
 * [buildVisitor] (per-PSI-node visitor) because detection is plain-text
 * regex over the document, not a PSI walk of a specific language grammar
 * -- see `build.gradle.kts` for why no JS/Python PSI dependency is taken.
 * This means the inspection is registered without a `language` filter in
 * `plugin.xml` (applies to any file type), and explicitly skips files
 * that are themselves `.env*` (nothing to scan there) or binary/huge.
 */
class MissingEnvVarInspection : LocalInspectionTool() {

    companion object {
        /** Files larger than this are skipped -- avoids pathological regex cost on generated/minified files. */
        const val MAX_FILE_LENGTH = 500_000
    }

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        val virtualFile = file.virtualFile ?: return null
        if (DeclaredEnvVarsLocator.isEnvFile(virtualFile.name)) return null

        val text = file.text
        if (text.length > MAX_FILE_LENGTH) return null

        val references = EnvVarReferenceScanner.scan(text)
        if (references.isEmpty()) return null

        val declared = declaredVarNames(file.project, file)
        val problems = mutableListOf<ProblemDescriptor>()

        for (ref in references) {
            if (ref.name in declared) continue

            val anchor = leafElementAt(file, ref.nameStartOffset) ?: continue
            val anchorStart = anchor.textRange.startOffset
            // TextRange here is relative to `anchor`, not to the whole
            // file -- confirmed via javap against the real platform jar
            // (InspectionManager.createProblemDescriptor(PsiElement,
            // TextRange, String, ProblemHighlightType, boolean,
            // LocalQuickFix...)) instead of guessing at the signature.
            val relativeRange = TextRange(ref.nameStartOffset - anchorStart, ref.nameEndOffset - anchorStart)
            if (relativeRange.startOffset < 0 || relativeRange.endOffset > anchor.textLength) continue

            val severity = if (ref.hasExplicitDefault) {
                ProblemHighlightType.WEAK_WARNING
            } else {
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            }
            val message = if (ref.hasExplicitDefault) {
                "Env var '${ref.name}' is not declared in any .env file (has a default in code, so this is less urgent)"
            } else {
                "Env var '${ref.name}' is used but not declared in any .env file"
            }

            problems += manager.createProblemDescriptor(
                anchor,
                relativeRange,
                message,
                severity,
                isOnTheFly,
                AddVariableToEnvExampleFix(ref.name),
            )

            val lineNumber = file.viewProvider.document?.getLineNumber(ref.nameStartOffset) ?: -1
            ReviewPrompt.recordHit(file.project, "${virtualFile.path}:$lineNumber:${ref.name}")
        }

        return if (problems.isEmpty()) null else problems.toTypedArray()
    }

    /**
     * Finds the .env(.example, .local, etc.) file(s) that apply to
     * [file]: the project's content roots, plus [file]'s own parent
     * directory (so a
     * monorepo subproject with its own `.env` next to a nested
     * `package.json` is still picked up even if it's not a separate
     * content root).
     */
    private fun declaredVarNames(project: Project, file: PsiFile): Set<String> {
        val virtualFile = file.virtualFile ?: return emptySet()
        val searchDirs = LinkedHashSet<VirtualFile>()

        virtualFile.parent?.let { searchDirs.add(it) }
        for (root in ProjectRootManager.getInstance(project).contentRoots) {
            searchDirs.add(root)
        }

        val envFiles = DeclaredEnvVarsLocator.findEnvFiles(searchDirs)
        return DeclaredEnvVarsLocator.declaredVarNames(envFiles)
    }

    /**
     * Resolves a leaf PSI element covering [startOffset] -- never a
     * composite node. Anchoring `ProblemDescriptor`/`LineMarkerInfo` on a
     * composite node is a real, documented platform gotcha: only leaf
     * elements are safe here. Since
     * detection is plain-text (no guarantee the PSI tree for this file
     * type even has meaningful structure at this offset), this walks
     * down to `firstChild` until a true leaf (`firstChild == null`) is
     * reached, and falls back to the file root only if the offset can't
     * be resolved into the tree at all (defensive; not expected in
     * practice for a valid offset inside a real PsiFile's text).
     */
    private fun leafElementAt(file: PsiFile, startOffset: Int): PsiElement? {
        if (startOffset < 0 || startOffset >= file.textLength) return null
        var element = file.findElementAt(startOffset) ?: return file
        while (element.firstChild != null) {
            element = element.firstChild
        }
        return element
    }
}
