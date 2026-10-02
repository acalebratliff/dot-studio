package io.github.acalebratliff.dotstudio.preview

import io.github.acalebratliff.dotstudio.DotStudioBundle
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Files
import java.util.Base64
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class PreviewExportTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val png =
        byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 13, 10, 26, 10, 0, 0)

    @Test
    fun `export is enabled only with JCEF and a successful render`() {
        assertEquals(DotStudioBundle.message("preview.export.unavailable.jcef"), exportUnavailableReason(false, null))
        assertEquals(DotStudioBundle.message("preview.export.unavailable.render"), exportUnavailableReason(true, null))
        assertNull(exportUnavailableReason(true, "<svg/>"))
    }

    @Test
    fun `an SVG export is exactly the rendered SVG as UTF-8`() = runBlocking {
        val svg = "<svg><text>naïve → 名前</text></svg>"
        val outcome = exportBytes(svg, ExportFormat.Svg, 1.seconds) { _, _ -> error("SVG is never rasterised") }
        assertArrayEquals(svg.toByteArray(Charsets.UTF_8), (outcome as ExportOutcome.Bytes).bytes)
    }

    @Test
    fun `a PNG export rasterises the given SVG at the chosen scale`() = runBlocking {
        val outcome = exportBytes("<svg/>", ExportFormat.Png(scale = 2), 1.seconds) { svg, scale ->
            assertEquals("<svg/>", svg)
            assertEquals(2, scale)
            PngResult.Image(png)
        }
        assertArrayEquals(png, (outcome as ExportOutcome.Bytes).bytes)
    }

    @Test
    fun `a failed or slow rasterisation fails the export with a message`() = runBlocking {
        val failed = exportBytes("<svg/>", ExportFormat.Png(scale = 1), 1.seconds) { _, _ -> PngResult.Failed("why") }
        assertEquals(ExportOutcome.Failed("why"), failed)
        val slow = exportBytes("<svg/>", ExportFormat.Png(scale = 1), 50.milliseconds) { _, _ -> awaitCancellation() }
        assertEquals(ExportOutcome.Failed(DotStudioBundle.message("preview.export.error.timeout", 0L)), slow)
    }

    @Test
    fun `page failure codes map to bundle messages`() {
        assertEquals(
            DotStudioBundle.message("preview.export.error.too.large", "20000×300"),
            pngFailure("too-large", "20000×300", scale = 1).message,
        )
        assertEquals(
            DotStudioBundle.message("preview.export.error.too.large.scale", 2, "20000×300"),
            pngFailure("too-large-scale", "20000×300", scale = 2).message,
        )
        assertEquals(DotStudioBundle.message("preview.export.error.image"), pngFailure("image", "", 1).message)
        // Unknown codes, and draw errors, keep the page's raw text as the detail.
        assertEquals(
            DotStudioBundle.message("preview.export.error.png", "RangeError"),
            pngFailure("draw", "RangeError", 1).message,
        )
    }

    @Test
    fun `disposing the preview cancels a running export so it reports nothing`() = runBlocking {
        val exports = PreviewExports(this)
        val reply = CompletableDeferred<Unit>()
        var reported = false
        val job = exports.launch {
            reply.await()
            reported = true
        }
        yield()
        exports.dispose()
        reply.complete(Unit)
        job.join()
        assertTrue(job.isCancelled)
        assertFalse(reported)
        // An export started after disposal never runs.
        val late = exports.launch { reported = true }
        late.join()
        assertFalse(reported)
    }

    @Test
    fun `only base64 that decodes to a PNG is accepted from the page`() {
        assertArrayEquals(png, (pngResult(Base64.getEncoder().encodeToString(png)) as PngResult.Image).bytes)
        // An empty canvas gives "data:," and so an empty body.
        val notPng = PngResult.Failed(DotStudioBundle.message("preview.export.error.not.png"))
        assertEquals(notPng, pngResult(""))
        assertEquals(notPng, pngResult(Base64.getEncoder().encodeToString("<svg/>".toByteArray())))
        assertEquals(notPng, pngResult("not base64!"))
    }

    @Test
    fun `writing an export creates the file or replaces an existing one`() {
        val target = folder.root.toPath().resolve("graph.svg")
        writeExportFile(target, "first, longer content".toByteArray())
        writeExportFile(target, "second".toByteArray())
        assertEquals("second", Files.readString(target))
    }
}
