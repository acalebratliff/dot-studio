package io.github.acalebratliff.dotstudio.preview

import io.github.acalebratliff.dotstudio.DotStudioBundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal class PreviewNoticeTest {
    private val text = DotStudioBundle.message("preview.jcef.unavailable")

    @Test
    fun `the notice shows its text`() {
        assertEquals(text, PreviewNotice(text).text)
    }

    @Test
    fun `the text wraps by words, and a narrower notice is taller`() {
        val notice = PreviewNotice(text)
        assertTrue(notice.area.lineWrap)
        assertTrue(notice.area.wrapStyleWord)

        val wide = heightAt(notice, 600)
        val narrow = heightAt(notice, 150)

        assertTrue("narrow $narrow should be taller than wide $wide", narrow > wide)
        assertTrue("the text must not be wider than the notice", notice.area.width <= 150)
    }

    // BorderLayout sizes the area from its preferred height before it gives it the new width, so lay out twice.
    private fun heightAt(notice: PreviewNotice, width: Int): Int {
        notice.setSize(width, 2000)
        repeat(2) { notice.doLayout() }
        return notice.area.height
    }
}
