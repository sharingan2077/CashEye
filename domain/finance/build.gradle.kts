plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // Project modules
    implementation(project(":core:model"))

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
