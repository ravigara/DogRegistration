// Location: [Your Project Root]/settings.gradle.kts

// 1. Defines where Gradle should look for plugins
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// 2. Defines where Gradle should look for dependencies AND
//    This block creates the "libs" object from your TOML file
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

// 3. Defines the modules in your project
rootProject.name = "DogRegistration"
include(":app")