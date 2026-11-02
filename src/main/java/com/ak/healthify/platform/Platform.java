package com.ak.healthify.platform;

import java.nio.file.Path;

public final class Platform {
    private Platform() {}

    public static boolean isModLoaded(String id) {
        //? if fabric {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded(id);
        *///?} else {
        return net.neoforged.fml.ModList.get().isLoaded(id);
        //?}
    }

    public static Path configDir() {
        //? if fabric {
        /*return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir();
        *///?} else {
        return net.neoforged.fml.loading.FMLPaths.CONFIGDIR.get();
        //?}
    }
}
