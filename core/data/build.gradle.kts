plugins {
    alias(libs.plugins.m3mangadex.android.library)
    alias(libs.plugins.m3mangadex.hilt)
}

android {
    namespace = "pt.aguiarvieira.m3mangadex.core.data"
}

dependencies {
    api(projects.core.model)
    api(projects.core.datastore)
    api(libs.androidx.paging.common)
    implementation(projects.core.network)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.androidx.paging.testing)
}
