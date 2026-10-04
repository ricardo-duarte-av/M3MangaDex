plugins {
    alias(libs.plugins.m3mangadex.android.library)
    alias(libs.plugins.m3mangadex.android.compose)
    alias(libs.plugins.m3mangadex.screenshots)
}

android {
    namespace = "pt.aguiarvieira.m3mangadex.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.animation)
    implementation(libs.material.kolor)
    api(libs.coil.compose)
}
