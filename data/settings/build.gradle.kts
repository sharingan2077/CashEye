import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.metro)
}

android {
    namespace = "com.yandex.school.casheye.data.settings"
    compileSdk { version = release(37) }

    defaultConfig { minSdk = 26 }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
}

dependencies {
    implementation(project(":domain:settings"))

    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.metro.runtime)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
