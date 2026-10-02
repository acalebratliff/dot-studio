package io.github.acalebratliff.dotstudio.preview

import io.github.acalebratliff.dotstudio.preview.jcef.RenderReply
import io.github.acalebratliff.dotstudio.preview.jcef.jsStringLiteral
import io.github.acalebratliff.dotstudio.preview.jcef.parseRenderReply
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class PageProtocolTest {
    @Test
    fun `an SVG reply keeps newlines in the body`() {
        assertEquals(RenderReply(7, RenderResult.Svg("<svg>\n</svg>")), parseRenderReply("7\nsvg\n<svg>\n</svg>"))
    }

    @Test
    fun `an error reply becomes one message per non-blank line`() {
        val expected = RenderReply(2, RenderResult.DotErrors(listOf("syntax error in line 1", "second")))
        assertEquals(expected, parseRenderReply("2\nerrors\nsyntax error in line 1\n\nsecond"))
        assertEquals(RenderReply(3, RenderResult.DotErrors(emptyList())), parseRenderReply("3\nerrors\n"))
    }

    @Test
    fun `a crash reply carries the detail`() {
        assertEquals(RenderReply(4, RenderResult.EngineFailure("RangeError")), parseRenderReply("4\ncrash\nRangeError"))
    }

    @Test
    fun `malformed replies are rejected`() {
        assertNull(parseRenderReply(""))
        assertNull(parseRenderReply("x\nsvg\n<svg/>"))
        assertNull(parseRenderReply("1\nscript\nalert(1)"))
        assertNull(parseRenderReply("1\nsvg"))
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
