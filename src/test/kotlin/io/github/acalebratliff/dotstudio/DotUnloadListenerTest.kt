package io.github.acalebratliff.dotstudio

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

internal class DotUnloadListenerTest : BasePlatformTestCase() {
    private val editors get() = FileEditorManager.getInstance(project)

    fun testClosesDotFilesOnlyWhenThisPluginUnloads() {
        val dot = myFixture.configureByText("graph.dot", "digraph {}").virtualFile
        val txt = myFixture.configureByText("notes.txt", "text").virtualFile

        DotUnloadListener(closesFiles = true).beforePluginUnload(plugin("com.intellij"), isUpdate = false)
        assertTrue(editors.isFileOpen(dot))

        DotUnloadListener(closesFiles = true).beforePluginUnload(ours(), isUpdate = false)
        assertFalse(editors.isFileOpen(dot))
        assertTrue(editors.isFileOpen(txt))
    }

    fun testReopensDotFilesWhenTheUpdatedVersionLoads() {
        val dot = myFixture.configureByText("graph.dot", "digraph {}").virtualFile

        DotUnloadListener(closesFiles = true).beforePluginUnload(ours(), isUpdate = true)
        assertFalse(editors.isFileOpen(dot))

        DotUnloadListener(closesFiles = true).pluginLoaded(ours())
        assertTrue(editors.isFileOpen(dot))
    }

    fun testDoesNotReopenAfterUninstallAndInstall() {
        val dot = myFixture.configureByText("graph.dot", "digraph {}").virtualFile

        DotUnloadListener(closesFiles = true).beforePluginUnload(ours(), isUpdate = false)
        DotUnloadListener(closesFiles = true).pluginLoaded(ours())
        assertFalse(editors.isFileOpen(dot))
    }

    fun testDropsAListLeftByAnotherProcess() {
        val dot = myFixture.configureByText("graph.dot", "digraph {}").virtualFile
        DotUnloadListener(closesFiles = true).beforePluginUnload(ours(), isUpdate = true)
        // As if the update had ended in a restart: the list was saved by an earlier process.
        PropertiesComponent.getInstance(project).setValue("dot-studio.reopen-after-update.process", "-1")

        DotUnloadListener(closesFiles = true).pluginLoaded(ours())
        assertFalse(editors.isFileOpen(dot))
        assertNull(PropertiesComponent.getInstance(project).getList("dot-studio.reopen-after-update.files"))
    }

    fun testLeavesFilesToThePlatformWhenClosingWouldPinTheClassLoader() {
        val dot = myFixture.configureByText("graph.dot", "digraph {}").virtualFile

        DotUnloadListener(closesFiles = false).beforePluginUnload(ours(), isUpdate = true)
        assertTrue(editors.isFileOpen(dot))
        assertNull(PropertiesComponent.getInstance(project).getList("dot-studio.reopen-after-update.files"))
    }

    private fun ours(): IdeaPluginDescriptor = plugin("io.github.acalebratliff.dotstudio")

    private fun plugin(id: String): IdeaPluginDescriptor = checkNotNull(PluginManagerCore.getPlugin(PluginId.getId(id)))
}
