import io.izzel.taboolib.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    id("io.izzel.taboolib") version "2.0.22"
    kotlin("jvm") version "2.1.10"
}

taboolib {
    env {
        // 安装模块
        install(
            Bukkit,
            BukkitHook,
            BukkitUI,
            BukkitNMS,
            BukkitNMSUtil,
            BukkitUtil,
            Basic,
            MinecraftChat,
            CommandHelper,
            AlkaidRedis,
            Database
        )
    }
    version {
        taboolib = "6.2.3-20d868d"
        coroutines = "1.10.1"
//        skipKotlinRelocate = true
//        skipKotlin = true
    }

    relocate("ink.pmc.advkt", "me.xiaozhangup.dolphin.lib.advkt")
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.nostal.ink/repository/maven-public/")
}

dependencies {
    compileOnly("me.xiaozhangup.octopus:octopus-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("ink.ptms.core:v12104:12104-minimize:mapped")
    compileOnly("ink.ptms.core:v12104:12104-minimize:universal")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))

    taboo("ink.pmc.advkt:core:1.0.1")
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
