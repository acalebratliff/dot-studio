package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import io.github.acalebratliff.dotstudio.DotStudioBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.annotations.Nls
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal sealed interface PreviewState {
    data object Rendering : PreviewState

    data class Rendered(val svg: String) : PreviewState

    data class Failed(val message: @Nls String) : PreviewState
}

/**
 * Renders the latest submitted DOT source after [debounce] of quiet. A newer submission cancels the render in
 * progress, so a superseded render never reaches [onState]. Disposing stops everything.
 */
internal class PreviewPipeline(
    scope: CoroutineScope,
    private val renderer: DotRenderer,
    private val onState: suspend (PreviewState) -> Unit,
    private val debounce: Duration = 300.milliseconds,
    private val timeout: Duration = 10.seconds,
) : Disposable {
    private val source = MutableStateFlow<CharSequence?>(null)

    private val job = scope.launch {
        source.filterNotNull().collectLatest { snapshot ->
            // collectLatest cancels this delay when newer source arrives, which is the debounce.
            delay(debounce)
            onState(PreviewState.Rendering)
            onState(render(snapshot.toString()))
        }
    }

    /** [dot] must be an immutable snapshot; it is turned into a String only after the debounce, off the caller's thread. */
    fun submit(dot: CharSequence) {
        source.value = dot
    }

    override fun dispose() {
        job.cancel()
    }

    private suspend fun render(dot: String): PreviewState {
        val result = withTimeoutOrNull(timeout) { renderer.render(dot) }
        LOG.debug { "Render finished: ${result?.javaClass?.simpleName ?: "timed out after $timeout"}" }
        return when (result) {
            null -> PreviewState.Failed(DotStudioBundle.message("preview.error.timeout", timeout.inWholeSeconds))

            is RenderResult.Svg -> PreviewState.Rendered(result.svg)

            is RenderResult.DotErrors -> PreviewState.Failed(dotErrorMessage(result.messages))

            is RenderResult.EngineFailure -> PreviewState.Failed(
                DotStudioBundle.message("preview.error.engine", result.detail),
            )
        }
    }

    private fun dotErrorMessage(messages: List<String>): @Nls String = if (messages.isEmpty()) {
        DotStudioBundle.message("preview.error.dot.unspecified")
    } else {
        DotStudioBundle.message("preview.error.dot", messages.joinToString("\n"))
    }

    private companion object {
        val LOG = logger<PreviewPipeline>()
    }
}
