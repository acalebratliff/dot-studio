package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import io.github.acalebratliff.dotstudio.DotStudioBundle

/** Must match the group ids in plugin.xml. */
internal const val ZOOM_GROUP_ID = "DotStudio.PreviewZoom"
internal const val EXPORT_GROUP_ID = "DotStudio.PreviewExport"

/**
 * The platform's text-and-preview editor, with the zoom and export actions next to its layout actions: in the editor
 * tab when the new UI shows them there, otherwise on the editor's toolbar.
 */
internal class DotSplitEditor(textEditor: TextEditor, preview: DotPreviewFileEditor) :
    TextEditorWithPreview(textEditor, preview, DotStudioBundle.message("preview.split.editor.name")) {
    override fun createRightToolbarActionGroup(): ActionGroup = DefaultActionGroup(previewGroups())

    override fun createTabActions(): Array<AnAction> =
        previewGroups().toTypedArray<AnAction>() + super.createTabActions()

    private fun previewGroups(): List<ActionGroup> = listOf(ZOOM_GROUP_ID, EXPORT_GROUP_ID)
        .mapNotNull { ActionManager.getInstance().getAction(it) as? ActionGroup }
}
