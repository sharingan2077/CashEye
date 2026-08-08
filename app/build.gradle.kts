import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Android and Kotlin
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    // Serialization, code generation and dependency injection
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.yandex.school.casheye"
    compileSdk {
        version = release(37)
    }

    installation {
        val installUser =
            providers.gradleProperty("androidInstallUser").orNull

        if (installUser != null) {
            installOptions += listOf("--user", installUser)
        }
    }

    defaultConfig {
        applicationId = "com.yandex.school.casheye"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiKey =
            rootProject
                .file("local/api_key.txt")
                .takeIf { it.isFile }
                ?.readText()
                ?.trim()
                .orEmpty()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            resValue("string", "app_name", "CashEye")
            optimization {
                enable = true
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "CashEye Debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
        compose = true
        resValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    // Project modules
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":data:finance"))
    implementation(project(":data:settings"))
    implementation(project(":domain:settings"))
    implementation(project(":feature:accounts"))
    implementation(project(":feature:analytics"))
    implementation(project(":feature:expenses"))
    implementation(project(":feature:income"))
    implementation(project(":feature:splash"))
    implementation(project(":feature:settings"))

    // Navigation
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.material3.adaptive.navigation3)

    // Compose and AndroidX
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Serialization and coroutines
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.kotlinx.coroutines.android)

    // Security
    implementation(libs.androidx.biometric)

    // Background work
    implementation(libs.androidx.work.runtime.ktx)

    // UI and visualization
    implementation(libs.android.lottie.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // Dependency injection
    implementation(libs.metro.viewmodel.compose)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug-only tools
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
