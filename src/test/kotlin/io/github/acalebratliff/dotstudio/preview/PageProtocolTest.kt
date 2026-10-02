package io.github.acalebratliff.dotstudio.preview

import io.github.acalebratliff.dotstudio.preview.jcef.PageReply
import io.github.acalebratliff.dotstudio.preview.jcef.jsStringLiteral
import io.github.acalebratliff.dotstudio.preview.jcef.parsePageReply
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
}
