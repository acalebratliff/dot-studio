package io.github.acalebratliff.dotstudio.lang

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import io.github.acalebratliff.dotstudio.DotStudioBundle
import javax.swing.Icon

internal class DotColorSettingsPage : ColorSettingsPage {
    override fun getDisplayName(): String = DotStudioBundle.message("filetype.display.name")

    override fun getIcon(): Icon = DotFileType.icon

    override fun getHighlighter(): SyntaxHighlighter = DotSyntaxHighlighter()

    override fun getDemoText(): String = DEMO_TEXT

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = arrayOf(
        descriptor("settings.color.keyword", DotHighlighterColors.KEYWORD),
        descriptor("settings.color.id", DotHighlighterColors.ID),
        descriptor("settings.color.numeral", DotHighlighterColors.NUMERAL),
        descriptor("settings.color.quoted.string", DotHighlighterColors.QUOTED_STRING),
        descriptor("settings.color.html.string", DotHighlighterColors.HTML_STRING),
        descriptor("settings.color.line.comment", DotHighlighterColors.LINE_COMMENT),
        descriptor("settings.color.block.comment", DotHighlighterColors.BLOCK_COMMENT),
        descriptor("settings.color.edgeop", DotHighlighterColors.EDGEOP),
        descriptor("settings.color.operator", DotHighlighterColors.OPERATOR),
        descriptor("settings.color.braces", DotHighlighterColors.BRACES),
        descriptor("settings.color.brackets", DotHighlighterColors.BRACKETS),
        descriptor("settings.color.semicolon", DotHighlighterColors.SEMICOLON),
        descriptor("settings.color.comma", DotHighlighterColors.COMMA),
    )

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    private fun descriptor(key: String, attributes: TextAttributesKey): AttributesDescriptor =
        AttributesDescriptor(DotStudioBundle.message(key), attributes)
}

private val DEMO_TEXT =
    """
    /* Build pipeline */
    strict digraph pipeline {
        graph [rankdir=LR, nodesep=0.5];
        node [shape=box];
        // Stages
        lint -> test -> build;
        build:e -> "release " + "notes";
        notes [label=<<b>Notes</b>>];
        subgraph cluster_ci { edge [weight=2]; lint -- test }
    }
    """.trimIndent()
