package io.github.acalebratliff.dotstudio.lang

import com.intellij.testFramework.fixtures.BasePlatformTestCase

internal class DotFoldingTest : BasePlatformTestCase() {
    override fun getTestDataPath(): String = "src/test/testData/folding"

    // Also covers a graph whose '}' is missing: its body folds to the end of the file.
    fun testFolding() = myFixture.testFolding("$testDataPath/${getTestName(false)}.dot")
}
