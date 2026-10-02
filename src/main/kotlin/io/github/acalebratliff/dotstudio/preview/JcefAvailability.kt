package io.github.acalebratliff.dotstudio.preview

import io.github.acalebratliff.dotstudio.preview.jcef.isJcefSupported

private const val JBCEF_APP = "com.intellij.ui.jcef.JBCefApp"

/**
 * True when the JCEF classes can be loaded and JCEF can run. This file references no JCEF type, so it is safe to call
 * when JCEF is missing; only after it returns true may callers touch a JCEF class.
 */
internal fun isJcefAvailable(): Boolean = jcefAvailable(
    loadJcefApp = { Class.forName(JBCEF_APP, false, DotPreviewPanel::class.java.classLoader) },
    isSupported = ::isJcefSupported,
)

internal fun jcefAvailable(loadJcefApp: () -> Unit, isSupported: () -> Boolean): Boolean {
    // On 2026.2 with the JCEF plugin disabled the classes are absent from our class path: forName throws
    // ClassNotFoundException, or NoClassDefFoundError (a LinkageError) when a class JBCefApp needs is missing.
    try {
        loadJcefApp()
    } catch (_: ClassNotFoundException) {
        return false
    } catch (_: LinkageError) {
        return false
    }
    return isSupported()
}
