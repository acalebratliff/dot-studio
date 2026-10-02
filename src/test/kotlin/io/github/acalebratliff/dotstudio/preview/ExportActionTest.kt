package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.acalebratliff.dotstudio.DotStudioBundle

internal class ExportActionTest : BasePlatformTestCase() {
    fun testHiddenOutsideDotPreviews() {
        val action = ExportSvgAction()
        val presentation = action.templatePresentation.clone()
        action.updatePresentation(presentation, panel = null)
        assertFalse(presentation.isVisible)
        assertFalse(presentation.isEnabled)
    }

    fun testDisabledWithReasonWithoutJcef() {
        val file = myFixture.configureByText("graph.dot", "digraph { a -> b }").virtualFile
        val editor = DotSplitEditorProvider().createEditor(project, file) as DotSplitEditor
        try {
            // TextEditorWithPreview builds its UI lazily and fails if that first happens during disposal.
            assertNotNull(editor.component)
            // Tests run with JCEF off (build.gradle.kts), so there is no rendered graph to export.
            val panel = (editor.previewEditor as DotPreviewFileEditor).panel
            for (action in listOf(ExportSvgAction(), ExportPng1xAction(), ExportPng2xAction())) {
                val presentation = action.templatePresentation.clone()
                action.updatePresentation(presentation, panel)
                assertTrue(presentation.isVisible)
                assertFalse(presentation.isEnabled)
                assertEquals(DotStudioBundle.message("preview.export.unavailable.jcef"), presentation.description)
            }
        } finally {
            Disposer.dispose(editor)
        }
    }

    fun testGroupHiddenInOtherEditors() {
        val file = myFixture.configureByText("notes.txt", "text").virtualFile
        val event = TestActionEvent.createTestEvent(
            SimpleDataContext.builder()
                .add(CommonDataKeys.PROJECT, project)
                .add(CommonDataKeys.VIRTUAL_FILE, file)
                .build(),
        )
        ExportGroup().update(event)
        assertFalse(event.presentation.isVisible)
    }

    fun testExportGroupHoldsTheThreeActions() {
        val group = ActionManager.getInstance().getAction(EXPORT_GROUP_ID) as ActionGroup
        assertTrue(group is ExportGroup)
        val ids = group.getChildren(null).map { ActionManager.getInstance().getId(it) }
        assertEquals(listOf("DotStudio.ExportSvg", "DotStudio.ExportPng1x", "DotStudio.ExportPng2x"), ids)
    }
}
