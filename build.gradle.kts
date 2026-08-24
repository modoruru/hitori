plugins {
    java
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17" apply false
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

val defaultJavaVersion = 23

allprojects {

    repositories {
        mavenCentral()

        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "papermc"
        }

        maven("https://jitpack.io") {
            name = "jitpack"
        }
    }

    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of((property("java") ?: defaultJavaVersion) as String))
            }
        }
    }
}

subprojects {
    if (name == "api") apply(plugin = "java-library")
    else apply(plugin = "java")

    dependencies {
        compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    }
}