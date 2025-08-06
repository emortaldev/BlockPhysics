import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-rc1"
}

group = "dev.emortal"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.stephengold:jolt-jni-Windows64:2.1.0")
    runtimeOnly("com.github.stephengold:jolt-jni-Linux64:2.1.0:ReleaseSp")
    runtimeOnly("com.github.stephengold:jolt-jni-Windows64:2.1.0:ReleaseSp")
    implementation("io.github.electrostat-lab:snaploader:1.1.1-stable")
    runtimeOnly("com.github.oshi:oshi-core:6.8.1")

    implementation("org.joml:joml:1.10.8")

    implementation("net.minestom:minestom:2025.07.30-1.21.8")

    implementation("ch.qos.logback:logback-classic:1.5.18")

    implementation("dev.hollowcube:polar:1.14.6")
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