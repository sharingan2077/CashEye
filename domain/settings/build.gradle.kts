plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
}
