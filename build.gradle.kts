// Location: [Your Project Root]/build.gradle.kts

// This file defines the plugins for the whole project,
// referencing the "libs" catalog created in settings.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
}