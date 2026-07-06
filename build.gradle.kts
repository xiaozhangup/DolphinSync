import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("io.izzel.taboolib") version "2.0.38"
    id("me.xiaozhangup.sftp-uploader") version "0.1.0"
    kotlin("jvm") version "2.3.20"
}

taboolib {
    env {
        // 安装模块
        install(
            Basic,
            Bukkit,
            BukkitHook,
            BukkitUtil,
            MinecraftChat,
            CommandHelper,
            AlkaidRedis,
            Database
        )
    }
    version {
        taboolib = "6.3.0-test-6-23-1"
        coroutines = "1.11.0"
        skipKotlinRelocate = true
        skipKotlin = true
    }

    description {
        dependencies {
            name("CarbKotlin")
        }
    }

    relocate("plutoproject.adventurekt", "me.xiaozhangup.dolphin.lib.adventurekt")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.nostal.ink/repository/maven-public/")
}

dependencies {
    compileOnly("me.xiaozhangup.octopus:octopus-api:26.2-R0.1-SNAPSHOT")
    compileOnly("redis.clients:jedis:5.1.0")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))

    taboo("plutoproject.adventurekt:core:v3.0.0-paper") {
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
            layout.buildDirectory.file("libs/DolphinSync-1.0.6.jar").get().asFile.absolutePath
        )
    )
}
