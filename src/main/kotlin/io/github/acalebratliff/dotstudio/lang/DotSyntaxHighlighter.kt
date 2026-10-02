package io.github.acalebratliff.dotstudio.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import io.github.acalebratliff.dotstudio.lang.psi.DotTypes

internal object DotHighlighterColors {
    val KEYWORD = createTextAttributesKey("DOT_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD)
    val ID = createTextAttributesKey("DOT_ID", DefaultLanguageHighlighterColors.IDENTIFIER)
    val NUMERAL = createTextAttributesKey("DOT_NUMERAL", DefaultLanguageHighlighterColors.NUMBER)
    val QUOTED_STRING = createTextAttributesKey("DOT_QUOTED_STRING", DefaultLanguageHighlighterColors.STRING)
    val HTML_STRING = createTextAttributesKey("DOT_HTML_STRING", DefaultLanguageHighlighterColors.MARKUP_TAG)
    val LINE_COMMENT = createTextAttributesKey("DOT_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT)
    val BLOCK_COMMENT = createTextAttributesKey("DOT_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT)
    val EDGEOP = createTextAttributesKey("DOT_EDGEOP", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    val OPERATOR = createTextAttributesKey("DOT_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN)
    val BRACES = createTextAttributesKey("DOT_BRACES", DefaultLanguageHighlighterColors.BRACES)
    val BRACKETS = createTextAttributesKey("DOT_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS)
    val SEMICOLON = createTextAttributesKey("DOT_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON)
    val COMMA = createTextAttributesKey("DOT_COMMA", DefaultLanguageHighlighterColors.COMMA)
}

internal class DotSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = DotLexerAdapter()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = pack(attributesFor(tokenType))

    private fun attributesFor(tokenType: IElementType): TextAttributesKey? = when (tokenType) {
        in DotTokenSets.KEYWORDS -> DotHighlighterColors.KEYWORD
        DotTypes.ID -> DotHighlighterColors.ID
        DotTypes.NUMERAL -> DotHighlighterColors.NUMERAL
        DotTypes.QUOTED_STRING -> DotHighlighterColors.QUOTED_STRING
        DotTypes.HTML_STRING -> DotHighlighterColors.HTML_STRING
        DotTypes.LINE_COMMENT -> DotHighlighterColors.LINE_COMMENT
        DotTypes.BLOCK_COMMENT -> DotHighlighterColors.BLOCK_COMMENT
        in DotTokenSets.EDGEOPS -> DotHighlighterColors.EDGEOP
        in DotTokenSets.OPERATORS -> DotHighlighterColors.OPERATOR
        DotTypes.LBRACE, DotTypes.RBRACE -> DotHighlighterColors.BRACES
        DotTypes.LBRACKET, DotTypes.RBRACKET -> DotHighlighterColors.BRACKETS
        DotTypes.SEMICOLON -> DotHighlighterColors.SEMICOLON
        DotTypes.COMMA -> DotHighlighterColors.COMMA
        TokenType.BAD_CHARACTER -> HighlighterColors.BAD_CHARACTER
        else -> null
    }
}

internal class DotSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        DotSyntaxHighlighter()
}
