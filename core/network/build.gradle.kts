plugins {
    alias(libs.plugins.m3mangadex.android.library)
    alias(libs.plugins.m3mangadex.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "pt.aguiarvieira.m3mangadex.core.network"
}

dependencies {
    api(projects.core.model)
    api(libs.okhttp)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.okhttp.mockwebserver)
}
