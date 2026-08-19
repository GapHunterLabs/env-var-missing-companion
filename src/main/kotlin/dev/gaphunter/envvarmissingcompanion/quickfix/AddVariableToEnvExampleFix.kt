package dev.gaphunter.envvarmissingcompanion.quickfix

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import dev.gaphunter.envvarmissingcompanion.parse.DeclaredEnvVarsLocator

/**
 * "Add VARNAME to .env.example" -- appends `VARNAME=` to the project's
 * real `.env.example` (found the same way [dev.gaphunter.envvarmissingcompanion.inspection.MissingEnvVarInspection]
 * found it: the fix's own file's parent directory, then project content
 * roots), or creates a new `.env.example` next to the fixed file if the
 * project has none at all -- never a crash, never a silent no-op.
 *
 * `startInWriteAction() = true` (explicit, not relying on the interface
 * default) so the platform wraps [applyFix] in a write action for us --
 * `EnvExampleWriter.addVariable` calls `VirtualFile.setBinaryContent`,
 * which requires one.
 */
class AddVariableToEnvExampleFix(private val varName: String) : LocalQuickFix {

    override fun getFamilyName(): String = "Add '$varName' to .env.example"

    override fun startInWriteAction(): Boolean = true

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val sourceFile = descriptor.psiElement?.containingFile?.virtualFile ?: return
        val sourceDir = sourceFile.parent ?: return

        val searchDirs = LinkedHashSet<VirtualFile>()
        searchDirs.add(sourceDir)
        for (root in ProjectRootManager.getInstance(project).contentRoots) {
            searchDirs.add(root)
        }

        val existingEnvExample = DeclaredEnvVarsLocator.findEnvFiles(searchDirs)
            .firstOrNull { it.name == ".env.example" }

        val targetDir = existingEnvExample?.parent ?: sourceDir
        EnvExampleWriter.addVariable(targetDir, existingEnvExample, varName)
    }
}
