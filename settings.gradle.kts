import java.net.URI

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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = URI("https://jitpack.io") }
        // For SNAPSHOT access
        // maven { url = URI("https://central.sonatype.com/repository/maven-snapshots/") }
    }
}

rootProject.name = "LiveKit Voice Assistant Example"
include(":app")

// For local development with the LiveKit Android SDK only.
// includeBuild("../components-android")