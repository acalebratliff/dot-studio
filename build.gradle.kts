import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import java.security.MessageDigest
import java.util.HexFormat

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.intellij.platform.grammarkit")
    id("org.jetbrains.changelog")
    id("org.jmailen.kotlinter")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        // 2025.2 (sinceBuild 252) bundles Kotlin stdlib 2.1, so the API and language level must stay at 2.1.
        apiVersion = KotlinVersion.KOTLIN_2_1
        languageVersion = KotlinVersion.KOTLIN_2_1
        allWarningsAsErrors = true
    }
}

sourceSets {
    main {
        java.srcDir(tasks.generateLexer.map { it.targetRootOutputDir.get() })
        java.srcDir(tasks.generateParser.map { it.targetRootOutputDir.get() })
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2025.2.6.3")
        testFramework(TestFrameworkType.Platform)
    }

    // The platform test framework runs JUnit 3/4-style BasePlatformTestCase tests.
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252"
            untilBuild = "262.*"
        }
    }

    pluginVerification {
        ides {
            select {
                types = listOf(IntelliJPlatformType.IntellijIdea)
                sinceBuild = "252"
                untilBuild = "262.*"
            }
            recommended()
        }
        failureLevel = VerifyPluginTask.FailureLevel.ALL
    }
}

tasks.verifyPlugin {
    // The Plugin Verifier keeps extracted-plugins/ in a home dir that defaults to ~/.pluginVerifier, shared by every
    // checkout on the machine, so concurrent runs from two worktrees corrupt each other. Give each checkout its own.
    val verifierHome = layout.buildDirectory.dir("pluginVerifierHome").get().asFile
    systemProperty("plugin.verifier.home.dir", verifierHome.absolutePath)
}

tasks.test {
    // Tests read their fixtures from src/test/testData by relative path, so Gradle must treat them as inputs.
    inputs.dir("src/test/testData").withPathSensitivity(PathSensitivity.RELATIVE).withPropertyName("testData")
}

tasks.generateLexer {
    sourceFile = layout.projectDirectory.file("src/main/grammars/Dot.flex")
}

tasks.generateParser {
    sourceFile = layout.projectDirectory.file("src/main/grammars/Dot.bnf")
    pathToParser = "io/github/acalebratliff/dotstudio/lang/parser/DotParser.java"
    pathToPsiRoot = "io/github/acalebratliff/dotstudio/lang/psi"
}

tasks.register<VerifyVendoredChecksums>("verifyVizJsChecksums") {
    group = "verification"
    description = "Checks the vendored viz-js files against the SHA-256s in third_party/viz-js/README.md."
    vendoredDir = layout.projectDirectory.dir("third_party/viz-js")
    report = layout.buildDirectory.file("vendored-checksums/viz-js.txt")
}

tasks.processResources {
    // A tampered renderer must never reach the plugin jar, so packaging waits for the checksum check.
    dependsOn("verifyVizJsChecksums")
    from(layout.projectDirectory.dir("third_party/viz-js/dist")) {
        into("preview/viz-js")
    }
}

tasks.test {
    // Tests must never start a real JCEF browser (CODING-STANDARDS 1.7); with JCEF off the preview shows its notice.
    systemProperty("ide.browser.jcef.enabled", "false")
}

tasks.check {
    dependsOn("verifyVizJsChecksums")
}

/**
 * Compares every file under [vendoredDir] (except its README.md) with the SHA-256 recorded for it in a README.md
 * table row of the form `` | `relative/path` | `sha256` | ... | ``.
 */
@CacheableTask
abstract class VerifyVendoredChecksums : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val vendoredDir: DirectoryProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val root = vendoredDir.get().asFile
        val row = Regex("""^\|\s*`([^`]+)`\s*\|\s*`([0-9a-f]{64})`\s*\|""")
        val expected = root.resolve(README).readLines()
            .mapNotNull { row.find(it) }
            .associate { it.groupValues[1] to it.groupValues[2] }
        if (expected.isEmpty()) throw GradleException("No checksum rows found in ${root.resolve(README)}")

        val actual = root.walkTopDown()
            .filter { it.isFile && it.name != README }
            .associate { it.relativeTo(root).invariantSeparatorsPath to sha256(it) }

        val problems = buildList {
            (actual.keys - expected.keys).sorted().forEach { add("$it: not listed in $README") }
            (expected.keys - actual.keys).sorted().forEach { add("$it: listed in $README but missing") }
            expected.keys.intersect(actual.keys).sorted()
                .filter { expected[it] != actual[it] }
                .forEach { add("$it: expected ${expected[it]}, got ${actual[it]}") }
        }
        if (problems.isNotEmpty()) {
            throw GradleException("Vendored checksum mismatch in $root:\n" + problems.joinToString("\n"))
        }
        report.get().asFile.writeText(actual.toSortedMap().entries.joinToString("") { "${it.value}  ${it.key}\n" })
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return HexFormat.of().formatHex(digest.digest())
    }

    private companion object {
        const val README = "README.md"
    }
}
