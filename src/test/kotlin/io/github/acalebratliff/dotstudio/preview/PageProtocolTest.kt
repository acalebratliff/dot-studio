package io.github.acalebratliff.dotstudio.preview

import io.github.acalebratliff.dotstudio.DotStudioBundle
import io.github.acalebratliff.dotstudio.preview.jcef.PageReply
import io.github.acalebratliff.dotstudio.preview.jcef.jsStringLiteral
import io.github.acalebratliff.dotstudio.preview.jcef.parsePageReply
import io.github.acalebratliff.dotstudio.preview.jcef.swingNoticeText
import io.github.acalebratliff.dotstudio.preview.jcef.zoomScript
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class PageProtocolTest {
    @Test
    fun `an SVG reply keeps newlines in the body`() {
        assertEquals(PageReply.Render(7, RenderResult.Svg("<svg>\n</svg>")), parsePageReply("7\nsvg\n<svg>\n</svg>"))
    }

    @Test
    fun `an error reply becomes one message per non-blank line`() {
        val expected = PageReply.Render(2, RenderResult.DotErrors(listOf("syntax error in line 1", "second")))
        assertEquals(expected, parsePageReply("2\nerrors\nsyntax error in line 1\n\nsecond"))
        assertEquals(PageReply.Render(3, RenderResult.DotErrors(emptyList())), parsePageReply("3\nerrors\n"))
    }

    @Test
    fun `a crash reply carries the detail`() {
        assertEquals(
            PageReply.Render(4, RenderResult.EngineFailure("RangeError")),
            parsePageReply("4\ncrash\nRangeError"),
        )
    }

    @Test
    fun `PNG replies carry the base64 body or the failure code and detail`() {
        assertEquals(PageReply.Png(5, "iVBORw0KGgo="), parsePageReply("5\npng\niVBORw0KGgo="))
        assertEquals(
            PageReply.PngFailed(6, "draw", "Error: x\ny"),
            parsePageReply("6\npng-error\ndraw\nError: x\ny"),
        )
        assertEquals(PageReply.PngFailed(8, "image", ""), parsePageReply("8\npng-error\nimage"))
    }

    @Test
    fun `a zoom reply carries the scale, its limits and whether the graph is fitted`() {
        assertEquals(
            PageReply.Zoom(PreviewZoom(scale = 0.75, minScale = 0.1, maxScale = 8.0, fitted = true)),
            parsePageReply("0\nzoom\n0.75\n0.1\n8\n1"),
        )
        assertEquals(
            PageReply.Zoom(PreviewZoom(scale = 0.02, minScale = 0.02, maxScale = 8.0, fitted = false)),
            parsePageReply("0\nzoom\n0.02\n0.02\n8\n0"),
        )
    }

    @Test
    fun `malformed zoom replies are rejected`() {
        for (body in listOf(
            "", "1\n0.1\n8", "1\n0.1\n8\nyes", "x\n0.1\n8\n1", "NaN\n0.1\n8\n1", "0\n0\n8\n1",
            "9\n0.1\n8\n1", "1\n2\n1\n0", "Infinity\n0.1\nInfinity\n0", "1\n0.1\n8\n1\n",
        )) {
            assertNull(body, parsePageReply("0\nzoom\n$body"))
        }
    }

    @Test
    fun `zoom commands reach the page as string literals`() {
        assertEquals("dotStudio.zoom(\"in\")", zoomScript(ZoomCommand.In))
        assertEquals("dotStudio.zoom(\"out\")", zoomScript(ZoomCommand.Out))
        assertEquals("dotStudio.zoom(\"fit\")", zoomScript(ZoomCommand.Fit))
        assertEquals("dotStudio.zoom(\"actual\")", zoomScript(ZoomCommand.ActualSize))
    }

    @Test
    fun `malformed replies are rejected`() {
        assertNull(parsePageReply(""))
        assertNull(parsePageReply("x\nsvg\n<svg/>"))
        assertNull(parsePageReply("1\nscript\nalert(1)"))
        assertNull(parsePageReply("1\nsvg"))
    }

    @Test
    fun `DOT source is encoded as a string literal that cannot end early`() {
        val dot = "digraph { a [label=\"x\\\"); alert(1); (\"\"]; }\n\u2028\u0000"
        assertEquals(
            "\"digraph { a [label=\\\"x\\\\\\\"); alert(1); (\\\"\\\"]; }\\u000a\\u2028\\u0000\"",
            jsStringLiteral(dot),
        )
    }

    @Test
    fun `after a page load failure an empty source shows the failure notice, not a blank browser`() {
        val expected = DotStudioBundle.message("preview.error.page", "HTTP 404")
        assertEquals(expected, swingNoticeText(PreviewState.Empty, "HTTP 404"))
        assertEquals(expected, swingNoticeText(PreviewState.Failed("render failed"), "HTTP 404"))
        assertNull(swingNoticeText(PreviewState.Rendering, "HTTP 404"))
    }

    @Test
    fun `before the page has loaded only a failed render goes to a notice`() {
        assertEquals("render failed", swingNoticeText(PreviewState.Failed("render failed"), null))
        assertNull(swingNoticeText(PreviewState.Empty, null))
        assertNull(swingNoticeText(PreviewState.Rendered("<svg/>"), null))
    }
}
