package io.github.acalebratliff.dotstudio

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileManager
import io.github.acalebratliff.dotstudio.lang.DotFileType

/**
 * Closes DOT files before this plugin unloads. On 2026.2 the platform unloads the optional JCEF part of the plugin
 * first and retires our element types with it, so an editor still showing a DOT file repaints with element types of
 * an unloaded plugin and the platform logs an error (#26).
 *
 * On an update the closed files are reopened once the new version has loaded. The list is kept in the project's
 * properties because nothing of the old plugin's may outlive it; it is tagged with this process, so a list left by an
 * update that ended in a restart is dropped rather than reopened later. Both callbacks run on the EDT.
 *
 * Before Java 24 every Swing component keeps the protection domains on the stack that built it. Closing a tab here
 * makes the platform rebuild the editor tabs' toolbar inside this call, and that toolbar then keeps the plugin's class
 * loader alive (2025.2 heap dump, #26). So the files are closed only when [closesFiles]; on older runtimes the
 * platform closes them itself during the unload, and those IDEs do not retire element types early.
 */
internal class DotUnloadListener(
    private val closesFiles: Boolean = Runtime.version().feature() >= FIRST_JAVA_WITHOUT_CALLER_CONTEXT,
) : DynamicPluginListener {
    override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        if (pluginDescriptor.pluginId.idString != PLUGIN_ID || !closesFiles) return
        for (project in ProjectManager.getInstance().openProjects) {
            val editors = FileEditorManager.getInstance(project)
            val dotFiles = editors.openFiles.filter { it.fileType == DotFileType }
            if (isUpdate && dotFiles.isNotEmpty()) {
                val properties = PropertiesComponent.getInstance(project)
                properties.setValue(REOPEN_PROCESS_KEY, currentProcess())
                properties.setList(REOPEN_FILES_KEY, dotFiles.map { it.url })
            }
            dotFiles.forEach(editors::closeFile)
        }
    }

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        if (pluginDescriptor.pluginId.idString != PLUGIN_ID) return
        for (project in ProjectManager.getInstance().openProjects) {
            val properties = PropertiesComponent.getInstance(project)
            val urls = properties.getList(REOPEN_FILES_KEY).orEmpty()
            val sameProcess = properties.getValue(REOPEN_PROCESS_KEY) == currentProcess()
            properties.setList(REOPEN_FILES_KEY, null)
            properties.unsetValue(REOPEN_PROCESS_KEY)
            if (!sameProcess) continue
            val editors = FileEditorManager.getInstance(project)
            urls.mapNotNull(VirtualFileManager.getInstance()::findFileByUrl).forEach { editors.openFile(it, false) }
        }
    }

    private fun currentProcess(): String = ProcessHandle.current().pid().toString()

    private companion object {
        const val PLUGIN_ID = "io.github.acalebratliff.dotstudio"

        // JEP 486: from Java 24 a new component no longer records the protection domains of its callers.
        const val FIRST_JAVA_WITHOUT_CALLER_CONTEXT = 24
        const val REOPEN_FILES_KEY = "dot-studio.reopen-after-update.files"
        const val REOPEN_PROCESS_KEY = "dot-studio.reopen-after-update.process"
    }
}
