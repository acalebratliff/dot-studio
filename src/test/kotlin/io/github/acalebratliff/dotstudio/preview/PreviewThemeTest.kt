package io.github.acalebratliff.dotstudio.preview

import org.junit.Assert.assertEquals
import org.junit.Test
import java.awt.Color

internal class PreviewThemeTest {
    @Test
    fun `opaque colours become hex and translucent ones rgba`() {
        assertEquals("#2b2d30", cssColor(Color(0x2b, 0x2d, 0x30)))
        assertEquals("rgba(255, 0, 0, 0.502)", cssColor(Color(255, 0, 0, 128)))
    }

    @Test
    fun `a dark editor background gives a dark colour scheme`() {
        val dark = PreviewTheme(Color(0x1e1f22), Color(0xbcbec4), Color(0x402929), Color(0x5e3838))
        assertEquals(
            """dotStudio.setTheme({"background": "#1e1f22", "foreground": "#bcbec4", "errorBackground": "#402929", """ +
                """"errorBorder": "#5e3838", "colorScheme": "dark"})""",
            previewThemeScript(dark),
        )
    }

    @Test
    fun `a light editor background gives a light colour scheme`() {
        val light = PreviewTheme(Color.WHITE, Color.BLACK, Color(0xfff7f7), Color(0xf2b6bb))
        assertEquals(
            """dotStudio.setTheme({"background": "#ffffff", "foreground": "#000000", "errorBackground": "#fff7f7", """ +
                """"errorBorder": "#f2b6bb", "colorScheme": "light"})""",
            previewThemeScript(light),
        )
    }
}
