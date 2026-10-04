import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "pt.aguiarvieira.m3mangadex.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    // Applied by id only (no API references): kept off the compile classpath so their Kotlin
    // metadata can't skew against Gradle's embedded compiler.
    runtimeOnly(libs.compose.gradlePlugin)
    runtimeOnly(libs.ksp.gradlePlugin)
    runtimeOnly(libs.roborazzi.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "m3mangadex.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "m3mangadex.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "m3mangadex.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "m3mangadex.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("jvmLibrary") {
            id = "m3mangadex.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("hilt") {
            id = "m3mangadex.hilt"
            implementationClass = "HiltConventionPlugin"
        }
        register("screenshots") {
            id = "m3mangadex.screenshots"
            implementationClass = "ScreenshotConventionPlugin"
        }
    }
}
