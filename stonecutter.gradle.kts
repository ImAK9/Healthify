plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1"

stonecutter parameters {
    replacements {
        string(current.parsed >= "26.1") {
            replace("ResourceLocation", "Identifier")
        }
    }
}
