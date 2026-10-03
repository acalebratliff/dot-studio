package io.github.acalebratliff.dotstudio.preview

import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.Nls
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * A notice that takes the place of the preview, such as when there is no browser. Its text wraps at the width it is
 * given, unlike the single line `JBPanelWithEmptyText` draws, which clips in a narrow split. References no JCEF type.
 */
internal class PreviewNotice(@Nls text: String) : JPanel(BorderLayout()) {
    /** Exposed so the wrapping can be checked. */
    val area = JBTextArea(text).apply {
        lineWrap = true
        wrapStyleWord = true
        isEditable = false
        isOpaque = false
        font = UIUtil.getLabelFont()
        foreground = UIUtil.getInactiveTextColor()
        border = JBUI.Borders.empty(12)
    }

    val text: String get() = area.text

    init {
        add(area, BorderLayout.NORTH)
    }
}
