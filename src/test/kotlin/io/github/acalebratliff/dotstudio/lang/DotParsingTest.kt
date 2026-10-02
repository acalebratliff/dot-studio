package io.github.acalebratliff.dotstudio.lang

import com.intellij.lang.LanguageBraceMatching
import com.intellij.testFramework.ParsingTestCase

internal class DotParsingTest : ParsingTestCase("", "dot", DotParserDefinition()) {
    // Grammar-Kit's error recovery uses the language's brace pairs, so trees for broken input match the IDE's only with
    // the brace matcher registered. Unregistered, the lookup would also cache "none" on the DotLanguage singleton, and
    // fixture tests running later in the same JVM would lose brace matching.
    override fun setUp() {
        super.setUp()
        addExplicitExtension(LanguageBraceMatching.INSTANCE, DotLanguage, DotBraceMatcher())
    }

    override fun getTestDataPath(): String = "src/test/testData/parser"

    override fun includeRanges(): Boolean = true

    fun testGraph() = doTest(true)

    fun testMultipleGraphs() = doTest(true)

    fun testEmptyFile() = doTest(true)

    fun testKeywordCase() = doTest(true)

    fun testAttrStmt() = doTest(true)

    fun testAttrStmtMacroName() = doTest(true)

    fun testAList() = doTest(true)

    fun testAttrAssignment() = doTest(true)

    fun testNodeStmt() = doTest(true)

    fun testEdgeStmt() = doTest(true)

    fun testNodeLists() = doTest(true)

    fun testPorts() = doTest(true)

    fun testSubgraph() = doTest(true)

    fun testSubgraphAttrList() = doTest(true)

    fun testIds() = doTest(true)

    fun testComments() = doTest(true)

    fun testRecoverFileEntry() = doTest(true)

    fun testRecoverGraphHeader() = doTest(true)

    fun testRecoverStmtEntry() = doTest(true)

    fun testRecoverAListEntry() = doTest(true)

    fun testRecoverUnclosedAttrList() = doTest(true)

    fun testRecoverAttrStmt() = doTest(true)

    fun testRecoverNodeList() = doTest(true)

    fun testRecoverMissingBraceBeforeGraph() = doTest(true)

    fun testRecoverSubgraphWithoutBody() = doTest(true)

    fun testRecoverConcatenation() = doTest(true)

    fun testRecoverPort() = doTest(true)

    fun testRecoverMissingBrace() = doTest(true)

    fun testRecoverBadCharacter() = doTest(true)
}
