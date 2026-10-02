package io.github.acalebratliff.dotstudio.lang

import com.intellij.lexer.Lexer
import com.intellij.testFramework.LexerTestCase

internal class DotLexerTest : LexerTestCase() {
    override fun createLexer(): Lexer = DotLexerAdapter()

    override fun getDirPath(): String = "src/test/testData/lexer"

    // The platform default resolves against the IDE home; our test data lives in the project.
    override fun getPathToTestDataFile(extension: String): String = "$dirPath/${getTestName(true)}$extension"

    fun testKeywords() = doTest()

    fun testIds() = doTest()

    fun testNumerals() = doTest()

    fun testQuotedStrings() = doTest()

    fun testHtmlStrings() = doTest()

    fun testComments() = doTest()

    fun testEdgeOps() = doTest()

    fun testPunctuation() = doTest()

    fun testErrors() = doTest()

    private fun doTest() {
        doFileTest("dot")
        checkCorrectRestart(loadTestDataFile(".dot"))
    }
}
