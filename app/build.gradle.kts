plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.dogregistration"
    compileSdk = 34

    // --- FIX 1: ALIGN JAVA AND KOTLIN COMPILERS ---
    // This resolves the 'Inconsistent JVM-target' error by forcing Javac to use version 17.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        applicationId = "com.example.dogregistration"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    // Kotlin JVM target is set to 17 (required for the Android Gradle Plugin 8.x)
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // --- Compose and Core (Aligned to Kotlin 1.9.24 stability) ---
    val composeBom = platform("androidx.compose:compose-bom:2024.04.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")

    // --- Lifecycle and Coroutines ---
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // --- CameraX and External Libraries ---
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("com.google.android.material:material:1.12.0")

    // --- ML Kit and TFLite ---
    implementation("com.google.mlkit:object-detection:17.0.1")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.15.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")

    // --- Room Database and Serialization ---
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")
    implementation("com.google.code.gson:gson:2.10.1")
}
