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
    maven {
        url = uri("https://repo.emortal.dev/snapshots")
    }
}

dependencies {
    implementation("com.github.stephengold:Libbulletjme-Windows64:22.0.1")

    // native libraries:
    runtimeOnly("com.github.stephengold:Libbulletjme-Linux64:22.0.1:SpDebug")

    implementation("io.github.electrostat-lab:snaploader:1.0.0-stable")

    implementation("net.minestom:minestom-snapshots:1_21_4-44b34717ed")

    implementation("ch.qos.logback:logback-classic:1.5.17")
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