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
version = "0.4.0"

repositories {
    mavenLocal() {
        mavenContent {
            includeModule("io.papermc.paper", "paper-api")
        }
    }
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.2-R0.1-SNAPSHOT")
    paperweight.paperDevBundle(libs.versions.minecraft.map { "$it-R0.1-SNAPSHOT" }) {
        exclude("io.papermc.paper", "paper-api")
    }
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
    toolVersion = "10.12.3"
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
