plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "OmniCraft"

include("omnicraft-core")
include("omnicraft-paper-legacy")
include("omnicraft-paper-26")
