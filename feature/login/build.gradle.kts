plugins {
    alias(libs.plugins.m3mangadex.android.feature)
}

android {
    namespace = "pt.aguiarvieira.m3mangadex.feature.login"
}

dependencies {
    implementation(projects.core.data)
}
