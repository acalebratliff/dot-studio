package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import io.github.acalebratliff.dotstudio.DotStudioBundle
import java.awt.event.HierarchyEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent

/**
 * The preview half of the DOT split editor. Changes to [document] reach the preview while it is showing. While it is
 * hidden (editor-only layout, or another tab selected) nothing renders, and the latest text renders when it shows
 * again. Visibility is used rather than `TextEditorWithPreview.onLayoutChange`, which a restored layout bypasses.
 */
internal class DotPreviewFileEditor(project: Project, private val file: VirtualFile, private val document: Document) :
    UserDataHolderBase(),
    FileEditor {
    private val panel = project.service<DotPreviewService>().createPanel(this)

    // EDT only. True when the document changed while the preview was hidden, or before it was first shown.
    private var stale = true

    init {
        document.addDocumentListener(
            object : DocumentListener {
                override fun documentChanged(event: DocumentEvent) {
                    if (panel.component.isShowing) panel.showSource(document.immutableCharSequence) else stale = true
                }
            },
            this,
        )
        panel.component.addHierarchyListener { event ->
            val showingChanged = event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L
            if (showingChanged && panel.component.isShowing && stale) {
                stale = false
                panel.showSource(document.immutableCharSequence)
            }
        }
    }

    override fun getComponent(): JComponent = panel.component

    override fun getPreferredFocusedComponent(): JComponent? = null

    override fun getName(): String = DotStudioBundle.message("preview.editor.name")

    override fun getFile(): VirtualFile = file

    override fun setState(state: FileEditorState) = Unit

    override fun isModified(): Boolean = false

    override fun isValid(): Boolean = file.isValid

    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit

    override fun dispose() = Unit
}
