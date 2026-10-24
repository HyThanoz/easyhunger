plugins {
    id("java")
}

group = "es.mytale"
version = "1.5.26"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// Ruta unica al HytaleServer.jar, definida en ~/.gradle/gradle.properties
// (hytaleServerJar=...). Todos los mods de MyTale compilan contra el mismo jar.
val hytaleServerJar: String = (findProperty("hytaleServerJar") as String?)
    ?: System.getProperty("user.home") + "/AppData/Roaming/Hytale/install/release/package/game/latest/Server/HytaleServer.jar"

dependencies {
    compileOnly(files(hytaleServerJar))
    // libs/ queda para dependencias opcionales de otros mods, no para el server jar.
    compileOnly(fileTree("libs") { include("*.jar") })
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
    doFirst {
        require(file(hytaleServerJar).exists()) {
            "No encuentro HytaleServer.jar en:\n  $hytaleServerJar\n" +
            "Corrige la ruta en C:/Users/<usuario>/.gradle/gradle.properties (hytaleServerJar=...)"
        }
    }
}

/**
 * Generate manifest.json content as a generated resource
 * using Gradle's group/name/version before every build/processResources.
 */
val generatedResourcesDir: Provider<Directory> = layout.buildDirectory.dir("generated/resources/main")

val generateManifest by tasks.registering {
    val outFile = generatedResourcesDir.map { it.file("manifest.json") }

    // Regenerate if any of these change
    inputs.property("group", project.group.toString())
    inputs.property("name", rootProject.name)
    inputs.property("version", project.version.toString())

    outputs.file(outFile)

    doLast {
        val f = outFile.get().asFile
        f.parentFile.mkdirs()

        val json = """
            {
              "Group": "${project.group}",
              "Name": "${rootProject.name}",
              "Version": "${project.version}",
              "ServerVersion": "*",
              "Main": "com.haas.easyhunger.EasyHunger",
              "Authors": [ { "Name": "haasapenas" } ],
              "IncludesAssetPack": true
            }
        """.trimIndent() + "\n"

        f.writeText(json, Charsets.UTF_8)
    }
}

// Make Gradle include the generated resources directory in the main resources
sourceSets {
    named("main") {
        resources.srcDir(generatedResourcesDir)
    }
}

// Ensure it's generated before resources are packaged (build/jar/run/etc.)
tasks.named<ProcessResources>("processResources") {
    dependsOn(generateManifest)
}



// AGPL-3.0: el LICENSE va dentro del jar (corremos una version modificada; ver MYTALE-CHANGES.md).
tasks.jar {
    from("LICENSE")
}
