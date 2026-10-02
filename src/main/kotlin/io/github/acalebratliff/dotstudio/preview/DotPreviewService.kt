package io.github.acalebratliff.dotstudio.preview

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope

/** Supplies the project's coroutine scope to preview panels, so renders stop when the project closes. */
@Service(Service.Level.PROJECT)
internal class DotPreviewService(private val scope: CoroutineScope) {
    fun createPanel(parent: Disposable): DotPreviewPanel = DotPreviewPanel(parent, scope)
}
