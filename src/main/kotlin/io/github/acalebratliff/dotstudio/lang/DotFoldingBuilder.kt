package io.github.acalebratliff.dotstudio.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.SyntaxTraverser
import io.github.acalebratliff.dotstudio.lang.psi.DotAttrList
import io.github.acalebratliff.dotstudio.lang.psi.DotGraph
import io.github.acalebratliff.dotstudio.lang.psi.DotSubgraph
import io.github.acalebratliff.dotstudio.lang.psi.DotTypes

internal const val BODY_PLACEHOLDER = "{...}"

/** Folds graph and subgraph bodies, attribute lists and block comments that span more than one line. */
internal class DotFoldingBuilder :
    FoldingBuilderEx(),
    DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> =
        SyntaxTraverser.psiTraverser(root).mapNotNull { element ->
            ProgressManager.checkCanceled()
            foldRange(element)
                ?.takeIf { document.getLineNumber(it.startOffset) != document.getLineNumber(it.endOffset) }
                ?.let { FoldingDescriptor(element.node, it) }
        }.toTypedArray()

    // A body without its '}' folds to the end of the element, which recovery ended at the next thing it can't contain.
    private fun foldRange(element: PsiElement): TextRange? = when {
        element is DotGraph || element is DotSubgraph ->
            element.node.findChildByType(DotTypes.LBRACE)?.let {
                TextRange(it.startOffset, element.textRange.endOffset)
            }

        element is DotAttrList || element.node.elementType == DotTypes.BLOCK_COMMENT -> element.textRange

        else -> null
    }

    override fun getPlaceholderText(node: ASTNode): String = when (node.elementType) {
        DotTypes.BLOCK_COMMENT -> "/*...*/"
        DotTypes.attr_list -> "[...]"
        else -> BODY_PLACEHOLDER
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}
