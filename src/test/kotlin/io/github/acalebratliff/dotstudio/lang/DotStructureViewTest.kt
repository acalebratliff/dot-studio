package io.github.acalebratliff.dotstudio.lang

import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

private const val SOURCE = """strict DiGraph G {
    node [shape=box]
    rankdir=LR
    a, b:p:n
    a -> b -> c [color=red]
    x -> {y z}
    {u v} -> w
    subgraph cluster_0 {
        c
        subgraph { d }
    }
    { e } [rank=same]
}
graph {
    f -- g, h
}
"""

internal class DotStructureViewTest : BasePlatformTestCase() {
    fun testStructureView() {
        myFixture.configureByText("graph.dot", SOURCE)
        myFixture.testStructureView { component ->
            PlatformTestUtil.expandAll(component.tree)
            PlatformTestUtil.assertTreeEqual(
                component.tree,
                """
                -graph.dot
                 -strict DiGraph G
                  a, b:p:n
                  a -> b -> c
                  -x -> {...}
                   -{...}
                    y
                    z
                  -{...} -> w
                   -{...}
                    u
                    v
                  -subgraph cluster_0
                   c
                   -subgraph
                    d
                  -{...}
                   e
                 -graph
                  f -- g, h
                """.trimIndent() + "\n",
            )
        }
    }

    // A graph without a body, a graph without its '}' and subgraphs without bodies are still listed by their headers.
    fun testBrokenInput() {
        myFixture.configureByText("graph.dot", "digraph G\ngraph H {\n    subgraph s\n    a -> subgraph\n")
        myFixture.testStructureView { component ->
            PlatformTestUtil.expandAll(component.tree)
            PlatformTestUtil.assertTreeEqual(
                component.tree,
                """
                -graph.dot
                 digraph G
                 -graph H
                  subgraph s
                  -a -> subgraph
                   subgraph
                """.trimIndent() + "\n",
            )
        }
    }

    fun testNavigatesToStatement() {
        myFixture.configureByText("graph.dot", SOURCE)
        val builder = DotStructureViewFactory().getStructureViewBuilder(myFixture.file) as TreeBasedStructureViewBuilder
        val model = builder.createStructureViewModel(myFixture.editor)
        try {
            val edge = model.root.children.first().children[1] as StructureViewTreeElement
            edge.navigate(true)
            assertEquals(SOURCE.indexOf("a -> b"), myFixture.caretOffset)
        } finally {
            Disposer.dispose(model)
        }
    }
}
