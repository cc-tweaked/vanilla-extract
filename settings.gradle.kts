rootProject.name = "vanilla-extract"

dependencyResolutionManagement {
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
}

include(":plugin")
include(":core")
include(":decompile")

for (project in rootProject.children) {
    project.projectDir = file("projects/${project.name}")
}
