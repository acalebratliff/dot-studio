package io.github.acalebratliff.dotstudio.lang

import com.intellij.extapi.psi.ASTWrapperPsiElement
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

private val FILE = IFileElementType(DotLanguage)

internal class DotFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DotLanguage) {
    override fun getFileType(): FileType = DotFileType

    override fun toString(): String = "DOT file"
}

// Flat until the Dot.bnf grammar lands: every token becomes a direct child of the file node.
internal class DotParserDefinition : ParserDefinition {
    override fun createLexer(project: Project?): Lexer = DotLexerAdapter()

    override fun createParser(project: Project?): PsiParser = PsiParser { root, builder ->
        val file = builder.mark()
        while (!builder.eof()) builder.advanceLexer()
        file.done(root)
        builder.treeBuilt
    }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = DotTokenTypes.COMMENTS

    override fun getStringLiteralElements(): TokenSet = DotTokenTypes.STRINGS

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = DotFile(viewProvider)
}
