package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBPanelWithEmptyText
import io.github.acalebratliff.dotstudio.DotStudioBundle
import io.github.acalebratliff.dotstudio.preview.jcef.JcefPreviewBrowser
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * The live preview of one DOT document. Renders and exports run in [scope] and stop when [parent] is disposed.
 * Without JCEF it shows a notice instead; JCEF classes are touched only after [isJcefAvailable] passes.
 */
internal class DotPreviewPanel(parent: Disposable, scope: CoroutineScope) {
    private val pipeline: PreviewPipeline?
    private val browser: JcefPreviewBrowser?
    val component: JComponent

    /** Exports of this preview; they stop when [parent] is disposed. */
    val exports = PreviewExports(scope).also { Disposer.register(parent, it) }

    /** The SVG of the graph on show, or null when nothing has rendered successfully. */
    val renderedSvg: String? get() = browser?.renderedSvg

    /** Why the graph cannot be exported now, or null when it can. */
    val exportUnavailableReason: @Nls String? get() = exportUnavailableReason(
        jcefAvailable = browser != null,
        renderedSvg,
    )

    /** False when there is no browser, so the page can never be zoomed. */
    val canZoom: Boolean get() = browser != null

    /** The page's zoom, or null while no graph is on show. */
    val zoom: PreviewZoom? get() = browser?.zoom

    init {
        if (isJcefAvailable()) {
            service<JcefProxyPreload>().beforeFirstBrowser()
            val jcefBrowser = JcefPreviewBrowser(parent)
            browser = jcefBrowser
            pipeline = PreviewPipeline(scope, jcefBrowser, jcefBrowser::show)
            Disposer.register(parent, pipeline)
            component = jcefBrowser.component
        } else {
            LOG.warn("JCEF is unavailable or not supported, so the DOT preview shows a notice instead")
            pipeline = null
            browser = null
            component = JBPanelWithEmptyText().withEmptyText(DotStudioBundle.message("preview.jcef.unavailable"))
        }
    }

    /** [dot] must be an immutable snapshot, such as `Document.getImmutableCharSequence()`. */
    fun showSource(dot: CharSequence) {
        pipeline?.submit(dot)
    }

    fun zoom(command: ZoomCommand) {
        browser?.zoom(command)
    }

    suspend fun rasterisePng(svg: String, scale: Int): PngResult = browser?.rasterisePng(svg, scale)
        ?: PngResult.Failed(DotStudioBundle.message("preview.export.unavailable.jcef"))

    private companion object {
        val LOG = logger<DotPreviewPanel>()
    }
}
