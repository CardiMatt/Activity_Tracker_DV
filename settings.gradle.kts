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

    plugins {
        id("com.google.gms.google-services") version "4.4.2" apply false
        id("com.google.dagger.hilt.android") version "2.48.1" apply false
        id("com.google.devtools.ksp") version "1.9.0-1.0.13"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Activity_Tracker_DV"
include(":app")
