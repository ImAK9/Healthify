package com.ak.healthify.client.compat;

import com.ak.healthify.Healthify;
import com.ak.healthify.platform.Platform;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class DynamicDifficultyCompat {
    public static final String MOD_ID = "dynamic_difficulty";

    private static final String API_CLASS = "dev.muon.dynamic_difficulty.api.LevelingAPI";
    private static final String PLATE_CLASS = "dev.muon.dynamic_difficulty.client.LevelPlateHandler";

    private static boolean initialized;
    private static boolean available;
    private static MethodHandle hasLevel;
    private static MethodHandle shouldShowLevel;
    private static MethodHandle getLevel;
    private static MethodHandle getLevelColor;

    private DynamicDifficultyCompat() {}

    public static boolean isAvailable() {
        init();
        return available;
    }

    public static int getLevel(LivingEntity entity) {
        if (!isAvailable()) return -1;
        try {
            if (!(boolean) hasLevel.invoke((Entity) entity)) return -1;
            if (!(boolean) shouldShowLevel.invoke((Entity) entity)) return -1;
            return (int) getLevel.invoke(entity);
        } catch (Throwable t) {
            disable("reading a mob level", t);
            return -1;
        }
    }

    public static int getLevelColor(Player viewer, LivingEntity entity, int fallback) {
        if (!isAvailable() || getLevelColor == null) return fallback;
        try {
            return (int) getLevelColor.invoke(viewer, entity);
        } catch (Throwable t) {
            getLevelColor = null;
            Healthify.LOGGER.warn("Healthify: couldn't read Dynamic Difficulty level colours, using the fallback colour", t);
            return fallback;
        }
    }

    private static void init() {
        if (initialized) return;
        initialized = true;
        if (!Platform.isModLoaded(MOD_ID)) return;
        try {
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            Class<?> api = Class.forName(API_CLASS);
            hasLevel = lookup.findStatic(api, "hasLevel", MethodType.methodType(boolean.class, Entity.class));
            shouldShowLevel = lookup.findStatic(api, "shouldShowLevel", MethodType.methodType(boolean.class, Entity.class));
            getLevel = lookup.findStatic(api, "getLevel", MethodType.methodType(int.class, LivingEntity.class));
            available = true;
            Healthify.LOGGER.info("Healthify: Dynamic Difficulty found, showing mob levels in health bars");
        } catch (Throwable t) {
            Healthify.LOGGER.warn("Healthify: Dynamic Difficulty is installed but its API couldn't be reached; levels won't be shown", t);
            return;
        }
        try {
            Class<?> plate = Class.forName(PLATE_CLASS);
            getLevelColor = MethodHandles.publicLookup().findStatic(plate, "getLevelColor",
                    MethodType.methodType(int.class, Player.class, LivingEntity.class));
        } catch (Throwable t) {
            getLevelColor = null;
        }
    }

    private static void disable(String what, Throwable t) {
        available = false;
        Healthify.LOGGER.warn("Healthify: Dynamic Difficulty error while {}; turning level display off", what, t);
    }
}
