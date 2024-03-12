plugins {
    id("vanilla-extract.java-convention")
}

dependencies {
    compileOnly(libs.jetbrainsAnnotations)
    compileOnly(libs.checkerFramework)

    implementation(libs.asm)
    implementation(libs.vineflower)
    implementation(libs.slf4j)
    implementation(libs.fabric.mappingIo)

    testCompileOnly(libs.jetbrainsAnnotations)
    testImplementation(libs.asm)
    testImplementation(libs.asm.tree)
    testImplementation(libs.bundles.test)
    testRuntimeOnly(libs.bundles.testRuntime)
    testRuntimeOnly(libs.slf4j.simple)
}
