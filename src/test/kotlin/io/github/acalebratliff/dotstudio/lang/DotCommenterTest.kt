package io.github.acalebratliff.dotstudio.lang

import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.testFramework.fixtures.BasePlatformTestCase

internal class DotCommenterTest : BasePlatformTestCase() {
    fun testLineComment() {
        myFixture.configureByText("graph.dot", "a -> <caret>b;\nc;\n")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        myFixture.checkResult("//a -> b;\nc;\n")
    }

    fun testLineUncomment() {
        myFixture.configureByText("graph.dot", "//a -> <caret>b;\n")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_LINE)
        myFixture.checkResult("a -> b;\n")
    }

    fun testBlockComment() {
        myFixture.configureByText("graph.dot", "a -> <selection>b [x=1]</selection>;\n")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_BLOCK)
        myFixture.checkResult("a -> /*b [x=1]*/;\n")
    }

    fun testBlockUncomment() {
        myFixture.configureByText("graph.dot", "a -> /*b <caret>[x=1]*/;\n")
        myFixture.performEditorAction(IdeActions.ACTION_COMMENT_BLOCK)
        myFixture.checkResult("a -> b [x=1];\n")
    }
}
