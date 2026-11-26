rootProject.name = "Nai64Patches"

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/MorpheApp/registry")
            credentials {
                username = providers.gradleProperty("gpr.user").orNull ?: System.getenv("GITHUB_ACTOR")
                password = providers.gradleProperty("gpr.key").orNull ?: System.getenv("GITHUB_TOKEN")
            }
        }
        maven { url = uri("https://jitpack.io") }
    }
}

plugins {
    id("app.morphe.patches") version "2.1.2"
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

configure<app.morphe.patches.gradle.SettingsExtension> {
    patchesProjectPath = ":patches"
    extensions {
        projectsPath = ":extensions"
        proguardFiles(rootProject.projectDir.resolve("extensions/extension/proguard-rules.pro").toString())
    }
}
