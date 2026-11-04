plugins {
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin")
}

val mcVersion: String = sc.current.version
val modId: String = sc.properties["mod.id"]
val modVersion: String = sc.properties["mod.version"]

version = "$modVersion+$mcVersion"
base.archivesName = "$modId-fabric"

val requiredJava: JavaVersion = if (sc.current.parsed >= "26.1") JavaVersion.VERSION_25 else JavaVersion.VERSION_21

repositories {
    exclusiveContent {
        forRepository { maven("https://maven.terraformersmc.com/releases") { name = "TerraformersMC" } }
        filter { includeGroup("com.terraformersmc") }
    }
}

dependencies {
    fun fapi(vararg modules: String) {
        for (it in modules) modImplementation(fabricApi.module(it, sc.properties["deps.fabric_api"]))
    }

    minecraft("com.mojang:minecraft:$mcVersion")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${sc.properties.get<String>("deps.fabric_loader")}")
    val resourceLoader = if (sc.current.parsed >= "26.1") "fabric-resource-loader-v1" else "fabric-resource-loader-v0"
    fapi("fabric-api-base", "fabric-lifecycle-events-v1", "fabric-rendering-v1", resourceLoader)
    modCompileOnly("com.terraformersmc:modmenu:${sc.properties.get<String>("deps.modmenu")}")

    modLocalRuntime("net.fabricmc.fabric-api:fabric-api:${sc.properties.get<String>("deps.fabric_api")}")
    modLocalRuntime("com.terraformersmc:modmenu:${sc.properties.get<String>("deps.modmenu")}") {
        isTransitive = false
    }
}

loom {
    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file(if (sc.current.parsed >= "26.1") "run-fabric-$mcVersion" else "run-fabric")
    }
}

java {
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
    toolchain {
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

sourceSets.main {
    java.exclude("com/ak/healthify/neoforge/**")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }

    processResources {
        val props = mapOf(
            "id" to modId,
            "name" to sc.properties.get<String>("mod.name"),
            "version" to modVersion,
            "description" to sc.properties.get<String>("mod.description"),
            "license" to sc.properties.get<String>("mod.license"),
            "minecraft" to sc.properties.get<String>("mod.mc_compat"),
            "loader" to Regex("\\d+\\.\\d+").find(sc.properties.get<String>("deps.fabric_loader"))!!.value,
        )
        inputs.properties(props)
        filesMatching("fabric.mod.json") { expand(props) }
        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        inputs.property("mixin_java", mixinJava)
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    withType<Jar> {
        exclude("data/healthify/function/**")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds the mod jar and copies it to build/libs/<mod version>/"
        from(loomx.modJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
        rename { "[Fabric] $modVersion - $mcVersion.jar" }
    }
}

publishMods {
    val token = providers.environmentVariable("CURSEFORGE_TOKEN")
    file.set(loomx.modJar.flatMap { it.archiveFile })
    displayName.set("[Fabric] $modVersion - $mcVersion")
    version.set("$modVersion+$mcVersion-fabric")
    changelog.set(providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOG.md")).asText.map {
        it.replace("\r\n", "\n").split(Regex("(?m)^(?=## )")).firstOrNull { part -> part.isNotBlank() }?.trim() ?: it
    })
    type.set(STABLE)
    modLoaders.add("fabric")
    dryRun.set(token.map { it.isBlank() }.orElse(true))

    curseforge {
        projectId.set(sc.properties.get<String>("publish.curseforge_id"))
        accessToken.set(token)
        minecraftVersions.add(mcVersion)
        javaVersions.add(requiredJava)
        client.set(true)
        server.set(false)
        requires("fabric-api")
        optional("dynamic-difficulty-mod", "dungeon-difficulty", "modmenu")
    }
}
