package io.github.acalebratliff.dotstudio.lang

import com.intellij.codeInsight.highlighting.BraceMatchingUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

internal class DotBraceMatcherTest : BasePlatformTestCase() {
    fun testBracesMatch() = assertMatch("digraph <caret>{ a -> b }", "}")

    fun testBracketsMatch() = assertMatch("a <caret>[x=1, y=2];", "];")

    fun testBraceInsideStringIsIgnored() = assertMatch("<caret>{ a [label=\"}\"] }", "}")

    fun testTypingBracketInsertsPair() {
        myFixture.configureByText("graph.dot", "a <caret>")
        myFixture.type('[')
        myFixture.checkResult("a [<caret>]")
    }

    private fun assertMatch(text: String, expectedSuffix: String) {
        myFixture.configureByText("graph.dot", text)
        val offset = BraceMatchingUtil.getMatchedBraceOffset(myFixture.editor, true, myFixture.file)
        assertEquals(expectedSuffix, myFixture.editor.document.text.substring(offset))
    }
}
