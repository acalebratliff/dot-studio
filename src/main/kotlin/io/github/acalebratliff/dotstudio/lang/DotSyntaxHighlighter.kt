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
        in DotTokenTypes.KEYWORDS -> DotHighlighterColors.KEYWORD
        DotTokenTypes.ID -> DotHighlighterColors.ID
        DotTokenTypes.NUMERAL -> DotHighlighterColors.NUMERAL
        DotTokenTypes.QUOTED_STRING -> DotHighlighterColors.QUOTED_STRING
        DotTokenTypes.HTML_STRING -> DotHighlighterColors.HTML_STRING
        DotTokenTypes.LINE_COMMENT -> DotHighlighterColors.LINE_COMMENT
        DotTokenTypes.BLOCK_COMMENT -> DotHighlighterColors.BLOCK_COMMENT
        in DotTokenTypes.EDGEOPS -> DotHighlighterColors.EDGEOP
        in DotTokenTypes.OPERATORS -> DotHighlighterColors.OPERATOR
        DotTokenTypes.LBRACE, DotTokenTypes.RBRACE -> DotHighlighterColors.BRACES
        DotTokenTypes.LBRACKET, DotTokenTypes.RBRACKET -> DotHighlighterColors.BRACKETS
        DotTokenTypes.SEMICOLON -> DotHighlighterColors.SEMICOLON
        DotTokenTypes.COMMA -> DotHighlighterColors.COMMA
        TokenType.BAD_CHARACTER -> HighlighterColors.BAD_CHARACTER
        else -> null
    }
}

internal class DotSyntaxHighlighterFactory : SyntaxHighlighterFactory() {
    override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
        DotSyntaxHighlighter()
}
