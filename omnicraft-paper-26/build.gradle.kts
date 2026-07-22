import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":omnicraft-core"))
    implementation(project(":omnicraft-paper-legacy"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.74-stable")
}

kotlin {
    jvmToolchain(25)
    compilerOptions.jvmTarget.set(JvmTarget.JVM_25)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release.set(25)
    }
    jar { enabled = false }
    shadowJar {
        archiveClassifier.set("")
        archiveVersion.set("")
        archiveFileName.set("OmniCraft-26.jar")
        duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
    }
    assemble { dependsOn(shadowJar) }
}
