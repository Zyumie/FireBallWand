plugins {
    java
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "fr.zyumie"
version = "3.7.1-SNAPSHOT"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://jitpack.io") // ItemsAdder
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1")
    compileOnly("me.clip:placeholderapi:2.12.3")
}

tasks {
    runServer {
        minecraftVersion("1.21.11")
    }
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }
    // plugin.yml utilise ${version}
    processResources {
        val props = mapOf("version" to project.version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") { expand(props) }
    }
}
