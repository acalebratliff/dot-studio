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

    fun testEmptyFileIsBlank() = assertBlank(true, "")

    fun testWhitespaceIsBlank() = assertBlank(true, " \n\t\n")

    fun testCommentsOnlyIsBlank() = assertBlank(true, "// a note\n/* another\nnote */\n# a third\n")

    fun testAGraphIsNotBlank() = assertBlank(false, "// a note\ndigraph {}")

    fun testStrayTextIsNotBlankSoGraphvizCanReportIt() = assertBlank(false, "a -> b")

    fun testUnterminatedCommentIsBlank() = assertBlank(true, "/* never closed")

    private fun assertGraphCount(expected: Int, dot: String) {
        assertEquals(expected, inspect(dot).graphCount)
    }

    private fun assertBlank(expected: Boolean, dot: String) {
        assertEquals(expected, inspect(dot).isBlank)
    }

    private fun inspect(dot: String): DotSourceInfo {
        val file = myFixture.configureByText("graph.dot", dot)
        return runReadAction { inspectDotFile(file) }
    }
}
