package io.github.acalebratliff.dotstudio.lang

import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

internal class DotSyntaxHighlighterTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/testData/highlighting"

    fun testAllAttributes() {
        val file = myFixture.configureByFile("allAttributes.dot")
        EditorTestUtil.testFileSyntaxHighlighting(file, "$testDataPath/allAttributes.txt", false)
    }
}
