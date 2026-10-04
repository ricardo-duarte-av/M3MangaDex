plugins {
    alias(libs.plugins.m3mangadex.android.library)
    alias(libs.plugins.m3mangadex.hilt)
}

android {
    namespace = "pt.aguiarvieira.m3mangadex.core.datastore"
}

dependencies {
    api(projects.core.model)
    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.core)
}
