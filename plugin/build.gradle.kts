plugins {
    id("io.papermc.paperweight.userdev")
    id("com.gradleup.shadow")
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    implementation(project(":api"))
}

tasks {

    shadowJar {
        archiveBaseName.set("hitori")

        from(project(":api").sourceSets.main.get().output)
    }

    jar {
        archiveBaseName.set("hitori")
        enabled = false
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        filesMatching("paper-plugin.yml") {
            expand(mapOf("version" to project.version))
        }
    }
}