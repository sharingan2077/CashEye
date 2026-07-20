import dev.detekt.gradle.extensions.DetektExtension
import org.jlleitschuh.gradle.ktlint.KtlintExtension

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false

    alias(libs.plugins.ksp) apply false

    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false

    alias(libs.plugins.metro) apply false
    alias(libs.plugins.android.library) apply false
}

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
        add("ktlintRuleset", "io.nlopez.compose.rules:ktlint:$ktlintComposeRulesVersion")
    }

    extensions.configure<KtlintExtension> {
        version.set(ktlintEngineVersion)
        outputToConsole.set(true)
        ignoreFailures.set(false)

        filter {
            exclude("**/generated/**")
        }
    }

    listOf("com.android.application", "com.android.library").forEach { androidPluginId ->
        pluginManager.withPlugin(androidPluginId) {
            extensions.configure<KtlintExtension> {
                android.set(true)
            }
        }
    }

    extensions.configure<DetektExtension> {
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
        buildUponDefaultConfig = true
    }
}
