package io.github.acalebratliff.dotstudio.preview.jcef

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.invoke.MethodHandleProxies
import java.lang.ref.WeakReference

internal class DetachableQueryHandlerTest {
    @Test
    fun `the handler forwards payloads until detached`() {
        val received = mutableListOf<String>()
        val handler = DetachableQueryHandler {
            received += it
            null
        }
        assertNull(handler.handler.apply("one"))
        handler.detach()
        assertNull(handler.handler.apply("two"))
        assertEquals(listOf("one"), received)
    }

    @Test
    fun `the handler JCEF keeps is not a plugin class and forgets the target once detached`() {
        val (handler, target) = handlerWithCollectableTarget()
        // A JDK wrapper, not a class of ours (in tests both would share one class loader, so check the kind).
        assertTrue(MethodHandleProxies.isWrapperInstance(handler.handler))
        handler.detach()
        repeat(GC_ATTEMPTS) {
            if (target.get() == null) return@repeat
            System.gc()
            Thread.sleep(GC_PAUSE_MS)
        }
        assertTrue("the detached handler still references its target", target.get() == null)
        // Equality and hashing (JBCefJSQuery keeps handlers in a map) work on the proxy.
        assertEquals(handler.handler, handler.handler)
        handler.handler.hashCode()
    }

    private fun handlerWithCollectableTarget(): Pair<DetachableQueryHandler, WeakReference<Any>> {
        val marker = Any()
        val target: (String) -> Nothing? = {
            marker.hashCode()
            null
        }
        return DetachableQueryHandler(target) to WeakReference(target)
    }

    private companion object {
        const val GC_ATTEMPTS = 20
        const val GC_PAUSE_MS = 50L
    }
}
