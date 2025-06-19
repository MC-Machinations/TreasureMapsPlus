import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
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
version = "0.7.2"

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
    testRuntimeOnly(libs.junit.platform)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
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

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

tasks {
    assemble {
        dependsOn(shadowJar)
    }

    shadowJar {
        isEnableRelocation = true
        relocationPrefix = "me.machinemaker.treasuremapsplus.libs"
    }

    compileJava {
        options.release = 21
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
        testLogging {
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.STANDARD_OUT)
        }
    }

    withType<RunServer> { // set for both runServer and runMojangMappedServer
        systemProperty("com.mojang.eula.agree", "true")

        downloadPlugins {
            modrinth("luckperms", "v5.4.145-bukkit")
        }
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
    }

    register("printVersion") {
        doFirst {
            println(version)
        }
    }
}
