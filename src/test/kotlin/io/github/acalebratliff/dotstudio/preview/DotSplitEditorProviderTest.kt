package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBPanelWithEmptyText
import io.github.acalebratliff.dotstudio.DotStudioBundle
import org.jdom.Element

internal class DotSplitEditorProviderTest : BasePlatformTestCase() {
    // Built per test: the base class looks up the text editor provider, which needs the test application.
    private val provider get() = DotSplitEditorProvider()

    fun testAcceptsDotAndGvFilesOnly() {
        assertTrue(provider.accept(project, myFixture.configureByText("graph.dot", "digraph {}").virtualFile))
        assertTrue(provider.accept(project, myFixture.configureByText("graph.gv", "graph {}").virtualFile))
        assertFalse(provider.accept(project, myFixture.configureByText("notes.txt", "digraph {}").virtualFile))
    }

    fun testCreatesTextEditorWithDotPreview() {
        val file = myFixture.configureByText("graph.dot", "digraph { a -> b }").virtualFile
        val editor = provider.createEditor(project, file)
        try {
            assertInstanceOf(editor, TextEditorWithPreview::class.java)
            val split = editor as TextEditorWithPreview
            // Builds the editor's UI as showing it would; TextEditorWithPreview creates it lazily and fails if that
            // first happens during disposal.
            assertNotNull(split.component)
            assertInstanceOf(split.previewEditor, DotPreviewFileEditor::class.java)
            assertEquals(file, split.previewEditor.file)
            assertEquals("digraph { a -> b }", split.textEditor.editor.document.text)
            // Tests run with JCEF off (build.gradle.kts), so the preview is the notice.
            val notice = assertInstanceOf(split.previewEditor.component, JBPanelWithEmptyText::class.java)
            assertEquals(DotStudioBundle.message("preview.jcef.unavailable"), notice.emptyText.text)
        } finally {
            Disposer.dispose(editor)
        }
    }

    fun testSavesAndRestoresCaretAndLayout() {
        val file = myFixture.configureByText("graph.dot", "digraph { a -> b }").virtualFile
        val saved = Element("state")
        withSplitEditor(file) { split ->
            split.textEditor.editor.caretModel.moveToOffset(CARET_OFFSET)
            split.setLayout(TextEditorWithPreview.Layout.SHOW_EDITOR)
            provider.writeState(split.getState(FileEditorStateLevel.FULL), project, saved)
        }
        withSplitEditor(file) { split ->
            // The platform also remembers the last layout chosen; change it so only the saved state can restore it.
            split.setLayout(TextEditorWithPreview.Layout.SHOW_PREVIEW)
            split.setState(provider.readState(saved, project, file))
            assertEquals(CARET_OFFSET, split.textEditor.editor.caretModel.offset)
            assertEquals(TextEditorWithPreview.Layout.SHOW_EDITOR, split.getLayout())
        }
    }

    fun testOpensWithDefaultsFromStateSavedBeforeStateWasKept() {
        // Earlier versions saved an empty element under this editor type id; workspaces still hold it.
        assertEquals("dot-studio-split-editor", provider.editorTypeId)
        val file = myFixture.configureByText("graph.dot", "digraph { a -> b }").virtualFile
        withSplitEditor(file) { split ->
            val layout = split.getLayout()
            split.setState(provider.readState(Element("state"), project, file))
            assertEquals(0, split.textEditor.editor.caretModel.offset)
            assertEquals(layout, split.getLayout())
        }
    }

    private fun withSplitEditor(file: VirtualFile, check: (TextEditorWithPreview) -> Unit) {
        val editor = provider.createEditor(project, file) as TextEditorWithPreview
        try {
            assertNotNull(editor.component)
            check(editor)
        } finally {
            Disposer.dispose(editor)
        }
    }

    private companion object {
        const val CARET_OFFSET = 10
    }
}
