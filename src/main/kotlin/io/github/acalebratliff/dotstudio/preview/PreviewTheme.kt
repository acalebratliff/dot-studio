package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.JBUI
import io.github.acalebratliff.dotstudio.preview.jcef.jsStringLiteral
import java.awt.Color
import java.util.Locale

/** The IDE colours the preview page uses, so it matches the editor beside it. */
internal data class PreviewTheme(
    val background: Color,
    val foreground: Color,
    val errorBackground: Color,
    val errorBorder: Color,
)

/** Reads the current editor colour scheme and UI theme. */
internal fun currentPreviewTheme(): PreviewTheme {
    val scheme = EditorColorsManager.getInstance().globalScheme
    return PreviewTheme(
        background = scheme.defaultBackground,
        foreground = scheme.defaultForeground,
        errorBackground = JBUI.CurrentTheme.Banner.ERROR_BACKGROUND,
        errorBorder = JBUI.CurrentTheme.Banner.ERROR_BORDER_COLOR,
    )
}

/** The page call that applies [theme]. The colours travel as string literals in an object, never as code. */
internal fun previewThemeScript(theme: PreviewTheme): String {
    val values = listOf(
        "background" to cssColor(theme.background),
        "foreground" to cssColor(theme.foreground),
        "errorBackground" to cssColor(theme.errorBackground),
        "errorBorder" to cssColor(theme.errorBorder),
        "colorScheme" to if (ColorUtil.isDark(theme.background)) "dark" else "light",
    )
    return values.joinToString(", ", prefix = "dotStudio.setTheme({", postfix = "})") { (key, value) ->
        "${jsStringLiteral(key)}: ${jsStringLiteral(value)}"
    }
}

internal fun cssColor(color: Color): String = if (color.alpha == OPAQUE) {
    String.format(Locale.ROOT, "#%02x%02x%02x", color.red, color.green, color.blue)
} else {
    String.format(
        Locale.ROOT,
        "rgba(%d, %d, %d, %.3f)",
        color.red,
        color.green,
        color.blue,
        color.alpha / OPAQUE.toFloat(),
    )
}

private const val OPAQUE = 255
