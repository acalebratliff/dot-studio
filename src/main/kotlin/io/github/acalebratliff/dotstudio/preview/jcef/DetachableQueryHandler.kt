package io.github.acalebratliff.dotstudio.preview.jcef

import com.intellij.ui.jcef.JBCefJSQuery
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.lang.invoke.VolatileCallSite
import java.util.function.Function
import java.lang.invoke.MethodHandleProxies.asInterfaceInstance as proxyOf

/**
 * A `JBCefJSQuery` handler that stops referring to [target] once [detach] is called.
 *
 * On 2025.2, JCEF keeps every handler added to a query in a JNI global reference for good, even after
 * `removeHandler` and `dispose` (#26). A handler of one of our classes would keep the plugin's class loader alive,
 * so the plugin could not be uninstalled without a restart. So [handler] is a JDK proxy over a call site, with no
 * class of ours, and [detach] points the call site away from [target].
 */
internal class DetachableQueryHandler(target: (String) -> JBCefJSQuery.Response?) {
    // Volatile, not Mutable: detach() runs on the disposing thread and the handler on a CEF thread, and a
    // MutableCallSite gives other threads no guarantee of seeing a new target without MutableCallSite.syncAll.
    private val site = VolatileCallSite(MethodType.methodType(Any::class.java, Any::class.java))

    val handler: Function<String, JBCefJSQuery.Response?>

    init {
        site.target = MethodHandles.publicLookup()
            .findVirtual(Function1::class.java, "invoke", MethodType.methodType(Any::class.java, Any::class.java))
            .bindTo(target)
        // The JDK defines the proxy class in the context class loader when the interface is a JDK one, so that
        // loader must not be ours. The platform's own loader outlives the plugin.
        val thread = Thread.currentThread()
        val previous = thread.contextClassLoader
        thread.contextClassLoader = JBCefJSQuery::class.java.classLoader
        try {
            // The proxy implements the raw Function interface; JBCefJSQuery only ever passes it a String and the
            // target returns a Response or null, so the generic types hold.
            @Suppress("UNCHECKED_CAST")
            handler = proxyOf(Function::class.java, site.dynamicInvoker()) as Function<String, JBCefJSQuery.Response?>
        } finally {
            thread.contextClassLoader = previous
        }
    }

    /** After this, [handler] answers every call with null (no response) and no longer refers to the target. */
    fun detach() {
        site.target = MethodHandles.dropArguments(MethodHandles.constant(Any::class.java, null), 0, Any::class.java)
    }
}
