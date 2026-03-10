plugins {
    java
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "com.pad9.Pad9"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.formdev:flatlaf:3.4")
    implementation("com.fifesoft:rsyntaxtextarea:3.4.1")
    implementation("com.google.code.gson:gson:2.11.0")
}

tasks.named<JavaExec>("run") {
    jvmArgs = listOf(
        "-XX:+UseZGC",
        "-XX:+ZGenerational",
        "-XX:SoftMaxHeapSize=256m",
        "-XX:+UseStringDeduplication"
    )
}
