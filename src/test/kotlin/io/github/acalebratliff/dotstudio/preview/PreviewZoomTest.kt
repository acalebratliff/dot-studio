package io.github.acalebratliff.dotstudio.preview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class PreviewZoomTest {
    private fun zoom(scale: Double, fitted: Boolean = false) =
        PreviewZoom(scale, minScale = 0.1, maxScale = 8.0, fitted)

    @Test
    fun `nothing is enabled while no graph is on show`() {
        for (command in ZoomCommand.entries) assertFalse(command.name, command.isEnabled(null))
    }

    @Test
    fun `zooming in and out stop at the limits`() {
        assertTrue(ZoomCommand.In.isEnabled(zoom(7.9)))
        assertFalse(ZoomCommand.In.isEnabled(zoom(8.0)))
        assertTrue(ZoomCommand.Out.isEnabled(zoom(0.2)))
        assertFalse(ZoomCommand.Out.isEnabled(zoom(0.1)))
    }

    @Test
    fun `fit is disabled while the graph is fitted`() {
        assertFalse(ZoomCommand.Fit.isEnabled(zoom(0.5, fitted = true)))
        assertTrue(ZoomCommand.Fit.isEnabled(zoom(0.5)))
        // A small graph fits at 100%; zooming in from there leaves fitting.
        assertTrue(ZoomCommand.Fit.isEnabled(zoom(1.0)))
    }

    @Test
    fun `actual size is disabled at exactly 100 percent`() {
        assertFalse(ZoomCommand.ActualSize.isEnabled(zoom(1.0, fitted = true)))
        assertTrue(ZoomCommand.ActualSize.isEnabled(zoom(0.5, fitted = true)))
        assertTrue(ZoomCommand.ActualSize.isEnabled(zoom(1.25)))
    }
}
