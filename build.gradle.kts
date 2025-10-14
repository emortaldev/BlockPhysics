import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}

group = "dev.emortal"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.stephengold:jolt-jni-Windows64:3.4.0")
    runtimeOnly("com.github.stephengold:jolt-jni-Linux64:3.4.0:ReleaseSp")
    runtimeOnly("com.github.stephengold:jolt-jni-Windows64:3.4.0:ReleaseSp")
    implementation("io.github.electrostat-lab:snaploader:1.1.1-stable")
    runtimeOnly("com.github.oshi:oshi-core:6.9.0")

    implementation("org.joml:joml:1.10.8")

    implementation("net.minestom:minestom:2025.10.11-1.21.10")

    implementation("ch.qos.logback:logback-classic:1.5.18")

    implementation("dev.hollowcube:polar:1.15.0")
    compileOnly("it.unimi.dsi:fastutil:8.5.18")
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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}