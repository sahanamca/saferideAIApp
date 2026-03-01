allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory = rootProject.layout.buildDirectory.dir("../../build").get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")

    // ensure plugin modules don't fail due to -Werror or obsolete Java options
    tasks.withType(JavaCompile::class.java).configureEach {
        // suppress obsolete option warnings
        options.compilerArgs.add("-Xlint:-options")
        // if a dependency adds -Werror, drop it so warnings do not break the build
        options.compilerArgs.removeAll(listOf("-Werror"))
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
