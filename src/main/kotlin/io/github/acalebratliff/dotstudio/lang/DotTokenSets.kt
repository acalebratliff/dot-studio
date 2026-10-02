package io.github.acalebratliff.dotstudio.lang

import com.intellij.psi.tree.TokenSet
import io.github.acalebratliff.dotstudio.lang.psi.DotTypes

internal object DotTokenSets {
    val KEYWORDS: TokenSet = TokenSet.create(
        DotTypes.STRICT,
        DotTypes.GRAPH,
        DotTypes.DIGRAPH,
        DotTypes.SUBGRAPH,
        DotTypes.NODE,
        DotTypes.EDGE,
    )
    val COMMENTS: TokenSet = TokenSet.create(DotTypes.LINE_COMMENT, DotTypes.BLOCK_COMMENT)
    val STRINGS: TokenSet = TokenSet.create(DotTypes.QUOTED_STRING, DotTypes.HTML_STRING)
    val EDGEOPS: TokenSet = TokenSet.create(DotTypes.EDGEOP_DIRECTED, DotTypes.EDGEOP_UNDIRECTED)
    val OPERATORS: TokenSet = TokenSet.create(DotTypes.EQ, DotTypes.COLON, DotTypes.PLUS)
}
