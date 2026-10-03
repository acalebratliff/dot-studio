package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import io.github.acalebratliff.dotstudio.DotStudioBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.Nls
import java.io.IOException
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

/** Supplies the project's coroutine scope to preview panels, so renders and exports stop when the project closes. */
@Service(Service.Level.PROJECT)
internal class DotPreviewService(private val project: Project, private val scope: CoroutineScope) {
    fun createPanel(parent: Disposable): DotPreviewPanel =
        DotPreviewPanel(parent, scope) { dot -> inspectDot(project, dot) }

    /**
     * Writes [svg], the graph on show when the export started, to [target] as [format]. Failures show a dialog.
     * Closing the preview first cancels the export.
     */
    fun export(panel: DotPreviewPanel, svg: String, format: ExportFormat, target: Path) {
        panel.exports.launch {
            when (val outcome = exportBytes(svg, format, EXPORT_TIMEOUT, panel::rasterisePng)) {
                is ExportOutcome.Failed -> showError(outcome.message)
                is ExportOutcome.Bytes -> write(target, outcome.bytes)
            }
        }
    }

    private suspend fun write(target: Path, bytes: ByteArray) {
        val failure = withContext(Dispatchers.IO) {
            try {
                writeExportFile(target, bytes)
                LocalFileSystem.getInstance().refreshAndFindFileByNioFile(target)
                null
            } catch (e: IOException) {
                DotStudioBundle.message("preview.export.error.write", target, e.message.orEmpty())
            }
        }
        failure?.let { showError(it) }
    }

    private suspend fun showError(message: @Nls String) {
        withContext(Dispatchers.EDT) {
            Messages.showErrorDialog(project, message, DotStudioBundle.message("preview.export.error.title"))
        }
    }

    private companion object {
        val EXPORT_TIMEOUT = 10.seconds
    }
}
