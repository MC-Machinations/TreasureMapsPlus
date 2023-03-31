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
version = "0.2.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public")
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(libs.mirror)

    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

checkstyle {
    configDirectory.set(rootProject.file(".checkstyle"))
    isShowViolations = true
    toolVersion = "10.3"
}

spotless {
    java {
        licenseHeaderFile(file("HEADER"))
    }
}

//license {
//    header(rootProject.file("HEADER"))
//    properties {
//        set("year", "2023")
//        set("name", "Machine_Maker")
//    }
//    newLine.set(false)
//    include("*.java")
//}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.release.set(17)
        options.encoding = Charsets.UTF_8.toString()
    }

    processResources {
        filteringCharset = Charsets.UTF_8.toString()
        filesMatching("paper-plugin.yml") {
            expand("version" to project.version)
        }
    }

    test {
        useJUnitPlatform()
    }

    withType<RunServer> {
        systemProperty("com.mojang.eula.agree", "true")
    }

    runServer {
        minecraftVersion("1.19.4")
    }
}