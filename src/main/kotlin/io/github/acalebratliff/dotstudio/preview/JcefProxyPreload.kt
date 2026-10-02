package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.components.Service
import com.intellij.util.net.ProxySettings

/**
 * Works around a platform bug (#20). The first `JBCefBrowser` in the IDE starts JCEF inside `JBCefApp$Holder.<clinit>`,
 * which reads the proxy settings: `SettingsHelper.loadArgs` → `JBCefProxySettings` → `HttpConfigurable`, whose
 * `initializeComponent` requests `ProxyMigrationService`. If `HttpConfigurable` does not exist yet, the platform's
 * `checkOutsideClassInitializer` logs a SEVERE "Class initialization must not depend on services" and blames the
 * plugin whose frame is on the stack: ours. On 2026.2 this races the IDE's first Marketplace request, which otherwise
 * creates `HttpConfigurable` first. Reading the proxy settings beforehand creates it outside any class initializer.
 * Where a proxy-override provider is active, `ProxySettings` does not read `HttpConfigurable`, so this does nothing
 * and the SEVERE is not worked around: JCEF's own proxy code still reaches `HttpConfigurable` inside `<clinit>`.
 */
@Service
internal class JcefProxyPreload {
    // Synchronized lazy: only the first browser in the application pays for it, whichever thread creates it.
    private val proxySettingsRead by lazy<Unit> { ProxySettings.getInstance().getProxyConfiguration() }

    /** Call before creating a JCEF browser. */
    fun beforeFirstBrowser(): Unit = proxySettingsRead
}
