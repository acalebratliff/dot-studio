package io.github.acalebratliff.dotstudio.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

internal class DotAnnotatorTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/testData/annotator"

    fun testEdgeOpMismatch() = doTest()

    fun testClosedTokens() = doTest()

    fun testUnterminatedQuotedString() = doTest()

    fun testUnterminatedHtmlString() = doTest()

    fun testUnterminatedBlockComment() = doTest()

    private fun doTest() {
        myFixture.testHighlighting(true, false, false, "${getTestName(false)}.dot")
    }
}
