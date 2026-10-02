package io.github.acalebratliff.dotstudio.preview.jcef

import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCallback
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceHandler
import org.cef.handler.CefResourceHandlerAdapter
import org.cef.handler.CefResourceRequestHandler
import org.cef.handler.CefResourceRequestHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse

// Never resolved over the network: every request is answered or cancelled by PreviewRequestHandler.
internal const val PREVIEW_ORIGIN = "https://dot-studio.preview"
internal const val PAGE_URL = "$PREVIEW_ORIGIN/index.html"

private const val CSP = "default-src 'none'; script-src $PREVIEW_ORIGIN 'wasm-unsafe-eval'; " +
    "worker-src $PREVIEW_ORIGIN; style-src $PREVIEW_ORIGIN; img-src $PREVIEW_ORIGIN; connect-src $PREVIEW_ORIGIN; " +
    "base-uri 'none'; form-action 'none'"
private val RESOURCE_PATH = Regex("""[a-z0-9-]+(/[a-z0-9-]+)*\.(html|js|css)""")

/**
 * Serves the preview page from plugin resources at [PREVIEW_ORIGIN] and blocks everything else. The frame may only
 * ever show [PAGE_URL]: a link in the graph, even to another path on our origin, would strand later renders.
 */
internal class PreviewRequestHandler : CefRequestHandlerAdapter() {
    private val resourceRequestHandler = ResourceRequestHandler()

    override fun onBeforeBrowse(
        browser: CefBrowser?,
        frame: CefFrame?,
        request: CefRequest,
        userGesture: Boolean,
        isRedirect: Boolean,
    ): Boolean {
        val blocked = request.url != PAGE_URL
        if (blocked) LOG.debug { "Blocked navigation to ${request.url}" }
        return blocked
    }

    override fun getResourceRequestHandler(
        browser: CefBrowser?,
        frame: CefFrame?,
        request: CefRequest,
        isNavigation: Boolean,
        isDownload: Boolean,
        requestInitiator: String?,
        disableDefaultHandling: BoolRef?,
    ): CefResourceRequestHandler = resourceRequestHandler

    private companion object {
        val LOG = logger<PreviewRequestHandler>()
    }
}

private class ResourceRequestHandler : CefResourceRequestHandlerAdapter() {
    override fun onBeforeResourceLoad(browser: CefBrowser?, frame: CefFrame?, request: CefRequest): Boolean =
        !isOwnUrl(request.url)

    override fun getResourceHandler(browser: CefBrowser?, frame: CefFrame?, request: CefRequest): CefResourceHandler =
        ResourceHandler(request.url.removePrefix("$PREVIEW_ORIGIN/").substringBefore('?').substringBefore('#'))
}

private fun isOwnUrl(url: String): Boolean = url.startsWith("$PREVIEW_ORIGIN/")

/** Answers one request. Unknown paths, `/favicon.ico` among them, get a 404. */
private class ResourceHandler(private val path: String) : CefResourceHandlerAdapter() {
    private var bytes: ByteArray? = null
    private var offset = 0

    override fun processRequest(request: CefRequest, callback: CefCallback): Boolean {
        bytes = if (RESOURCE_PATH.matches(path)) {
            ResourceHandler::class.java.classLoader.getResourceAsStream("preview/$path")?.use { it.readBytes() }
        } else {
            null
        }
        callback.Continue()
        return true
    }

    override fun getResponseHeaders(response: CefResponse, responseLength: IntRef, redirectUrl: StringRef?) {
        val body = bytes
        response.setHeaderMap(mapOf("Content-Security-Policy" to CSP, "X-Content-Type-Options" to "nosniff"))
        if (body == null) {
            response.status = 404
            response.mimeType = "text/plain"
            responseLength.set(0)
            return
        }
        response.status = 200
        response.mimeType = when (path.substringAfterLast('.')) {
            "html" -> "text/html"
            "css" -> "text/css"
            else -> "text/javascript"
        }
        responseLength.set(body.size)
    }

    override fun readResponse(
        dataOut: ByteArray,
        bytesToRead: Int,
        bytesRead: IntRef,
        callback: CefCallback?,
    ): Boolean {
        val body = bytes
        if (body == null || offset >= body.size) {
            bytesRead.set(0)
            return false
        }
        val count = minOf(bytesToRead, body.size - offset)
        System.arraycopy(body, offset, dataOut, 0, count)
        offset += count
        bytesRead.set(count)
        return true
    }
}
