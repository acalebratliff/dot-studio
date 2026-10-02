package io.github.acalebratliff.dotstudio.lang

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

internal class DotBraceMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(
        BracePair(DotTokenTypes.LBRACE, DotTokenTypes.RBRACE, true),
        BracePair(DotTokenTypes.LBRACKET, DotTokenTypes.RBRACKET, false),
    )

    override fun getPairs(): Array<BracePair> = pairs

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}
