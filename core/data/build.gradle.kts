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
    // api: Hilt's generated component in :app has to see the database types this module provides.
    api(projects.core.database)
    api(projects.core.auth)
    implementation(projects.core.network)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.androidx.paging.testing)
}
