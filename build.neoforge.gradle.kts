plugins {
    `java-library`
    id("net.neoforged.moddev")
    id("me.modmuss50.mod-publish-plugin")
    idea
}

val mcVersion: String = sc.current.version
val isModern: Boolean = sc.current.parsed >= "26.1"
val modId: String = sc.properties["mod.id"]
val modVersion: String = sc.properties["mod.version"]

version = "$modVersion+$mcVersion"
group = sc.properties.get<String>("mod.group")
base.archivesName = "$modId-neoforge"

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
        "neo_version_min" to sc.properties.get<String>("deps.neoforge_min"),
        "mod_id" to modId,
        "mod_name" to sc.properties.get<String>("mod.name"),
        "mod_license" to sc.properties.get<String>("mod.license"),
        "mod_version" to modVersion,
        "loader_lines" to (if (isModern) "" else "modLoader=\"javafml\"\nloaderVersion=\"[1,)\""),
        "logo_lines" to (if (sc.current.parsed >= "26.3") "iconFile=\"healthify.png\""
                         else "logoFile=\"healthify.png\"\nlogoBlur=false"),
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from(rootProject.file("src/main/templates"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets.main {
    resources.srcDir(generateModMetadata)
    java.exclude("com/ak/healthify/fabric/**")
}

tasks.processResources {
    exclude("fabric.mod.json", "healthify.mixins.json")
}
neoForge.ideSyncTask(generateModMetadata)

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named<Jar>("jar") {
    exclude("data/healthify/function/**")
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds the mod jar and copies it to build/libs/<mod version>/"
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
    rename { "[NeoForge] $modVersion - $mcVersion.jar" }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

publishMods {
    val token = providers.environmentVariable("CURSEFORGE_TOKEN")
    file.set(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    displayName.set("[NeoForge] $modVersion - $mcVersion")
    version.set("$modVersion+$mcVersion-neoforge")
    changelog.set(providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOG.md")).asText.map {
        it.replace("\r\n", "\n").split(Regex("(?m)^(?=## )")).firstOrNull { part -> part.isNotBlank() }?.trim() ?: it
    })
    type.set(STABLE)
    modLoaders.add("neoforge")
    dryRun.set(token.map { it.isBlank() }.orElse(true))

    curseforge {
        projectId.set(sc.properties.get<String>("publish.curseforge_id"))
        accessToken.set(token)
        minecraftVersions.add(mcVersion)
        javaVersions.add(if (isModern) JavaVersion.VERSION_25 else JavaVersion.VERSION_21)
        client.set(true)
        server.set(false)
        optional("dynamic-difficulty-mod", "dungeon-difficulty")
        if (!isModern) optional("l2hostility")
        optional("champions-unofficial")
    }
}
