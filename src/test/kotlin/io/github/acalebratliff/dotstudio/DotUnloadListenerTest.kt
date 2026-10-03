package io.github.acalebratliff.dotstudio

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManagerCore
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

    fun testReopensDotFilesWhenReinstalledInTheSameSession() {
        val dot = myFixture.configureByText("graph.dot", "digraph {}").virtualFile

        DotUnloadListener(closesFiles = true).beforePluginUnload(ours(), isUpdate = false)
        assertFalse(editors.isFileOpen(dot))
        DotUnloadListener(closesFiles = true).pluginLoaded(ours())
        assertTrue(editors.isFileOpen(dot))
    }

    fun testReopensFilesWhoseNamesContainALineBreak() {
        val odd = myFixture.configureByText("line\nbreak.dot", "digraph {}").virtualFile
        val dot = myFixture.configureByText("graph.dot", "digraph {}").virtualFile

        DotUnloadListener(closesFiles = true).beforePluginUnload(ours(), isUpdate = false)
        DotUnloadListener(closesFiles = true).pluginLoaded(ours())
        assertTrue(editors.isFileOpen(odd))
        assertTrue(editors.isFileOpen(dot))
    }

    fun testReopensOnlyOnce() {
        val dot = myFixture.configureByText("graph.dot", "digraph {}").virtualFile
        DotUnloadListener(closesFiles = true).beforePluginUnload(ours(), isUpdate = false)
        DotUnloadListener(closesFiles = true).pluginLoaded(ours())
        editors.closeFile(dot)

        DotUnloadListener(closesFiles = true).pluginLoaded(ours())
        assertFalse(editors.isFileOpen(dot))
    }

    fun testDoesNotReopenFilesTheUserClosed() {
        val dot = myFixture.configureByText("graph.dot", "digraph {}").virtualFile
        editors.closeFile(dot)

        DotUnloadListener(closesFiles = true).beforePluginUnload(ours(), isUpdate = false)
        DotUnloadListener(closesFiles = true).pluginLoaded(ours())
        assertFalse(editors.isFileOpen(dot))
    }

    fun testReopensOnlyInTheProjectThatClosedTheFiles() {
        val dot = myFixture.configureByText("graph.dot", "digraph {}").virtualFile
        DotUnloadListener(closesFiles = true).beforePluginUnload(ours(), isUpdate = false)

        // The list is keyed by project, so a list saved for another project (here: a different key) is not used.
        val key = System.getProperties().stringPropertyNames().single {
            it.startsWith("dot-studio.reopen-after-reload.")
        }
        System.setProperty(key.replace(project.locationHash, "other-project"), System.clearProperty(key))
        try {
            DotUnloadListener(closesFiles = true).pluginLoaded(ours())
            assertFalse(editors.isFileOpen(dot))
        } finally {
            System.clearProperty(key.replace(project.locationHash, "other-project"))
        }
    }

    fun testLeavesFilesToThePlatformWhenClosingWouldPinTheClassLoader() {
        val dot = myFixture.configureByText("graph.dot", "digraph {}").virtualFile

        DotUnloadListener(closesFiles = false).beforePluginUnload(ours(), isUpdate = true)
        assertTrue(editors.isFileOpen(dot))
        assertTrue(
            System.getProperties().stringPropertyNames().none {
                it.startsWith("dot-studio.reopen-after-reload.")
            },
        )
    }

    private fun ours(): IdeaPluginDescriptor = plugin("io.github.acalebratliff.dotstudio")

    private fun plugin(id: String): IdeaPluginDescriptor = checkNotNull(PluginManagerCore.getPlugin(PluginId.getId(id)))
}
