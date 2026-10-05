plugins {
    alias(libs.plugins.m3mangadex.android.library)
    alias(libs.plugins.m3mangadex.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "pt.aguiarvieira.m3mangadex.core.auth"
}

dependencies {
    api(projects.core.network)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.okhttp.mockwebserver)
}
