package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreviewProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.github.acalebratliff.dotstudio.lang.DotFileType

/**
 * Opens DOT files in the platform's text-and-preview editor, which supplies the editor/split/preview layout toggle.
 *
 * The platform builds the text editor here, not code of ours. On 2025.2 every Swing component remembers the classes
 * on the stack that created it, and platform services keep some editor components after the editor closes, so a
 * text editor built inside a method of ours would keep the plugin from unloading (#26).
 */
internal class DotSplitEditorProvider :
    TextEditorWithPreviewProvider(DotPreviewEditorProvider()),
    DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file.fileType == DotFileType

    override fun getEditorTypeId(): String = "dot-studio-split-editor"

    override fun createSplitEditor(firstEditor: TextEditor, secondEditor: FileEditor): FileEditor =
        DotSplitEditor(firstEditor, secondEditor as DotPreviewFileEditor)
}

/** Makes the preview half of [DotSplitEditorProvider]. It is not registered, so it never opens a file by itself. */
internal class DotPreviewEditorProvider :
    FileEditorProvider,
    DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file.fileType == DotFileType

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        // The text editor half is built from the same document, so it exists for any file this provider accepts.
        val document = checkNotNull(FileDocumentManager.getInstance().getDocument(file)) { "No document for $file" }
        return DotPreviewFileEditor(project, file, document)
    }

    override fun getEditorTypeId(): String = "dot-studio-preview"

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.NONE
}
