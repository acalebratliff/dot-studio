package io.github.acalebratliff.dotstudio.lang

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import io.github.acalebratliff.dotstudio.DotStudioBundle
import javax.swing.Icon

internal object DotFileType : LanguageFileType(DotLanguage) {
    override fun getName(): String = "DotStudio"

    override fun getDisplayName(): String = DotStudioBundle.message("filetype.display.name")

    override fun getDescription(): String = DotStudioBundle.message("filetype.description")

    override fun getDefaultExtension(): String = "dot"

    override fun getIcon(): Icon = AllIcons.FileTypes.Diagram
}
