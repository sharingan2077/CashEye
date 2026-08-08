import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Android, Kotlin and dependency injection
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.yandex.school.casheye.feature.settings"
    compileSdk { version = release(37) }

    defaultConfig { minSdk = 26 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
}

dependencies {
    // Project modules
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":domain:finance"))
    implementation(project(":domain:settings"))

    // Compose and lifecycle
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.ui.test.junit4)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Dependency injection
    implementation(libs.metro.runtime)
    implementation(libs.metro.viewmodel.compose)

    // Animation
    implementation(libs.androidx.compose.animation)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Instrumented tests
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)

    // Debug-only tools
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
