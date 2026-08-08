import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Android, Kotlin and dependency injection
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.yandex.school.casheye.feature.analytics"
    compileSdk { version = release(37) }
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true }
}

kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_21) } }

dependencies {
    // Project modules
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":domain:finance"))

    // Compose and lifecycle
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Dependency injection
    implementation(libs.metro.runtime)
    implementation(libs.metro.viewmodel.compose)

    // Visualization
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    // Debug-only tools
    debugImplementation(libs.androidx.compose.ui.tooling)
}
