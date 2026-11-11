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

    maven(url = "https://central.sonatype.com/repository/maven-snapshots/") {
        content { // This filtering is optional, but recommended
            includeModule("net.minestom", "minestom")
            includeModule("net.minestom", "testing")
        }
    }
}

dependencies {
    implementation("com.github.stephengold:jolt-jni-Windows64:6.1.1")
    runtimeOnly("com.github.stephengold:jolt-jni-Linux64:6.1.1:ReleaseSp")
    runtimeOnly("com.github.stephengold:jolt-jni-Windows64:6.1.1:ReleaseSp")
    implementation("io.github.electrostat-lab:snaploader:1.1.1-stable")
    runtimeOnly("com.github.oshi:oshi-core:7.7.0")

    implementation("org.joml:joml:1.10.8")

    implementation("net.minestom:minestom:26_3-SNAPSHOT")

    implementation("ch.qos.logback:logback-classic:1.6.5")

    implementation("dev.hollowcube:polar:1.16.0")

    implementation("io.github.toxicity188:dynamicuv:1.2.2") // player models
    compileOnly("it.unimi.dsi:fastutil:8.5.19")
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