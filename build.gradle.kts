import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
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
