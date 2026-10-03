package io.github.acalebratliff.dotstudio.preview.jcef

import io.github.acalebratliff.dotstudio.DotStudioBundle
import io.github.acalebratliff.dotstudio.preview.PreviewState
import io.github.acalebratliff.dotstudio.preview.PreviewZoom
import io.github.acalebratliff.dotstudio.preview.RenderResult
import io.github.acalebratliff.dotstudio.preview.ZoomCommand

/**
 * A message from `reply()` in `preview/preview.js`. Render and PNG replies answer the request with the same `id`;
 * the page sends [Zoom] by itself, with id 0, whenever the zoom of the graph on show changes.
 */
internal sealed interface PageReply {
    data class Render(val id: Long, val result: RenderResult) : PageReply

    /** The answer to a `rasterise` request. */
    sealed interface PngReply : PageReply {
        val id: Long
    }

    /** [base64] is the PNG from `canvas.toDataURL`, without the `data:` prefix. Not yet checked to be a PNG. */
    data class Png(override val id: Long, val base64: String) : PngReply

    /** [code] says why (see `pngFailure`); [detail] is the page's own text for it, possibly empty. */
    data class PngFailed(override val id: Long, val code: String, val detail: String) : PngReply

    data class Zoom(val zoom: PreviewZoom) : PageReply
}

/** Parses a reply sent by `reply()` in `preview/preview.js`: `id`, `kind` and body, separated by newlines. */
internal fun parsePageReply(payload: String): PageReply? {
    val parts = payload.split('\n', limit = 3)
    if (parts.size != 3) return null
    val id = parts[0].toLongOrNull() ?: return null
    val body = parts[2]
    return when (parts[1]) {
        "svg" -> PageReply.Render(id, RenderResult.Svg(body))
        "errors" -> PageReply.Render(id, RenderResult.DotErrors(body.lines().filter { it.isNotBlank() }))
        "crash" -> PageReply.Render(id, RenderResult.EngineFailure(body))
        "png" -> PageReply.Png(id, body)
        "png-error" -> body.split('\n', limit = 2).let { PageReply.PngFailed(id, it[0], it.getOrElse(1) { "" }) }
        "zoom" -> parseZoom(body)?.let { PageReply.Zoom(it) }
        else -> null
    }
}

// Body: scale, minimum and maximum scale, and 1 or 0 for fitted, one per line.
private fun parseZoom(body: String): PreviewZoom? {
    val fields = body.split('\n')
    if (fields.size != ZOOM_FIELDS) return null
    val (scale, min, max) = fields.take(ZOOM_FIELDS - 1).map { field ->
        field.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0 } ?: return null
    }
    if (min > max || scale !in min..max) return null
    val fitted = when (fields.last()) {
        "1" -> true
        "0" -> false
        else -> return null
    }
    return PreviewZoom(scale, min, max, fitted)
}

private const val ZOOM_FIELDS = 4

/** The page call that carries out [command]. */
internal fun zoomScript(command: ZoomCommand): String = "dotStudio.zoom(${jsStringLiteral(command.pageName)})"

/** Encodes [value] as a JavaScript string literal, so text reaches the page as data and never as code. */
internal fun jsStringLiteral(value: String): String = buildString(value.length + 2) {
    append('"')
    for (c in value) {
        when {
            c == '"' -> append("\\\"")
            c == '\\' -> append("\\\\")
            c < ' ' || c == '\u2028' || c == '\u2029' -> append("\\u").append(c.code.toString(16).padStart(4, '0'))
            else -> append(c)
        }
    }
    append('"')
}

/**
 * What replaces a preview whose page cannot show [state], or null for nothing: a failed page load is the notice for
 * both a failed render and an empty source, so that an empty file does not leave a blank browser. Before the page
 * has loaded ([pageFailure] null) only a failed render has anything to say.
 */
internal fun swingNoticeText(state: PreviewState, pageFailure: String?): String? = when {
    pageFailure != null && state != PreviewState.Rendering ->
        DotStudioBundle.message("preview.error.page", pageFailure)

    state is PreviewState.Failed -> state.message

    else -> null
}
