import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.emortal"
version = "1.0.0"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

dependencies {
    implementation("com.badlogicgames.gdx:gdx-bullet:1.12.0")
    implementation("com.badlogicgames.gdx:gdx-bullet-platform:1.12.0:natives-desktop")

    implementation("net.minestom:minestom-snapshots:e8e22a2b15")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("BlockPhysics")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "dev.emortal.Main"))
        }
    }
}

tasks {
    build {
        dependsOn(shadowJar)
    }
}