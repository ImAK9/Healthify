plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1-neoforge"

stonecutter parameters {
    val (version, loader) = current.project.split('-', limit = 2)

    properties {
        tags(version, loader)
    }

    constants {
        match(loader, "fabric", "neoforge")
    }

    replacements {
        string(current.parsed >= "26.1") {
            replace("ResourceLocation", "Identifier")
        }
    }
}
