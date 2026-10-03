package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import io.github.acalebratliff.dotstudio.lang.DotFileType
import io.github.acalebratliff.dotstudio.lang.psi.DotGraph

/**
 * What the preview needs to know about a DOT source besides its picture. [isBlank] means nothing but whitespace and
 * comments, so there is nothing to render yet.
 */
internal data class DotSourceInfo(val graphCount: Int, val isBlank: Boolean = false)

/** Counts the graphs of [file] at the top level, from its PSI. Call it in a read action. */
internal fun inspectDotFile(file: PsiFile): DotSourceInfo = DotSourceInfo(
    graphCount = PsiTreeUtil.getChildrenOfTypeAsList(file, DotGraph::class.java).size,
    isBlank = file.children.all { it is PsiWhiteSpace || it is PsiComment },
)

/** Parses [dot], an immutable snapshot, in a read action, so call it off the EDT. Nothing is attached to a project file. */
internal suspend fun inspectDot(project: Project, dot: String): DotSourceInfo = readAction {
    inspectDotFile(PsiFileFactory.getInstance(project).createFileFromText("preview.dot", DotFileType, dot))
}
