package io.github.acalebratliff.dotstudio

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFileManager
import io.github.acalebratliff.dotstudio.lang.DotFileType

/**
 * Closes DOT files before this plugin unloads. On 2026.2 the platform unloads the optional JCEF part of the plugin
 * first and retires our element types with it, so an editor still showing a DOT file repaints with element types of
 * an unloaded plugin and the platform logs an error (#26).
 *
 * The files closed here are reopened, in the same project, when the plugin loads again in this IDE session (an update
 * or a reinstall). The 2026.2 update path reports isUpdate = false to listeners, so the flag is not consulted. The list
 * must outlive this plugin's classes and must not be saved: writing it to the project's properties during the unload
 * made 2026.2 log a reparse error for workspace.xml (#26). So it is a JVM system property per project, gone after a
 * restart. Both callbacks run on the EDT.
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
            val urls = dotFiles.map { it.url }
            if (urls.isNotEmpty()) System.setProperty(reopenKey(project), urls.joinToString(URL_SEPARATOR))
            dotFiles.forEach(editors::closeFile)
        }
    }

    override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        if (pluginDescriptor.pluginId.idString != PLUGIN_ID) return
        for (project in ProjectManager.getInstance().openProjects) {
            val urls = System.clearProperty(reopenKey(project))?.split(URL_SEPARATOR) ?: continue
            val editors = FileEditorManager.getInstance(project)
            urls.mapNotNull(VirtualFileManager.getInstance()::findFileByUrl).forEach { editors.openFile(it, false) }
        }
    }

    private companion object {
        const val PLUGIN_ID = "io.github.acalebratliff.dotstudio"

        // JEP 486: from Java 24 a new component no longer records the protection domains of its callers.
        const val FIRST_JAVA_WITHOUT_CALLER_CONTEXT = 24

        // VFS URLs are not escaped, so a file name's line break stays in the URL; no file name can contain NUL.
        const val URL_SEPARATOR = "\u0000"

        fun reopenKey(project: Project): String = "dot-studio.reopen-after-reload.${project.locationHash}"
    }
}
