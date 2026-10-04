plugins {
    alias(libs.plugins.m3mangadex.android.feature)
}

android {
    namespace = "pt.aguiarvieira.m3mangadex.feature.browse"
}

dependencies {
    implementation(projects.core.data)
}
