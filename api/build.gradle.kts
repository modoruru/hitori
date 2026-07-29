import groovy.util.Node

plugins {
    id("io.papermc.paperweight.userdev")
    id("maven-publish")
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    api("org.json:json:${property("json_version")}")
    api("net.elytrium:serializer:${property("serializer_version")}")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.platform:junit-platform-launcher")
}

tasks {
    test {
        useJUnitPlatform()
    }

    jar {
        archiveBaseName.set("hitori-api")
    }

    val sourcesJar by registering(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }
}

publishing {
    repositories {
        maven {
            name = "modoruReleases"
            url = uri("https://repository.modoru.fun/releases")

            credentials {
                username = System.getenv("REPOSILITE_USERNAME") ?: ""
                password = System.getenv("REPOSILITE_TOKEN") ?: ""
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            groupId = rootProject.group.toString()
            artifactId = rootProject.name
            version = rootProject.version.toString()

            artifact(tasks.named("jar"))
            artifact(tasks.named("sourcesJar"))

            pom {
                withXml {
                    val dependenciesNode = asNode().appendNode("dependencies")
                    addDependency(dependenciesNode, "org.json", "json", property("json_version"))
                    addDependency(dependenciesNode, "net.elytrium", "serializer", property("serializer_version"))
                    addDependency(dependenciesNode, "dev.jorel", "commandapi-paper-core", property("commandapi_version"))
                }
            }
        }
    }
}

fun addDependency(parent: Node, groupId: String, artifactId: String, version: Any?) {
    val dependency = parent.appendNode("dependency")
    dependency.appendNode("groupId", groupId)
    dependency.appendNode("artifactId", artifactId)
    dependency.appendNode("version", version as String)
    dependency.appendNode("scope", "compile")
}