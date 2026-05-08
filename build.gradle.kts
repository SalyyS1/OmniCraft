plugins {
    kotlin("jvm") version "2.3.0" apply false
    id("com.gradleup.shadow") version "9.0.0-beta12" apply false
}

allprojects {
    group = "com.salyvn"
    version = "0.1.0"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
        maven("https://repo.extendedclip.com/releases/")
        maven("https://jitpack.io")
    }
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }
    }
}
