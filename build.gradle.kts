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
    implementation("com.github.stephengold:Libbulletjme:20.2.0")
//    implementation("com.badlogicgames.gdx:gdx-bullet:1.12.0")
//    implementation("com.badlogicgames.gdx:gdx-bullet-platform:1.12.0:natives-desktop")

    implementation("net.minestom:minestom-snapshots:e8e22a2b15")

    implementation("dev.emortal:rayfast:928feb1")
    implementation("ch.qos.logback:logback-classic:1.5.1")
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
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