plugins {
    alias(libs.plugins.m3mangadex.android.feature)
}

android {
    namespace = "pt.aguiarvieira.m3mangadex.feature.settings"
}

dependencies {
    implementation(projects.core.data)
    implementation(libs.androidx.activity.compose)
}
