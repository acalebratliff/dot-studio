package io.github.acalebratliff.dotstudio.lang

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase

internal class DotFileTypeTest : BasePlatformTestCase() {
    fun testDotAndGvExtensions() {
        val fileTypes = FileTypeManager.getInstance()
        assertEquals(DotFileType, fileTypes.getFileTypeByFileName("graph.dot"))
        assertEquals(DotFileType, fileTypes.getFileTypeByFileName("graph.gv"))
    }

    fun testFileIsParsedAsDot() {
        val file = myFixture.configureByText("graph.gv", "digraph { a -> b }")
        assertInstanceOf(file, DotFile::class.java)
    }
}
