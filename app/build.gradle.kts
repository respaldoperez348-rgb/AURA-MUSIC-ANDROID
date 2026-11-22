plugins {
    id("com.android.application")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.auramusic.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.auramusic.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "3.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":innertube"))

    // Media3
    implementation(libs.media3)
    implementation(libs.media3.okhttp)
    implementation(libs.media3.session)

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.activity)
    implementation(libs.viewmodel.compose)

    // Images
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.palette)

    // Preferences & Utilities
    implementation(libs.datastore)
    implementation(libs.timber)
    coreLibraryDesugaring(libs.desugaring)

    testImplementation(libs.junit)
}
