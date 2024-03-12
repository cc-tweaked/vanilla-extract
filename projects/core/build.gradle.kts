plugins {
    id("vanilla-extract.java-convention")
}

dependencies {
    compileOnly(libs.jetbrainsAnnotations)
    compileOnly(libs.checkerFramework)

    implementation(libs.gson)
    implementation(libs.fabric.tinyRemapper)
    implementation(libs.fabric.accessWidener)
    implementation(libs.fabric.mappingIo)
    implementation(libs.fabric.unpickFormatUtils)
    implementation(libs.slf4j)

    testCompileOnly(libs.jetbrainsAnnotations)
    testImplementation(libs.guava)
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.testRuntime)
    testRuntimeOnly(libs.slf4j.simple)
}
