plugins {
    alias(libs.plugins.m3mangadex.android.feature)
}

android {
    namespace = "pt.aguiarvieira.m3mangadex.feature.library"
}

dependencies {
    implementation(projects.core.data)
    implementation(libs.androidx.paging.compose)
}
