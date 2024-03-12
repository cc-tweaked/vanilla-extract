plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("vanilla-extract.java-convention")
    alias(libs.plugins.shadow)
}

dependencies {
    // Nasty hack to ensure the Gradle API isn't shadowed.
    configurations["api"].dependencies.remove(gradleApi())
    compileOnly(gradleApi())

    implementation(project(":core"))
    implementation(project(":decompile"))
    implementation(libs.fabric.mappingIo)
    shadow(libs.bundles.unshadowedDeps)

    testCompileOnly(libs.jetbrainsAnnotations)
    testImplementation(libs.guava)
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.testRuntime)
}

gradlePlugin {
    plugins {
        create("vanilla-extract") {
            id = "cc.tweaked.vanilla-extract"
            implementationClass = "cc.tweaked.vanillaextract.VanillaPlugin"
        }
    }
}

tasks.jar {
    archiveClassifier = "slim"
}

tasks.sourcesJar {
    from(project(":core").sourceSets.main.get().allSource)
    from(project(":decompile").sourceSets.main.get().allSource)
}

tasks.shadowJar {
    archiveClassifier = ""
    dependencies {
        include(project(":core"))
        include(project(":decompile"))
        include(dependency(libs.fabric.accessWidener.get()))
        include(dependency(libs.fabric.mappingIo.get()))
        include(dependency(libs.fabric.tinyRemapper.get()))
        include(dependency(libs.fabric.unpickFormatUtils.get()))
    }

    // We need to be careful with our remapping, as this will also remap constant strings!
    // We over-specify, to avoid remapping the constant "net.fabricmc" string.
    relocate("net.fabricmc.accesswidener", "cc.tweaked.vanillaextract.vendor.accesswidener")
    relocate("net.fabricmc.tinyremapper", "cc.tweaked.vanillaextract.vendor.tinyremapper")
    relocate("net.fabricmc.mappingio", "cc.tweaked.vanillaextract.vendor.mappingio")
    relocate("daomephsta.unpick.constantmappers.datadriven", "cc.tweaked.vanillaextract.vendor.unpick")

    minimize()
}

tasks.pluginUnderTestMetadata {
    pluginClasspath.from(project.sourceSets.main.get().runtimeClasspath)
    pluginClasspath.from(project(":core").sourceSets.main.get().runtimeClasspath)
}

// Apply the same trick as the Gradle plugin publishing tool to avoid exporting our "actual" dependencies.
components.named<AdhocComponentWithVariants>("java") {
    addVariantsFromConfiguration(configurations.getByName("apiElements")) { skip() }
    addVariantsFromConfiguration(configurations.getByName("runtimeElements")) { skip() }
    addVariantsFromConfiguration(configurations.getByName("shadowRuntimeElements")) { mapToMavenScope("runtime") }
}

publishing {
    repositories {
        maven("https://squiddev.cc/maven") {
            name = "SquidDev"

            credentials(PasswordCredentials::class)
        }
    }
}

tasks.test {
    useJUnitPlatform {
        if (properties.containsKey("fast-tests")) excludeTags("slow")
    }
}
