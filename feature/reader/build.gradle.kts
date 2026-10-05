plugins {
    alias(libs.plugins.m3mangadex.android.feature)
}

android {
    namespace = "pt.aguiarvieira.m3mangadex.feature.reader"
}

dependencies {
    implementation(projects.core.data)
    implementation(libs.telephoto.zoomable.image.coil3)
    implementation(libs.coil.compose)
    implementation(libs.androidx.browser)
}
