package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBPanelWithEmptyText
import io.github.acalebratliff.dotstudio.DotStudioBundle
import io.github.acalebratliff.dotstudio.preview.jcef.JcefPreviewBrowser
import kotlinx.coroutines.CoroutineScope
import javax.swing.JComponent

/**
 * The live preview of one DOT document. Renders run in [scope] and stop when [parent] is disposed.
 * Without JCEF it shows a notice instead; JCEF classes are touched only after [isJcefAvailable] passes.
 */
internal class DotPreviewPanel(parent: Disposable, scope: CoroutineScope) {
    private val pipeline: PreviewPipeline?
    val component: JComponent

    init {
        if (isJcefAvailable()) {
            val browser = JcefPreviewBrowser(parent)
            pipeline = PreviewPipeline(scope, browser, browser::show)
            Disposer.register(parent, pipeline)
            component = browser.component
        } else {
            LOG.warn("JCEF is unavailable or not supported, so the DOT preview shows a notice instead")
            pipeline = null
            component = JBPanelWithEmptyText().withEmptyText(DotStudioBundle.message("preview.jcef.unavailable"))
        }
    }

    fun showSource(dot: String) {
        pipeline?.submit(dot)
    }

    private companion object {
        val LOG = logger<DotPreviewPanel>()
    }
}
