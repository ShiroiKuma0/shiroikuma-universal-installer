import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlinx.serialization)
    id("kotlin-parcelize")
}

// === Fork versioning (shiroikuma fork) — values live in gradle.properties ========================
// forkVersionName = "<VERSION_NAME>+<BUILD_NUMBER>"; forkVersionCode = VERSION_CODE * 10000 + BUILD_NUMBER.
val forkVersionName = "${project.property("VERSION_NAME")}+${project.property("BUILD_NUMBER")}"
val forkVersionCode = project.property("VERSION_CODE").toString().toInt() * 10000 +
        project.property("BUILD_NUMBER").toString().toInt()

base {
    archivesName = "shiroikuma-universal-installer_${forkVersionName}"
}

android {
    namespace = "app.pwhs.universalinstaller"
    compileSdk = 36

    defaultConfig {
        // Fork identity: distinct applicationId so we coexist with the upstream/Play app.
        // The code namespace (above) stays on app.pwhs.universalinstaller, so R/BuildConfig,
        // sources, AIDL and FileProvider class names are unchanged from upstream.
        applicationId = project.property("APP_ID").toString()
        minSdk = 24
        targetSdk = 36
        versionCode = forkVersionCode
        versionName = forkVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Load signing config from key.properties (CI/CD)
    val keyPropertiesFile = rootProject.file("key.properties")
    val useReleaseKeystore = keyPropertiesFile.exists()

    if (useReleaseKeystore) {
        val keyProperties = Properties().apply {
            load(keyPropertiesFile.inputStream())
        }
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(keyProperties["storeFile"] as String)
                storePassword = keyProperties["storePassword"] as String
                keyAlias = keyProperties["keyAlias"] as String
                keyPassword = keyProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (useReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    // Single distribution: ships libsu for real Root install support alongside Shizuku
    // and the default system installer. The previous store/full split was removed —
    // apps in this category on Play routinely ship Root/Shizuku/Default together, so
    // the static-analysis concern that drove the split didn't pan out.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
            freeCompilerArgs = listOf("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
        aidl = true
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

// Build the release APK, copy it to ~/tmp under our canonical name, and bump BUILD_NUMBER
// for the next build. Mirrors the buildFoss/fork-build pattern used across the shiroikuma forks.
// (Upstream v1.8.3 dropped the store/full flavor split — single release artifact now.)
tasks.register("buildFork") {
    description = "Build the release APK, copy it to ~/tmp, and bump BUILD_NUMBER for next time."
    dependsOn("assembleRelease")
    doLast {
        val apkName = "shiroikuma-universal-installer_${forkVersionName}.apk"
        val outputDir = layout.buildDirectory.dir("outputs/apk/release").get().asFile
        val targetDir = File(System.getProperty("user.home"), "tmp")
        targetDir.mkdirs()
        outputDir.listFiles { _, name -> name.endsWith(".apk") }?.firstOrNull()?.let { apk ->
            val targetFile = File(targetDir, apkName)
            apk.copyTo(targetFile, overwrite = true)
            println("\u001b[1;36m>>> ${targetFile.absolutePath}\u001b[0m")
            println("\u001b[1;36m>>> versionCode $forkVersionCode\u001b[0m")
        } ?: throw GradleException("No APK found in $outputDir")

        // Auto-increment BUILD_NUMBER for the next build.
        val propsFile = rootProject.file("gradle.properties")
        val currentBuildNumber = project.property("BUILD_NUMBER").toString().toInt()
        val nextBuildNumber = currentBuildNumber + 1
        propsFile.writeText(
            propsFile.readText().replace(
                "BUILD_NUMBER=$currentBuildNumber",
                "BUILD_NUMBER=$nextBuildNumber"
            )
        )
        println("\u001b[1;36m>>> BUILD_NUMBER bumped to $nextBuildNumber\u001b[0m")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.coil.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.bundles.ackpine)
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)


    implementation(libs.bottom.sheet)

    implementation(libs.timber)
    compileOnly(libs.rikka.stub)
    implementation(libs.hiddenapibypass)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.logging)

    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    implementation(libs.bundles.ackpine.libsu)
    implementation(libs.libsu.core)
    implementation(libs.libsu.service)

    implementation(libs.nanohttpd)
    implementation(libs.zxing.core)
    // Open-source QR scanner (Apache-2.0) for "Send to TV" — avoids proprietary ML Kit.
    implementation(libs.zxing.android.embedded)
}