package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.Disposable
import io.github.acalebratliff.dotstudio.DotStudioBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.annotations.Nls
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64
import kotlin.time.Duration

/** What an export writes. A PNG is rasterised at [scale] times the size the preview shows the graph at. */
internal sealed interface ExportFormat {
    val extension: String

    data object Svg : ExportFormat {
        override val extension = "svg"
    }

    data class Png(val scale: Int) : ExportFormat {
        override val extension = "png"
    }
}

internal sealed interface PngResult {
    class Image(val bytes: ByteArray) : PngResult

    data class Failed(val message: @Nls String) : PngResult
}

private val PNG_SIGNATURE =
    byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10)

/** Decodes the page's base64 PNG, rejecting anything that is not one (an empty canvas gives an empty string). */
internal fun pngResult(base64: String): PngResult {
    val bytes = try {
        Base64.getDecoder().decode(base64)
    } catch (_: IllegalArgumentException) {
        return PngResult.Failed(DotStudioBundle.message("preview.export.error.not.png"))
    }
    val isPng = bytes.size > PNG_SIGNATURE.size && bytes.copyOf(PNG_SIGNATURE.size).contentEquals(PNG_SIGNATURE)
    return if (isPng) {
        PngResult.Image(
            bytes,
        )
    } else {
        PngResult.Failed(DotStudioBundle.message("preview.export.error.not.png"))
    }
}

/**
 * Maps a failure the page reports for a PNG at [scale] to a message. [code] is one of the codes `rasterise` in
 * `preview/preview.js` sends; [detail] is the page's own text (pixel size or JS error), shown as-is.
 */
internal fun pngFailure(code: String, detail: String, scale: Int): PngResult.Failed = PngResult.Failed(
    when (code) {
        "too-large" -> DotStudioBundle.message("preview.export.error.too.large", detail)
        "too-large-scale" -> DotStudioBundle.message("preview.export.error.too.large.scale", scale, detail)
        "image" -> DotStudioBundle.message("preview.export.error.image")
        else -> DotStudioBundle.message("preview.export.error.png", detail)
    },
)

/** Why export is disabled, shown as the action's description, or null when the graph on show can be exported. */
internal fun exportUnavailableReason(jcefAvailable: Boolean, renderedSvg: String?): @Nls String? = when {
    !jcefAvailable -> DotStudioBundle.message("preview.export.unavailable.jcef")
    renderedSvg == null -> DotStudioBundle.message("preview.export.unavailable.render")
    else -> null
}

/** Writes [bytes] to [target], replacing an existing file. Does file I/O, so never call it on the EDT. */
internal fun writeExportFile(target: Path, bytes: ByteArray) {
    Files.write(target, bytes)
}

internal sealed interface ExportOutcome {
    class Bytes(val bytes: ByteArray) : ExportOutcome

    data class Failed(val message: @Nls String) : ExportOutcome
}

/**
 * The file contents for exporting [svg] as [format]. An SVG is exactly the rendered SVG; a PNG comes from [rasterise],
 * which is given [timeout] before the export fails.
 */
internal suspend fun exportBytes(
    svg: String,
    format: ExportFormat,
    timeout: Duration,
    rasterise: suspend (svg: String, scale: Int) -> PngResult,
): ExportOutcome = when (format) {
    ExportFormat.Svg -> ExportOutcome.Bytes(svg.toByteArray(Charsets.UTF_8))

    is ExportFormat.Png -> when (val png = withTimeoutOrNull(timeout) { rasterise(svg, format.scale) }) {
        null -> ExportOutcome.Failed(DotStudioBundle.message("preview.export.error.timeout", timeout.inWholeSeconds))
        is PngResult.Failed -> ExportOutcome.Failed(png.message)
        is PngResult.Image -> ExportOutcome.Bytes(png.bytes)
    }
}

/**
 * Runs one preview's exports in [scope]. Disposing cancels the exports still running, so closing the preview
 * mid-export neither writes a file nor reports a late failure.
 */
internal class PreviewExports(private val scope: CoroutineScope) : Disposable {
    // A child of the scope's job, so closing the project cancels exports too; a failed export cancels no other.
    private val jobs = SupervisorJob(scope.coroutineContext.job)

    fun launch(block: suspend CoroutineScope.() -> Unit): Job = scope.launch(jobs, block = block)

    override fun dispose() {
        jobs.cancel()
    }
}
