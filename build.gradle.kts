plugins {
    `java-library`
    id("net.neoforged.moddev")
    idea
}

val mcVersion: String = sc.current.version
val isModern: Boolean = sc.current.parsed >= "26.1"
val modId: String = sc.properties["mod.id"]
val modVersion: String = sc.properties["mod.version"]

version = "$modVersion+$mcVersion"
group = sc.properties.get<String>("mod.group")
base.archivesName = modId

java.toolchain.languageVersion = JavaLanguageVersion.of(if (isModern) 25 else 21)

neoForge {
    version = sc.properties.get<String>("deps.neoforge")

    if (!isModern) {
        parchment {
            minecraftVersion = sc.properties.get<String>("deps.parchment_mc")
            mappingsVersion = sc.properties.get<String>("deps.parchment")
        }
    }

    runs {
        register("client") {
            client()
            gameDirectory.set(rootProject.file(if (isModern) "run-$mcVersion" else "run"))
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to mcVersion,
        "minecraft_version_range" to sc.properties.get<String>("mod.mc_range"),
        "neo_version" to sc.properties.get<String>("deps.neoforge"),
        "mod_id" to modId,
        "mod_name" to sc.properties.get<String>("mod.name"),
        "mod_license" to sc.properties.get<String>("mod.license"),
        "mod_version" to modVersion,
        "loader_lines" to (if (isModern) "" else "modLoader=\"javafml\"\nloaderVersion=\"[1,)\""),
        "logo_lines" to (if (sc.current.parsed >= "26.3") "iconFile=\"healthify.png\""
                         else "logoFile=\"healthify.png\"\n# Keep the pixel art sharp in the Mods list\nlogoBlur=false"),
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from(rootProject.file("src/main/templates"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets.main { resources.srcDir(generateModMetadata) }
neoForge.ideSyncTask(generateModMetadata)

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds the mod jar and copies it to build/libs/<mod version>/"
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
