package io.github.acalebratliff.dotstudio.preview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

internal class JcefAvailabilityTest {
    private val supportNotAsked: () -> Boolean = {
        throw AssertionError("isSupported must not run without JCEF classes")
    }

    @Test
    fun `missing JCEF classes mean unavailable without asking JCEF`() {
        assertFalse(
            jcefAvailable(loadJcefApp = {
                throw ClassNotFoundException("JBCefApp")
            }, isSupported = supportNotAsked),
        )
    }

    @Test
    fun `a JCEF class that fails to link means unavailable without asking JCEF`() {
        assertFalse(
            jcefAvailable(loadJcefApp = {
                throw NoClassDefFoundError("org/cef/CefApp")
            }, isSupported = supportNotAsked),
        )
    }

    @Test
    fun `loadable but unsupported JCEF means unavailable`() {
        assertFalse(jcefAvailable(loadJcefApp = {}, isSupported = { false }))
    }

    @Test
    fun `loadable and supported JCEF means available`() {
        assertTrue(jcefAvailable(loadJcefApp = {}, isSupported = { true }))
    }
}
