package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBPanelWithEmptyText
import io.github.acalebratliff.dotstudio.DotStudioBundle

internal class DotSplitEditorProviderTest : BasePlatformTestCase() {
    private val provider = DotSplitEditorProvider()

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
}
