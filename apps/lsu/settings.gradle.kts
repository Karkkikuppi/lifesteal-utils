pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://maven.kikugie.dev/snapshots"); name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
}

stonecutter {
    create(rootProject) {
        // See https://stonecutter.kikugie.dev/wiki/start/#choosing-minecraft-versions
        versions("1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1")
        vcsVersion = "1.21.11"
    }
}

rootProject.name = "Lifesteal Utils"