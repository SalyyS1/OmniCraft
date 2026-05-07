plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":omnicraft-core"))
    implementation(project(":omnicraft-paper-legacy"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.10")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

tasks {
    jar { enabled = false }
    shadowJar {
        archiveClassifier.set("")
        archiveVersion.set("")
        archiveFileName.set("OmniCraft-26.jar")
    }
    assemble { dependsOn(shadowJar) }
}
