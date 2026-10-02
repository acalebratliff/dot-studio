package io.github.acalebratliff.dotstudio.lang

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet

internal class DotTokenType(debugName: String) : IElementType(debugName, DotLanguage)

// @JvmField so the generated Java lexer can import the token types statically.
internal object DotTokenTypes {
    @JvmField val STRICT = DotTokenType("STRICT")

    @JvmField val GRAPH = DotTokenType("GRAPH")

    @JvmField val DIGRAPH = DotTokenType("DIGRAPH")

    @JvmField val SUBGRAPH = DotTokenType("SUBGRAPH")

    @JvmField val NODE = DotTokenType("NODE")

    @JvmField val EDGE = DotTokenType("EDGE")

    @JvmField val ID = DotTokenType("ID")

    @JvmField val NUMERAL = DotTokenType("NUMERAL")

    @JvmField val QUOTED_STRING = DotTokenType("QUOTED_STRING")

    @JvmField val HTML_STRING = DotTokenType("HTML_STRING")

    @JvmField val LINE_COMMENT = DotTokenType("LINE_COMMENT")

    @JvmField val BLOCK_COMMENT = DotTokenType("BLOCK_COMMENT")

    @JvmField val EDGEOP_DIRECTED = DotTokenType("EDGEOP_DIRECTED")

    @JvmField val EDGEOP_UNDIRECTED = DotTokenType("EDGEOP_UNDIRECTED")

    @JvmField val SEMICOLON = DotTokenType("SEMICOLON")

    @JvmField val COMMA = DotTokenType("COMMA")

    @JvmField val EQ = DotTokenType("EQ")

    @JvmField val COLON = DotTokenType("COLON")

    @JvmField val PLUS = DotTokenType("PLUS")

    @JvmField val LBRACKET = DotTokenType("LBRACKET")

    @JvmField val RBRACKET = DotTokenType("RBRACKET")

    @JvmField val LBRACE = DotTokenType("LBRACE")

    @JvmField val RBRACE = DotTokenType("RBRACE")

    val KEYWORDS: TokenSet = TokenSet.create(STRICT, GRAPH, DIGRAPH, SUBGRAPH, NODE, EDGE)
    val COMMENTS: TokenSet = TokenSet.create(LINE_COMMENT, BLOCK_COMMENT)
    val STRINGS: TokenSet = TokenSet.create(QUOTED_STRING, HTML_STRING)
    val EDGEOPS: TokenSet = TokenSet.create(EDGEOP_DIRECTED, EDGEOP_UNDIRECTED)
    val OPERATORS: TokenSet = TokenSet.create(EQ, COLON, PLUS)
}
