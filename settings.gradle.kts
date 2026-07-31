// Centralized repository policy is Gradle's recommended configuration.
@file:Suppress("UnstableApiUsage")

// Plugin repositories
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// Shared dependency repositories
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CashEye"

// Application
include(":app")

// Shared modules
include(":core:model")
include(":core:designsystem")
include(":core:common")

// Domain and data layers
include(":domain:finance")
include(":data:finance")
include(":domain:settings")
include(":data:settings")

// Feature modules
include(":feature:expenses")
include(":feature:income")
include(":feature:accounts")
include(":feature:analytics")
include(":feature:splash")
include(":feature:settings")
