package io.github.acalebratliff.dotstudio.preview.jcef

import io.github.acalebratliff.dotstudio.preview.RenderResult

/** A message from `reply()` in `preview/preview.js`. Every reply answers the request with the same [id]. */
internal sealed interface PageReply {
    val id: Long

    data class Render(override val id: Long, val result: RenderResult) : PageReply

    /** The answer to a `rasterise` request. */
    sealed interface PngReply : PageReply

    /** [base64] is the PNG from `canvas.toDataURL`, without the `data:` prefix. Not yet checked to be a PNG. */
    data class Png(override val id: Long, val base64: String) : PngReply

    /** [code] says why (see `pngFailure`); [detail] is the page's own text for it, possibly empty. */
    data class PngFailed(override val id: Long, val code: String, val detail: String) : PngReply
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
        else -> null
    }
}

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
