package io.github.acalebratliff.dotstudio.lang

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import io.github.acalebratliff.dotstudio.lang.psi.DotTypes

internal class DotBraceMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(
        BracePair(DotTypes.LBRACE, DotTypes.RBRACE, true),
        BracePair(DotTypes.LBRACKET, DotTypes.RBRACKET, false),
    )

    override fun getPairs(): Array<BracePair> = pairs

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile?, openingBraceOffset: Int): Int = openingBraceOffset
}
