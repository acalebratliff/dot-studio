package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase

internal class ZoomActionTest : BasePlatformTestCase() {
    private val actions get() = listOf(ZoomInAction(), ZoomOutAction(), ActualSizeAction(), ZoomToFitAction())

    fun testHiddenOutsideDotPreviews() {
        for (action in actions) {
            val presentation = action.templatePresentation.clone()
            action.updatePresentation(presentation, panel = null)
            assertFalse(presentation.isVisible)
            assertFalse(presentation.isEnabled)
        }
    }

    fun testHiddenWithoutJcef() {
        val file = myFixture.configureByText("graph.dot", "digraph { a -> b }").virtualFile
        val editor = DotSplitEditorProvider().createEditor(project, file) as DotSplitEditor
        try {
            // TextEditorWithPreview builds its UI lazily and fails if that first happens during disposal.
            assertNotNull(editor.component)
            // Tests run with JCEF off (build.gradle.kts), so the preview is a notice that cannot zoom.
            val panel = (editor.previewEditor as DotPreviewFileEditor).panel
            for (action in actions) {
                val presentation = action.templatePresentation.clone()
                action.updatePresentation(presentation, panel)
                assertFalse(presentation.isVisible)
                assertFalse(presentation.isEnabled)
            }
        } finally {
            Disposer.dispose(editor)
        }
    }

    fun testZoomGroupHoldsTheFourActions() {
        val group = ActionManager.getInstance().getAction(ZOOM_GROUP_ID) as ActionGroup
        val ids = group.getChildren(null).map { ActionManager.getInstance().getId(it) }
        assertEquals(
            listOf("DotStudio.ZoomIn", "DotStudio.ZoomOut", "DotStudio.ActualSize", "DotStudio.ZoomToFit"),
            ids,
        )
    }

    fun testSplitEditorShowsZoomBeforeExport() {
        val file = myFixture.configureByText("graph.dot", "digraph { a -> b }").virtualFile
        val editor = DotSplitEditorProvider().createEditor(project, file) as DotSplitEditor
        try {
            assertNotNull(editor.component)
            val ids = editor.tabActions.getChildren(null).mapNotNull { ActionManager.getInstance().getId(it) }
            assertEquals(listOf(ZOOM_GROUP_ID, EXPORT_GROUP_ID), ids.take(2))
        } finally {
            Disposer.dispose(editor)
        }
    }
}
