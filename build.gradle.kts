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
        "-XX:SoftMaxHeapSize=256m",
        "-XX:+UseStringDeduplication"
    )
}

tasks.register("packageDist") {
    group = "distribution"
    description = "Creates a self-contained app image with native launcher via jpackage"
    dependsOn("jar")

    doLast {
        val javaHome = javaToolchains.launcherFor {
            languageVersion = JavaLanguageVersion.of(25)
        }.get().metadata.installationPath.asFile

        val buildDir = layout.buildDirectory.get().asFile
        val inputDir = File(buildDir, "package-input")
        val runtimeDir = File(buildDir, "package-runtime")
        val destDir = File(buildDir, "dist")

        // Clean previous output
        inputDir.deleteRecursively()
        runtimeDir.deleteRecursively()
        File(destDir, "Pad9").deleteRecursively()
        inputDir.mkdirs()

        val isWindows = System.getProperty("os.name").lowercase().contains("win")
        val binExt = if (isWindows) ".exe" else ""

        // Step 1: jlink — create minimal JRE
        providers.exec {
            commandLine(
                File(javaHome, "bin/jlink$binExt").absolutePath,
                "--add-modules", "java.base,java.desktop,java.logging",
                "--output", runtimeDir.absolutePath,
                "--strip-debug",
                "--compress", "zip-9",
                "--no-header-files",
                "--no-man-pages"
            )
        }.result.get()

        // Copy app JARs to input dir
        project.copy {
            from(tasks.named("jar"))
            into(inputDir)
        }
        project.copy {
            from(configurations["runtimeClasspath"])
            into(inputDir)
        }

        // Step 2: jpackage — create app image with native .exe launcher
        val jarName = tasks.named<Jar>("jar").get().archiveFileName.get()
        providers.exec {
            commandLine(
                File(javaHome, "bin/jpackage$binExt").absolutePath,
                "--type", "app-image",
                "--name", "Pad9",
                "--app-version", "1.0.0",
                "--input", inputDir.absolutePath,
                "--main-jar", jarName,
                "--main-class", "com.pad9.Pad9",
                "--runtime-image", runtimeDir.absolutePath,
                "--java-options", "-XX:+UseZGC",
                "--java-options", "-XX:SoftMaxHeapSize=256m",
                "--java-options", "-XX:+UseStringDeduplication",
                "--dest", destDir.absolutePath
            )
        }.result.get()

        // Clean temp dirs
        inputDir.deleteRecursively()
        runtimeDir.deleteRecursively()

        val appDir = File(destDir, "Pad9")
        val totalSize = appDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        println("App image: ${appDir.absolutePath}")
        println("Total size: ${totalSize / 1024 / 1024} MB")
    }
}
