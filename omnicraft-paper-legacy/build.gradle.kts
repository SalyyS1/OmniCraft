plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

dependencies {
    implementation(project(":omnicraft-core"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.1.10")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("net.Indyuce:MMOItems-API:6.10.1-SNAPSHOT")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveVersion.set("")
        archiveFileName.set("OmniCraft-legacy.jar")
    }
    assemble { dependsOn(shadowJar) }
}
