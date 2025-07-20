import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-rc1"
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
    runtimeOnly("com.github.stephengold:Libbulletjme-Linux64:22.0.1:SpRelease")
    runtimeOnly("com.github.stephengold:Libbulletjme-Windows64:22.0.1:SpRelease")

    implementation("io.github.electrostat-lab:snaploader:1.1.1-stable")

    implementation("net.minestom:minestom:2025.07.17-1.21.8")

    implementation("ch.qos.logback:logback-classic:1.5.18")
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