import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("com.gradleup.shadow") version "9.4.3"
    id("me.xiaozhangup.sftp-uploader") version "0.1.0"
    kotlin("jvm") version "2.3.20"
}



repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.nostal.ink/repository/maven-public/")
}

dependencies {
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    compileOnly("me.xiaozhangup.crab:CarbKotlin:2.3.20:paper") {
        isTransitive = false
    }
    compileOnly("me.xiaozhangup.octopus:octopus-api:26.2-R0.1-SNAPSHOT")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))

    compileOnly("plutoproject.adventurekt:core:v3.0.0") {
        isTransitive = false
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

sftpUploader {
    host.set("xiaozhangup@s1.dimc.cloud")
    target.set("Minecraft")
    jars.set(
        listOf(
            layout.buildDirectory.file("libs/DolphinSync-1.0.7.jar").get().asFile.absolutePath
        )
    )
}

// Native runtime and thin compile-time API.
tasks.jar { archiveClassifier.set("plain") }
tasks.shadowJar {
    archiveClassifier.set("")
    filesMatching("META-INF/services/**") { duplicatesStrategy = DuplicatesStrategy.INCLUDE }
    mergeServiceFiles()
    exclude("META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA")
    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*:.*"))
        exclude(dependency("org.jetbrains.kotlinx:.*:.*"))
    }

}
val apiJar = tasks.register<Jar>("apiJar") {
    archiveClassifier.set("api")
    from(sourceSets.main.get().output) { exclude("plugin.yml") }
}
tasks.assemble { dependsOn(tasks.shadowJar, apiJar) }
tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") { expand("version" to project.version) }
}
