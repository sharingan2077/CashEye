import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    alias(libs.plugins.jetbrains.kotlin.serialization)

    alias(libs.plugins.ksp)

    alias(libs.plugins.metro)
}

android {
    namespace = "com.yandex.school.casheye"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.yandex.school.casheye"
        minSdk = 26
        targetSdk = 36
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
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":data:finance"))
    implementation(project(":data:settings"))
    implementation(project(":domain:settings"))
    implementation(project(":feature:accounts"))
    implementation(project(":feature:analytics"))
    implementation(project(":feature:expenses"))
    implementation(project(":feature:income"))
    implementation(project(":feature:splash"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.material3.adaptive.navigation3)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.kotlinx.serialization.core)

    implementation(libs.android.lottie.compose)

    implementation(libs.androidx.core.splashscreen)

    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.metro.viewmodel.compose)
}
