package io.github.acalebratliff.dotstudio.lang

import com.intellij.lexer.FlexAdapter

internal class DotLexerAdapter : FlexAdapter(DotLexer(null))
