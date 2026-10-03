package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.DumbAwareAction

/**
 * Zooms the DOT preview of the event's file. No default shortcut: the usual zoom keys (Ctrl+=, Ctrl+Minus) expand and
 * collapse folds in the text editor beside the preview. Ctrl+wheel over the preview zooms too, and users can assign
 * keys in the keymap.
 */
internal abstract class ZoomAction(private val command: ZoomCommand) : DumbAwareAction() {
    // EDT because the update reads whether the preview component is showing.
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        updatePresentation(e.presentation, dotPreviewOf(e)?.panel)
    }

    override fun actionPerformed(e: AnActionEvent) {
        dotPreviewOf(e)?.panel?.zoom(command)
    }

    /** Hidden without a JCEF preview; disabled while the preview is hidden or the zoom would not change. */
    fun updatePresentation(presentation: Presentation, panel: DotPreviewPanel?) {
        presentation.isVisible = panel != null && panel.canZoom
        presentation.isEnabled = panel != null && panel.component.isShowing && command.isEnabled(panel.zoom)
    }
}

internal class ZoomInAction : ZoomAction(ZoomCommand.In)

internal class ZoomOutAction : ZoomAction(ZoomCommand.Out)

internal class ZoomToFitAction : ZoomAction(ZoomCommand.Fit)

internal class ActualSizeAction : ZoomAction(ZoomCommand.ActualSize)
