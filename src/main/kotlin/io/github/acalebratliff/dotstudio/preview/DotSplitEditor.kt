package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import io.github.acalebratliff.dotstudio.DotStudioBundle

/** Must match the group id in plugin.xml. */
internal const val EXPORT_GROUP_ID = "DotStudio.PreviewExport"

/**
 * The platform's text-and-preview editor, with the export actions next to its layout actions: in the editor tab when
 * the new UI shows them there, otherwise on the editor's toolbar.
 */
internal class DotSplitEditor(textEditor: TextEditor, preview: DotPreviewFileEditor) :
    TextEditorWithPreview(textEditor, preview, DotStudioBundle.message("preview.split.editor.name")) {
    override fun createRightToolbarActionGroup(): ActionGroup? = exportGroup()

    override fun createTabActions(): Array<AnAction> = exportGroup()?.let { arrayOf(it, *super.createTabActions()) }
        ?: super.createTabActions()

    private fun exportGroup(): ActionGroup? = ActionManager.getInstance().getAction(EXPORT_GROUP_ID) as? ActionGroup
}
