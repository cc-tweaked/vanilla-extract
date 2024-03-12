rootProject.name = "vanilla-extract"

include(":plugin")
include(":core")
include(":decompile")

for (project in rootProject.children) {
    project.projectDir = file("projects/${project.name}")
}
