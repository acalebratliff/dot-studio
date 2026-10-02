package io.github.acalebratliff.dotstudio.lang

import com.intellij.lang.Language
import io.github.acalebratliff.dotstudio.DotStudioBundle

// The ID is plugin-specific because a second plugin registering a language with the same ID fails at load time.
internal object DotLanguage : Language("DotStudio") {
    override fun getDisplayName(): String = DotStudioBundle.message("language.display.name")
}
