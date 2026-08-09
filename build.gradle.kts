// Top-level build file where you can add configuration options common to all sub-projects/modules.

// AGP 8.11.2 ships with an older R8 that doesn't recognize Kotlin 2.3 metadata. Override
// to R8 8.13.19 (minimum version supporting Kotlin 2.3 per
// https://developer.android.com/studio/build/kotlin-d8-r8-versions). Drop this block when we
// upgrade AGP to a version whose bundled R8 already supports the Kotlin in use.
buildscript {
    dependencies {
        classpath("com.android.tools:r8:8.13.19")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.ksp) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.android.library) apply false
    // Only :app applies these, and only when a google-services.json is present — see
    // app/build.gradle.kts. Declared here so their classes are on the buildscript classpath
    // either way, which is what lets :app import the extension types unconditionally.
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}