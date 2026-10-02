package io.github.acalebratliff.dotstudio.lang;

import com.intellij.lexer.FlexLexer;
import com.intellij.psi.tree.IElementType;

import static com.intellij.psi.TokenType.BAD_CHARACTER;
import static com.intellij.psi.TokenType.WHITE_SPACE;
import static io.github.acalebratliff.dotstudio.lang.DotTokenTypes.*;

// Lexical rules follow https://graphviz.org/doc/info/lang.html.
// Every token is returned in YYINITIAL, so the lexer can restart at any token boundary.

%%

%class DotLexer
%implements FlexLexer
%unicode
%function advance
%type IElementType

%{
  private int htmlStart;
  private int htmlDepth;

  private IElementType endHtmlString() {
    yybegin(YYINITIAL);
    zzStartRead = htmlStart;
    return HTML_STRING;
  }
%}

%state HTML

WHITE_SPACE=[ \t\r\n]+
// The spec allows bytes \200-\377 in IDs; in UTF-8 those are exactly the bytes of non-ASCII characters.
ID_START=[a-zA-Z_] | [^\u0000-\u007F]
ID_PART={ID_START} | [0-9]
NUMERAL=-?(\.[0-9]+ | [0-9]+(\.[0-9]*)?)
// Only \" is an escape, but a backslash also pairs with \\ and a newline (line continuation), as Graphviz's scanner does.
QUOTED_STRING=\"([^\"\\] | \\[^])*\\?\"?
LINE_COMMENT="//"[^\r\n]*
BLOCK_COMMENT="/*"([^*] | \*+[^*/])*\**"/"?

%%

<YYINITIAL> {
  {WHITE_SPACE}                    { return WHITE_SPACE; }

  [Ss][Tt][Rr][Ii][Cc][Tt]         { return STRICT; }
  [Gg][Rr][Aa][Pp][Hh]             { return GRAPH; }
  [Dd][Ii][Gg][Rr][Aa][Pp][Hh]     { return DIGRAPH; }
  [Ss][Uu][Bb][Gg][Rr][Aa][Pp][Hh] { return SUBGRAPH; }
  [Nn][Oo][Dd][Ee]                 { return NODE; }
  [Ee][Dd][Gg][Ee]                 { return EDGE; }

  {ID_START}{ID_PART}*             { return ID; }
  {NUMERAL}                        { return NUMERAL; }
  {QUOTED_STRING}                  { return QUOTED_STRING; }
  "<"                              { htmlStart = zzStartRead; htmlDepth = 1; yybegin(HTML); }

  {LINE_COMMENT}                   { return LINE_COMMENT; }
  {BLOCK_COMMENT}                  { return BLOCK_COMMENT; }
  // Graphviz's scan.l discards '#' to end of line anywhere: as preprocessor output at line start, as a shell-like comment elsewhere.
  "#"[^\r\n]*                      { return LINE_COMMENT; }

  "->"                             { return EDGEOP_DIRECTED; }
  "--"                             { return EDGEOP_UNDIRECTED; }
  ";"                              { return SEMICOLON; }
  ","                              { return COMMA; }
  "="                              { return EQ; }
  ":"                              { return COLON; }
  "+"                              { return PLUS; }
  "["                              { return LBRACKET; }
  "]"                              { return RBRACKET; }
  "{"                              { return LBRACE; }
  "}"                              { return RBRACE; }
}

<HTML> {
  "<"                              { htmlDepth++; }
  ">"                              { if (--htmlDepth == 0) return endHtmlString(); }
  [^<>]+                           { }
  <<EOF>>                          { return endHtmlString(); }
}

[^]                                { return BAD_CHARACTER; }
