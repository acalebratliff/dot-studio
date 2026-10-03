package io.github.acalebratliff.dotstudio.preview

import io.github.acalebratliff.dotstudio.DotStudioBundle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class PreviewPipelineTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val renderer = FakeRenderer()
    private val states = Channel<PreviewState>(Channel.UNLIMITED)

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `a burst of edits renders only the last one`() = runBlocking {
        val pipeline = pipeline(debounce = 500.milliseconds)
        pipeline.submit("a")
        delay(50.milliseconds)
        pipeline.submit("b")
        delay(50.milliseconds)
        pipeline.submit("c")

        assertEquals("c", renderer.started.receiveWithin())
        renderer.replyTo("c").complete(RenderResult.Svg("<svg>c</svg>"))
        assertEquals(PreviewState.Rendering, states.receiveWithin())
        assertEquals(PreviewState.Rendered("<svg>c</svg>"), states.receiveWithin())
        assertTrue(renderer.started.tryReceive().isFailure)
    }

    @Test
    fun `a newer edit cancels the render in progress and its result is discarded`() = runBlocking {
        val pipeline = pipeline()
        pipeline.submit("old")
        assertEquals("old", renderer.started.receiveWithin())
        pipeline.submit("new")
        assertEquals("new", renderer.started.receiveWithin())
        assertEquals("old", renderer.cancelled.receiveWithin())

        renderer.replyTo("old").complete(RenderResult.Svg("<svg>old</svg>"))
        renderer.replyTo("new").complete(RenderResult.Svg("<svg>new</svg>"))
        assertEquals(PreviewState.Rendering, states.receiveWithin())
        assertEquals(PreviewState.Rendering, states.receiveWithin())
        assertEquals(PreviewState.Rendered("<svg>new</svg>"), states.receiveWithin())
        assertTrue(states.tryReceive().isFailure)
    }

    @Test
    fun `dispose cancels the render in progress and ignores later edits`() = runBlocking {
        val pipeline = pipeline()
        pipeline.submit("a")
        assertEquals("a", renderer.started.receiveWithin())

        pipeline.dispose()
        assertEquals("a", renderer.cancelled.receiveWithin())
        pipeline.submit("b")
        delay(200.milliseconds)
        assertTrue(renderer.started.tryReceive().isFailure)
    }

    @Test
    fun `a render that exceeds the timeout is cancelled and reported`() = runBlocking {
        pipeline(timeout = 100.milliseconds).submit("slow")

        assertEquals(PreviewState.Rendering, states.receiveWithin())
        val expected = DotStudioBundle.message("preview.error.timeout", 0L)
        assertEquals(PreviewState.Failed(expected), states.receiveWithin())
        assertEquals("slow", renderer.cancelled.receiveWithin())
    }

    @Test
    fun `Graphviz errors map to the DOT error message`() = runBlocking {
        val failed = renderOnce(RenderResult.DotErrors(listOf("syntax error in line 1 near '}'")))
        assertEquals(DotStudioBundle.message("preview.error.dot", "syntax error in line 1 near '}'"), failed.message)
        assertTrue(failed.message.contains("syntax error in line 1"))
    }

    @Test
    fun `Graphviz failure without messages maps to the generic DOT error message`() = runBlocking {
        val failed = renderOnce(RenderResult.DotErrors(emptyList()))
        assertEquals(DotStudioBundle.message("preview.error.dot.unspecified"), failed.message)
    }

    @Test
    fun `an engine failure maps to the engine error message`() = runBlocking {
        val failed = renderOnce(RenderResult.EngineFailure("RangeError: Maximum call stack size exceeded"))
        assertEquals(
            DotStudioBundle.message("preview.error.engine", "RangeError: Maximum call stack size exceeded"),
            failed.message,
        )
        assertTrue(failed.message.contains("RangeError"))
    }

    @Test
    fun `the graph count of the source goes with the rendered svg`() = runBlocking {
        pipeline(inspect = { DotSourceInfo(graphCount = 2) }).submit("two graphs")
        renderer.replyTo("two graphs").complete(RenderResult.Svg("<svg/>"))

        assertEquals(PreviewState.Rendering, states.receiveWithin())
        assertEquals(PreviewState.Rendered("<svg/>", graphCount = 2), states.receiveWithin())
    }

    @Test
    fun `a blank source is empty, not an error, and is not rendered`() = runBlocking {
        pipeline(inspect = { DotSourceInfo(graphCount = 0, isBlank = true) }).submit("// nothing yet")

        assertEquals(PreviewState.Empty, states.receiveWithin())
        delay(100.milliseconds)
        assertTrue(renderer.started.tryReceive().isFailure)
        assertTrue(states.tryReceive().isFailure)
    }

    private suspend fun renderOnce(result: RenderResult): PreviewState.Failed {
        pipeline().submit("graph")
        renderer.replyTo("graph").complete(result)
        assertEquals(PreviewState.Rendering, states.receiveWithin())
        return states.receiveWithin() as PreviewState.Failed
    }

    private fun pipeline(
        debounce: Duration = Duration.ZERO,
        timeout: Duration = 5.seconds,
        inspect: suspend (String) -> DotSourceInfo = { DotSourceInfo(graphCount = 1) },
    ): PreviewPipeline = PreviewPipeline(scope, renderer, { states.send(it) }, inspect, debounce, timeout)

    private suspend fun <T> Channel<T>.receiveWithin(): T = withTimeout(5.seconds) { receive() }

    private class FakeRenderer : DotRenderer {
        val started = Channel<String>(Channel.UNLIMITED)
        val cancelled = Channel<String>(Channel.UNLIMITED)
        private val replies = ConcurrentHashMap<String, CompletableDeferred<RenderResult>>()

        fun replyTo(dot: String): CompletableDeferred<RenderResult> = replies.computeIfAbsent(dot) {
            CompletableDeferred()
        }

        override suspend fun render(dot: String): RenderResult {
            started.send(dot)
            try {
                return replyTo(dot).await()
            } catch (e: CancellationException) {
                cancelled.trySend(dot)
                throw e
            }
        }
    }
}
