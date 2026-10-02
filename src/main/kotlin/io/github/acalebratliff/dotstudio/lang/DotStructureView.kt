package io.github.acalebratliff.dotstudio.lang

import com.intellij.icons.AllIcons
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import io.github.acalebratliff.dotstudio.lang.psi.DotEdgeRHS
import io.github.acalebratliff.dotstudio.lang.psi.DotEdgeStmt
import io.github.acalebratliff.dotstudio.lang.psi.DotGraph
import io.github.acalebratliff.dotstudio.lang.psi.DotNodeId
import io.github.acalebratliff.dotstudio.lang.psi.DotNodeStmt
import io.github.acalebratliff.dotstudio.lang.psi.DotStmtList
import io.github.acalebratliff.dotstudio.lang.psi.DotSubgraph
import io.github.acalebratliff.dotstudio.lang.psi.DotTypes
import javax.swing.Icon

internal class DotStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder =
        object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel =
                DotStructureViewModel(psiFile, editor)
        }
}

internal class DotStructureViewModel(file: PsiFile, editor: Editor?) :
    StructureViewModelBase(file, editor, DotStructureViewElement(file)),
    StructureViewModel.ElementInfoProvider {
    init {
        withSuitableClasses(
            DotGraph::class.java,
            DotSubgraph::class.java,
            DotNodeStmt::class.java,
            DotEdgeStmt::class.java,
        )
    }

    override fun isAlwaysShowsPlus(element: StructureViewTreeElement): Boolean = false

    override fun isAlwaysLeaf(element: StructureViewTreeElement): Boolean = element.value is DotNodeStmt
}

/** Graphs contain subgraphs and node and edge statements; an edge statement contains the subgraphs in its chain. */
internal class DotStructureViewElement(element: PsiElement) : PsiTreeElementBase<PsiElement>(element) {
    override fun getPresentableText(): String? = when (val element = element) {
        is PsiFile -> element.name
        is DotGraph, is DotSubgraph -> header(element)
        is DotNodeStmt -> element.nodeIdList.joinToString(", ") { it.text }
        is DotEdgeStmt -> buildString { appendChain(element) }
        else -> null
    }

    override fun getIcon(open: Boolean): Icon? = when (element) {
        is DotGraph -> AllIcons.Nodes.Module
        is DotSubgraph -> AllIcons.Nodes.Folder
        is DotNodeStmt -> AllIcons.Nodes.Field
        is DotEdgeStmt -> AllIcons.Nodes.Related
        else -> super.getIcon(open)
    }

    override fun getChildrenBase(): Collection<StructureViewTreeElement> = when (val element = element) {
        is PsiFile -> element.children.filterIsInstance<DotGraph>()
        is DotGraph -> statements(element.stmtList)
        is DotSubgraph -> statements(element.stmtList)
        is DotEdgeStmt -> element.children.filterIsInstance<DotSubgraph>() + element.edgeRHS.subgraphList
        else -> emptyList()
    }.map(::DotStructureViewElement)

    // grammar.y lets a subgraph statement carry an attribute list, which parses as a node_stmt around the subgraph.
    private fun statements(stmtList: DotStmtList?): List<PsiElement> = stmtList?.children.orEmpty().mapNotNull {
        when (it) {
            is DotNodeStmt -> if (it.nodeIdList.isEmpty()) it.children.firstOrNull { c -> c is DotSubgraph } else it
            is DotSubgraph, is DotEdgeStmt -> it
            else -> null
        }
    }
}

// The tokens before '{' as written, so `strict digraph G` keeps the user's keyword case. A body alone is `{...}`.
private fun header(element: PsiElement): String = generateSequence(element.firstChild) { it.nextSibling }
    .takeWhile { it.node.elementType != DotTypes.LBRACE }
    .filter {
        it !is PsiErrorElement && it.node.elementType != TokenType.WHITE_SPACE &&
            it.node.elementType !in DotTokenSets.COMMENTS
    }
    .joinToString(" ") { it.text }
    .ifEmpty { BODY_PLACEHOLDER }

private fun StringBuilder.appendChain(element: PsiElement) {
    for (child in generateSequence(element.firstChild) { it.nextSibling }) {
        when {
            child is DotNodeId -> append(child.text)
            child is DotSubgraph -> append(header(child))
            child is DotEdgeRHS -> appendChain(child)
            child.node.elementType == DotTypes.COMMA -> append(", ")
            child.node.elementType in DotTokenSets.EDGEOPS -> append(" ${child.text} ")
        }
    }
}
