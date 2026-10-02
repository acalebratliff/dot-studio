package io.github.acalebratliff.dotstudio.lang

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import io.github.acalebratliff.dotstudio.lang.parser.DotParser
import io.github.acalebratliff.dotstudio.lang.psi.DotTypes

private val FILE = IFileElementType(DotLanguage)

internal class DotFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DotLanguage) {
    override fun getFileType(): FileType = DotFileType

    override fun toString(): String = "DOT file"
}

internal class DotParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = DotLexerAdapter()

    override fun createParser(project: Project?): PsiParser = DotParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = DotTokenSets.COMMENTS

    override fun getStringLiteralElements(): TokenSet = DotTokenSets.STRINGS

    override fun createElement(node: ASTNode): PsiElement = DotTypes.Factory.createElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = DotFile(viewProvider)
}
