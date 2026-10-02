package io.github.acalebratliff.dotstudio.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import io.github.acalebratliff.dotstudio.DotStudioBundle.message
import io.github.acalebratliff.dotstudio.lang.psi.DotGraph
import io.github.acalebratliff.dotstudio.lang.psi.DotTypes
import org.jetbrains.annotations.Nls

private val GRAPH_TYPES = TokenSet.create(DotTypes.GRAPH, DotTypes.DIGRAPH)

/**
 * Reports what the parser can't see: an edge operator that doesn't match the graph type (Graphviz's scanner rejects
 * it), and strings or comments the lexer ran to the end of the file because they were never closed.
 */
internal class DotAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val message = errorMessage(element) ?: return
        holder.newAnnotation(HighlightSeverity.ERROR, message).create()
    }

    private fun errorMessage(element: PsiElement): @Nls String? = when (element.node.elementType) {
        DotTypes.EDGEOP_DIRECTED ->
            if (graphType(element) == DotTypes.GRAPH) message("annotator.edgeop.undirected.graph") else null

        DotTypes.EDGEOP_UNDIRECTED ->
            if (graphType(element) == DotTypes.DIGRAPH) message("annotator.edgeop.directed.graph") else null

        DotTypes.QUOTED_STRING ->
            if (isClosedQuotedString(element.text)) null else message("annotator.unterminated.quoted.string")

        DotTypes.HTML_STRING ->
            if (isClosedHtmlString(element.text)) null else message("annotator.unterminated.html.string")

        DotTypes.BLOCK_COMMENT ->
            if (isClosedBlockComment(element.text)) null else message("annotator.unterminated.block.comment")

        else -> null
    }

    private fun graphType(element: PsiElement) =
        PsiTreeUtil.getParentOfType(element, DotGraph::class.java)?.node?.findChildByType(GRAPH_TYPES)?.elementType

    // The closing quote must not itself be escaped, so an even number of backslashes precedes it.
    private fun isClosedQuotedString(text: String): Boolean =
        text.length >= 2 && text.endsWith('"') && text.dropLast(1).drop(1).takeLastWhile { it == '\\' }.length % 2 == 0

    // The lexer ends an HTML string where its angle brackets balance, or at the end of the file.
    private fun isClosedHtmlString(text: String): Boolean = text.count { it == '<' } == text.count { it == '>' }

    private fun isClosedBlockComment(text: String): Boolean = text.length >= 4 && text.endsWith("*/")
}
