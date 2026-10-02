package io.github.acalebratliff.dotstudio.lang

import com.intellij.lang.LanguageBraceMatching
import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.impl.PsiBuilderAdapter
import com.intellij.testFramework.ParsingTestCase
import io.github.acalebratliff.dotstudio.lang.parser.DotParser

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

    fun testRecoverTypoInAList() = doTest(true)

    fun testRecoverAttrStmt() = doTest(true)

    fun testRecoverNodeList() = doTest(true)

    fun testRecoverMissingBraceBeforeGraph() = doTest(true)

    fun testRecoverSubgraphWithoutBody() = doTest(true)

    fun testRecoverConcatenation() = doTest(true)

    fun testRecoverPort() = doTest(true)

    fun testRecoverMissingBrace() = doTest(true)

    fun testRecoverBadCharacter() = doTest(true)

    // A lookahead that rescans the rest of an attribute list at every entry made parsing quadratic in the list's length
    // (#15 review: 12000 entries took 7.1 s). Every lookahead or recovery step moves the builder with advanceLexer and
    // a rollback moves it back, so the number of advanceLexer calls is the parser's work. It is deterministic, unlike a
    // timing, and grows 4x for 4x the entries when parsing is linear and 16x when it is quadratic.
    fun testAttrListParsingIsLinear() {
        val valid = { n: Int -> "digraph { a [" + (1..n).joinToString(", ") { "k$it=v" } + "] }" }
        val unclosed = { n: Int -> "digraph { a [\n" + (1..n).joinToString("") { "k$it=v;\n" } + "}" }
        val typo = { n: Int -> "digraph { a [color red, " + (1..n).joinToString(", ") { "k$it=v" } + "] }" }
        for (input in listOf(valid, unclosed, typo)) {
            val growth = advances(input(4000)).toDouble() / advances(input(1000))
            assertTrue("parser work grew ${growth}x for 4x the entries", growth < 6)
        }
    }

    private fun advances(text: String): Int {
        var count = 0
        val builder =
            object : PsiBuilderAdapter(
                PsiBuilderFactory.getInstance().createBuilder(DotParserDefinition(), DotLexerAdapter(), text),
            ) {
                override fun advanceLexer() {
                    count++
                    super.advanceLexer()
                }
            }
        DotParser().parse(DotParserDefinition().fileNodeType, builder)
        return count
    }
}
