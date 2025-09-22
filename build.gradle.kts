import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("io.izzel.taboolib") version "2.0.23"
    kotlin("jvm") version "2.1.21"
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
            Database
        )
    }
    version {
        taboolib = "6.2.3-test-17"
        coroutines = "1.10.2"
        skipKotlinRelocate = true
        skipKotlin = true
    }

    description {
        dependencies {
            name("CarbKotlin")
            name("SlimeCargoNext")
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
    compileOnly("me.xiaozhangup.octopus:octopus-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("me.xiaozhangup:SlimeCargoNext:1.0.1")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))

    taboo("plutoproject.adventurekt:core:2.1.1") {
        isTransitive = false
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
