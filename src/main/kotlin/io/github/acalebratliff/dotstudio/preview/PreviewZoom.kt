package io.github.acalebratliff.dotstudio.preview

/**
 * How the preview page shows the graph, as the page last reported it. Scales are multiples of the graph's actual size
 * (1.0 is 100%). [fitted] means the page fits the graph to the preview, and keeps doing so as the preview resizes or
 * the graph re-renders, until the user zooms. The page clamps [scale] to the limits, so equality with them is exact.
 */
internal data class PreviewZoom(val scale: Double, val minScale: Double, val maxScale: Double, val fitted: Boolean)

/** The zoom requests a toolbar action sends to the page; [pageName] is the argument `dotStudio.zoom` takes. */
internal enum class ZoomCommand(val pageName: String) {
    In("in"),
    Out("out"),
    Fit("fit"),
    ActualSize("actual"),
    ;

    /** False when the request would change nothing, or when no graph is on show ([zoom] is null). */
    fun isEnabled(zoom: PreviewZoom?): Boolean = zoom != null &&
        when (this) {
            In -> zoom.scale < zoom.maxScale
            Out -> zoom.scale > zoom.minScale
            Fit -> !zoom.fitted
            ActualSize -> zoom.scale != 1.0
        }
}
