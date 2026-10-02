package io.github.acalebratliff.dotstudio.preview.jcef

import io.github.acalebratliff.dotstudio.preview.RenderResult

internal data class RenderReply(val id: Long, val result: RenderResult)

/** Parses a reply sent by `reply()` in `preview/preview.js`: `id`, `kind` and body, separated by newlines. */
internal fun parseRenderReply(payload: String): RenderReply? {
    val parts = payload.split('\n', limit = 3)
    if (parts.size != 3) return null
    val id = parts[0].toLongOrNull() ?: return null
    val body = parts[2]
    val result = when (parts[1]) {
        "svg" -> RenderResult.Svg(body)
        "errors" -> RenderResult.DotErrors(body.lines().filter { it.isNotBlank() })
        "crash" -> RenderResult.EngineFailure(body)
        else -> return null
    }
    return RenderReply(id, result)
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
