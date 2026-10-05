import java.util.Properties

plugins {
    alias(libs.plugins.m3mangadex.android.application)
    alias(libs.plugins.m3mangadex.android.compose)
    alias(libs.plugins.m3mangadex.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing: keystore.properties (local, gitignored) first, then environment variables (CI).
// With neither, the release build is produced unsigned instead of failing.
val keystoreProperties =
    Properties().apply {
        val file = rootProject.file("keystore.properties")
        if (file.exists()) file.inputStream().use(::load)
    }

fun signingValue(
    propKey: String,
    envKey: String,
): String? = keystoreProperties.getProperty(propKey) ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("storeFile", "KEYSTORE_FILE")
val releaseStorePassword = signingValue("storePassword", "KEYSTORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "KEY_PASSWORD")
val hasReleaseSigning =
    listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword).all { it != null }

// One number to bump: versionName is the milestone version (0.<milestone>.<build>; 1.0.0 once
// everything is done), and Play's versionCode is derived from it so it always grows with it.
val appVersionName = providers.gradleProperty("m3mangadex.versionName").get()
val appVersionCode =
    appVersionName.split(".").map(String::toInt).let { (major, minor, patch) ->
        require(minor < 1000 && patch < 1000) { "versionName $appVersionName: minor and patch must be < 1000" }
        major * 1_000_000 + minor * 1_000 + patch
    }

android {
    namespace = "pt.aguiarvieira.m3mangadex"

    defaultConfig {
        applicationId = "pt.aguiarvieira.m3mangadex"
        // CI checks a `vX.Y.Z` tag against versionName (gradle.properties).
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Installs beside the Play build.
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.network)
    implementation(projects.core.data)
    implementation(projects.feature.browse)
    implementation(projects.feature.library)
    implementation(projects.feature.downloads)
    implementation(projects.feature.login)
    implementation(projects.feature.updates)
    implementation(projects.feature.search)
    implementation(projects.feature.manga)
    implementation(projects.feature.reader)
    implementation(projects.feature.settings)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.androidx.compose.material3.navigation.suite)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.work.runtime.ktx)
}
