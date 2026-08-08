import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    // Android and Kotlin
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.kotlin.serialization) apply false

    // Code generation and dependency injection
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.metro) apply false

    // Code quality
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

// Versions used by the shared quality configuration
val ktlintPluginId =
    libs.plugins.ktlint
        .get()
        .pluginId
val detektPluginId =
    libs.plugins.detekt
        .get()
        .pluginId
val ktlintEngineVersion = libs.versions.ktlintEngine.get()

val ktlintComposeRulesVersion = libs.versions.composeRules.get()

subprojects {
    pluginManager.apply(ktlintPluginId)
    pluginManager.apply(detektPluginId)

    dependencies {
        // Compose-specific ktlint rules
        add("ktlintRuleset", "io.nlopez.compose.rules:ktlint:$ktlintComposeRulesVersion")
    }

    // Shared ktlint configuration
    extensions.configure<KtlintExtension> {
        version.set(ktlintEngineVersion)
        outputToConsole.set(true)
        ignoreFailures.set(false)

        filter {
            exclude("**/generated/**")
        }
    }

    // Android modules require Android-aware ktlint rules.
    listOf("com.android.application", "com.android.library").forEach { androidPluginId ->
        pluginManager.withPlugin(androidPluginId) {
            extensions.configure<KtlintExtension> {
                android.set(true)
            }
        }
    }

    // Shared Detekt configuration
    extensions.configure<DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
    }
}
