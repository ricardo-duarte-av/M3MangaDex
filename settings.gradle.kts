pluginManagement {
    includeBuild("build-logic")
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
    }
}

rootProject.name = "M3MangaDex"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":baselineprofile")
include(":core:model")
include(":core:network")
include(":core:datastore")
include(":core:database")
include(":core:auth")
include(":core:data")
include(":core:designsystem")
include(":feature:browse")
include(":feature:search")
include(":feature:login")
include(":feature:library")
include(":feature:downloads")
include(":feature:updates")
include(":feature:manga")
include(":feature:reader")
include(":feature:settings")
