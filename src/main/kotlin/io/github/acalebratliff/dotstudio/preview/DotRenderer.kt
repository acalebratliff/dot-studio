package io.github.acalebratliff.dotstudio.preview

/** Turns DOT source into SVG. The seam between [PreviewPipeline] and the browser that runs viz-js. */
internal interface DotRenderer {
    /**
     * Never throws except for cancellation: every failure comes back as [RenderResult.EngineFailure], because an
     * exception would end [PreviewPipeline]'s collector for good. Cancelling the calling coroutine must stop the render.
     */
    suspend fun render(dot: String): RenderResult
}

internal sealed interface RenderResult {
    data class Svg(val svg: String) : RenderResult

    /** Graphviz rejected the input. [messages] are Graphviz's own error lines, possibly empty. */
    data class DotErrors(val messages: List<String>) : RenderResult

    /** The renderer itself failed, for example by running out of stack on a very large graph. */
    data class EngineFailure(val detail: String) : RenderResult
}
