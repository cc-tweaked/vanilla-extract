plugins {
    `java-library`
}

val projectVersion = extra["projectVersion"] as String

group = "cc.tweaked.vanilla-extract"
version = projectVersion

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }

    withSourcesJar()
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
    }
}

sourceSets.all {
    tasks.named(compileJavaTaskName, JavaCompile::class.java) {
        options.compilerArgs.add("-Xlint")
    }
}
