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
    implementation("de.fabmax:physx-jni:2.3.0")

    // native libraries - you can add the one matching your system or all
//    runtimeOnly("de.fabmax:physx-jni:2.3.0:natives-windows")
//    runtimeOnly("de.fabmax:physx-jni:2.3.0:natives-linux")
//    runtimeOnly("de.fabmax:physx-jni:2.3.0:natives-macos")
//    runtimeOnly("de.fabmax:physx-jni:2.3.0:natives-macos-arm64")

    implementation(files("libs/physx-jni-natives-linux-cuda-2.3.0.jar"))
    implementation(files("libs/physx-jni-natives-windows-cuda-2.3.0.jar"))

    val os = org.gradle.internal.os.OperatingSystem.current()
    val lwjglNatives = when {
        os.isLinux -> "natives-linux"
        os.isMacOsX -> "natives-macos"
        else -> "natives-windows"
    }
    implementation("org.lwjgl:lwjgl:3.3.3")
    implementation("org.lwjgl:lwjgl:3.3.3:$lwjglNatives")

    implementation("dev.hollowcube:minestom-ce:30699ec3fd")
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