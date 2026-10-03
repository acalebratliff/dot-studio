package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.application.runReadAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase

internal class DotSourceInfoTest : BasePlatformTestCase() {
    fun testOneGraph() = assertGraphCount(1, "digraph G { a -> b }")

    fun testTwoGraphs() = assertGraphCount(2, "digraph First { a -> b }\n\ngraph Second { x -- y }")

    fun testSubgraphsAreNotCounted() = assertGraphCount(1, "digraph { subgraph cluster_a { a } subgraph { b } }")

    fun testBraceAndKeywordInsideStringsAndCommentsAreNotCounted() =
        assertGraphCount(1, "// digraph X {}\ndigraph { a [label=\"digraph Y {}\"] } /* graph Z {} */")

    fun testStrictGraphsCount() = assertGraphCount(2, "strict graph { a } strict digraph { b }")

    fun testNoGraph() = assertGraphCount(0, "a -> b")

    private fun assertGraphCount(expected: Int, dot: String) {
        val file = myFixture.configureByText("graph.dot", dot)
        assertEquals(DotSourceInfo(expected), runReadAction { inspectDotFile(file) })
    }
}
