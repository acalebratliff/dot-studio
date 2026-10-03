package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.project.DumbAwareAction
import io.github.acalebratliff.dotstudio.DotStudioBundle

/**
 * The export actions. It is hidden outside DOT previews, so editors of other files never show it in their context menu;
 * a group's default presentation would only disable it there.
 */
internal class ExportGroup : DefaultActionGroup() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = dotPreviewOf(e) != null
    }
}

/** Saves the graph on show in the DOT preview of the event's file. */
internal abstract class ExportAction(private val format: ExportFormat) : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        updatePresentation(e.presentation, dotPreviewOf(e)?.panel)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val panel = dotPreviewOf(e)?.panel ?: return
        // The graph on show now is what gets exported, even if a render finishes while the dialog is open.
        val svg = panel.renderedSvg ?: return
        val descriptor = FileSaverDescriptor(
            DotStudioBundle.message("preview.export.dialog.title"),
            DotStudioBundle.message("preview.export.dialog.description", format.extension.uppercase()),
            format.extension,
        )
        val target = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
            .save(file.parent, "${file.nameWithoutExtension}.${format.extension}") ?: return
        project.service<DotPreviewService>().export(panel, svg, format, target.file.toPath())
    }

    /** Hidden outside DOT previews; disabled, with the reason as its description, when there is nothing to export. */
    fun updatePresentation(presentation: Presentation, panel: DotPreviewPanel?) {
        presentation.isVisible = panel != null
        val reason = panel?.exportUnavailableReason
        presentation.isEnabled = panel != null && reason == null
        presentation.description = reason ?: templatePresentation.description
    }
}

/** The DOT preview open for the event's file, or null when that file has none. */
internal fun dotPreviewOf(e: AnActionEvent): DotPreviewFileEditor? {
    val project = e.project ?: return null
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return null
    return FileEditorManager.getInstance(project).getEditors(file)
        .firstNotNullOfOrNull { (it as? TextEditorWithPreview)?.previewEditor as? DotPreviewFileEditor }
}

internal class ExportSvgAction : ExportAction(ExportFormat.Svg)

internal class ExportPng1xAction : ExportAction(ExportFormat.Png(scale = 1))

internal class ExportPng2xAction : ExportAction(ExportFormat.Png(scale = 2))
