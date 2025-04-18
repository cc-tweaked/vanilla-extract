plugins {
    `java-library`
}

val projectVersion: String by extra

group = "cc.tweaked.vanilla-extract"
version = projectVersion

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    withSourcesJar()
}

repositories {
    mavenCentral()

    exclusiveContent {
        forRepository {
            maven("https://maven.fabricmc.net/") {
                name = "Fabric"
            }
        }

        filter {
            includeGroup("net.fabricmc")
            includeGroup("net.fabricmc.unpick")
        }
    }
}

tasks.withType(AbstractArchiveTask::class.java).configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
    filePermissions {}
    dirPermissions {}
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
    }
}
