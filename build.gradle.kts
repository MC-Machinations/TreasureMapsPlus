import xyz.jpenilla.runpaper.task.RunServer

plugins {
    java
    checkstyle
    idea
    alias(libs.plugins.paper.userdev)
    alias(libs.plugins.paper.run)
    alias(libs.plugins.spotless)
    alias(libs.plugins.shadow)
}

group = "me.machinemaker"
version = "0.5.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    paperweight.paperDevBundle(libs.versions.minecraft.map { "$it-R0.1-SNAPSHOT" })
    implementation(libs.mirror)
    implementation(libs.reflectionRemapper)

    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

checkstyle {
    configDirectory.set(rootProject.file(".checkstyle"))
    isShowViolations = true
    toolVersion = "10.12.5"
}

spotless {
    java {
        licenseHeaderFile(file("HEADER"))
    }
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    shadowJar {
        isEnableRelocation = true
        relocationPrefix = "me.machinemaker.treasuremapsplus.libs"
    }

    compileJava {
        options.release.set(17)
        options.encoding = Charsets.UTF_8.toString()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.toString()
        filesMatching("paper-plugin.yml") {
            expand("version" to version)
        }
    }

    test {
        useJUnitPlatform()
    }

    withType<RunServer> { // set for both runServer and runMojangMappedServer
        systemProperty("com.mojang.eula.agree", "true")

        downloadPlugins {
            url("https://download.luckperms.net/1526/bukkit/loader/LuckPerms-Bukkit-5.4.113.jar")
        }
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
    }

    create("printVersion") {
        doFirst {
            println(version)
        }
    }
}
