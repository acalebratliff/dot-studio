package io.github.acalebratliff.dotstudio.preview.jcef

import com.intellij.ui.jcef.JBCefApp

/** Call only after the JCEF classes are known to load (see `isJcefAvailable`). */
internal fun isJcefSupported(): Boolean = JBCefApp.isSupported()
