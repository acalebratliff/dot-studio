package io.github.acalebratliff.dotstudio.preview.jcef

import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.colors.EditorColorsListener
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.ui.jcef.JBCefBrowserBase
import com.intellij.ui.jcef.JBCefJSQuery
import io.github.acalebratliff.dotstudio.DotStudioBundle
import io.github.acalebratliff.dotstudio.preview.DotRenderer
import io.github.acalebratliff.dotstudio.preview.PreviewState
import io.github.acalebratliff.dotstudio.preview.RenderResult
import io.github.acalebratliff.dotstudio.preview.currentPreviewTheme
import io.github.acalebratliff.dotstudio.preview.previewThemeScript
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandler
import org.cef.handler.CefLoadHandlerAdapter
import java.awt.BorderLayout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * The preview page in a JCEF browser. It renders DOT in a Web Worker (`preview/preview.js`) and shows the result.
 * Callers must have checked `isJcefAvailable()`. If the page cannot load, failures show as a notice instead.
 */
internal class JcefPreviewBrowser(parent: Disposable) : DotRenderer {
    private val browser = JBCefBrowser.createBuilder().build()

    // Typed as the base class to select create(JBCefBrowserBase); the create(JBCefBrowser) overload is deprecated.
    private val replyQuery = JBCefJSQuery.create(browser as JBCefBrowserBase)
    private val pageLoad = CompletableDeferred<Unit>()

    // Set before pageLoad completes, so a completed pageLoad with no failure means the page is ready.
    @Volatile private var pageFailure: String? = null

    @Volatile private var noticeShown = false

    // Built on the EDT (constructor, theme listeners) and read on the CEF thread when the page loads.
    @Volatile private var themeScript = previewThemeScript(currentPreviewTheme())
    private val root = JPanel(BorderLayout()).apply { add(browser.component) }
    private val pendingRenders = ConcurrentHashMap<Long, CompletableDeferred<RenderResult>>()
    private val nextRenderId = AtomicLong()

    val component: JComponent get() = root

    init {
        Disposer.register(parent, browser)
        Disposer.register(parent, replyQuery)
        // Runs on a CEF thread, so it only completes a deferred that a coroutine is waiting on.
        replyQuery.addHandler { payload ->
            val reply = parseRenderReply(payload)
            if (reply == null) {
                LOG.warn("Ignoring a malformed reply from the preview page")
            } else {
                pendingRenders[reply.id]?.complete(reply.result)
            }
            null
        }
        browser.jbCefClient.addRequestHandler(PreviewRequestHandler(), browser.cefBrowser)
        browser.jbCefClient.addLoadHandler(
            object : CefLoadHandlerAdapter() {
                override fun onLoadEnd(cefBrowser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
                    if (!frame.isMain || frame.url != PAGE_URL) return
                    if (httpStatusCode != HTTP_OK) {
                        failPageLoad("HTTP $httpStatusCode")
                        return
                    }
                    cefBrowser.executeJavaScript(
                        "window.dotStudioReply = function (p) { ${replyQuery.inject("p")} };",
                        PAGE_URL,
                        0,
                    )
                    // Ready first, then the theme: a theme change in between sees the page ready and applies itself,
                    // and this run picks up the newest script. Applying a theme twice is harmless.
                    pageLoad.complete(Unit)
                    cefBrowser.executeJavaScript(themeScript, PAGE_URL, 0)
                }

                override fun onLoadError(
                    cefBrowser: CefBrowser,
                    frame: CefFrame,
                    errorCode: CefLoadHandler.ErrorCode,
                    errorText: String,
                    failedUrl: String,
                ) {
                    if (frame.isMain && failedUrl == PAGE_URL) failPageLoad(errorText)
                }
            },
            browser.cefBrowser,
        )
        browser.jbCefClient.addLifeSpanHandler(
            object : CefLifeSpanHandlerAdapter() {
                // A popup browser would have none of our request handlers, so links with target=_blank or a
                // middle-click could load remote content. Returning true cancels every popup.
                override fun onBeforePopup(
                    cefBrowser: CefBrowser,
                    frame: CefFrame,
                    targetUrl: String,
                    targetFrameName: String?,
                ): Boolean {
                    LOG.debug { "Blocked a popup to $targetUrl" }
                    return true
                }
            },
            browser.cefBrowser,
        )
        val connection = ApplicationManager.getApplication().messageBus.connect(parent)
        connection.subscribe(LafManagerListener.TOPIC, LafManagerListener { applyTheme() })
        connection.subscribe(EditorColorsManager.TOPIC, EditorColorsListener { applyTheme() })
        browser.loadURL(PAGE_URL)
    }

    override suspend fun render(dot: String): RenderResult {
        pageLoad.await()
        pageFailure?.let { return RenderResult.EngineFailure(it) }
        val id = nextRenderId.incrementAndGet()
        val reply = CompletableDeferred<RenderResult>()
        pendingRenders[id] = reply
        try {
            execute("dotStudio.render($id, ${jsStringLiteral(dot)})")
            return reply.await()
        } finally {
            pendingRenders.remove(id)
            // Terminates the worker, the only way to stop a Graphviz layout that is running.
            if (!reply.isCompleted) execute("dotStudio.cancel($id)")
        }
    }

    suspend fun show(state: PreviewState) {
        if (pageLoad.isCompleted && pageFailure == null) {
            if (noticeShown) withContext(Dispatchers.EDT) { setContent(browser.component, notice = false) }
            execute(
                when (state) {
                    PreviewState.Rendering -> "dotStudio.showRendering()"
                    is PreviewState.Rendered -> "dotStudio.showSvg(${jsStringLiteral(state.svg)})"
                    is PreviewState.Failed -> "dotStudio.showMessage(${jsStringLiteral(state.message)})"
                },
            )
        } else if (state is PreviewState.Failed) {
            // The page cannot show anything yet (or ever), so the failure goes to a Swing notice instead.
            val message = pageFailure?.let { DotStudioBundle.message("preview.error.page", it) } ?: state.message
            withContext(Dispatchers.EDT) { setContent(JBPanelWithEmptyText().withEmptyText(message), notice = true) }
        }
    }

    private fun applyTheme() {
        themeScript = previewThemeScript(currentPreviewTheme())
        LOG.debug { "Theme changed: $themeScript" }
        if (pageLoad.isCompleted && pageFailure == null) execute(themeScript)
    }

    private fun setContent(content: JComponent, notice: Boolean) {
        root.removeAll()
        root.add(content)
        root.revalidate()
        root.repaint()
        noticeShown = notice
    }

    // CEF can report one failure twice (onLoadError, then onLoadEnd with the HTTP status); the first one wins.
    @Synchronized
    private fun failPageLoad(detail: String) {
        if (pageLoad.isCompleted) return
        LOG.warn("The preview page failed to load: $detail")
        pageFailure = detail
        pageLoad.complete(Unit)
    }

    private fun execute(script: String) {
        if (!browser.isDisposed) browser.cefBrowser.executeJavaScript(script, PAGE_URL, 0)
    }

    private companion object {
        const val HTTP_OK = 200
        val LOG = logger<JcefPreviewBrowser>()
    }
}
