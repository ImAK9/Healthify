package com.ak.healthify.client.compat;

import com.ak.healthify.Config;
import com.ak.healthify.Healthify;
import com.ak.healthify.platform.Platform;
import java.lang.reflect.Method;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public final class DungeonDifficultyCompat {
    public static final String MOD_ID = "dungeon_difficulty";

    private static boolean initialized;
    private static boolean available;
    private static Method getHelper;
    private static Method getAttachmentHelper;
    private static Method getData;
    private static Method difficultyName;
    private static Method level;

    private DungeonDifficultyCompat() {}

    public static boolean isAvailable() {
        init();
        return available;
    }

    public static Component forBar(LivingEntity entity) {
        if (!Config.SHOW_DUNGEON_DIFFICULTY.get() || !isAvailable()) return null;
        try {
            Object helper = getHelper.invoke(null);
            Object attachments = helper == null ? null : getAttachmentHelper.invoke(helper);
            Object data = attachments == null ? null : getData.invoke(attachments, entity);
            if (data == null) return null;
            String name = (String) difficultyName.invoke(data);
            int value = (int) level.invoke(data);
            if (name == null || name.isEmpty() || value <= 0) return null;
            String key = "difficulty.type." + name.toLowerCase(Locale.ROOT);
            return Component.translatableWithFallback(key, capitalize(name) + " %s", value)
                    .withStyle(style -> style.withColor(colorFor(value)));
        } catch (Throwable t) {
            disable(t);
            return null;
        }
    }

    private static int colorFor(int value) {
        if (value <= 2) return 0x90EE90;
        if (value <= 4) return 0x3CB371;
        if (value <= 6) return 0xFFD700;
        if (value <= 8) return 0xFF8C00;
        return 0xDC143C;
    }

    private static String capitalize(String name) {
        String spaced = name.replace('_', ' ');
        return spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
    }

    private static void init() {
        if (initialized) return;
        initialized = true;
        if (!Platform.isModLoaded(MOD_ID) || !Platform.isModLoaded(DynamicDifficultyCompat.MOD_ID)) return;
        try {
            Class<?> main = Class.forName("dev.muon.dynamic_difficulty.DynamicDifficulty");
            Class<?> platform = Class.forName("dev.muon.dynamic_difficulty.platform.PlatformHelper");
            Class<?> attachments = Class.forName("dev.muon.dynamic_difficulty.compat.dungeon_difficulty.DungeonDifficultyAttachmentHelper");
            Class<?> data = Class.forName("dev.muon.dynamic_difficulty.compat.dungeon_difficulty.DungeonDifficultyData");
            getHelper = main.getMethod("getHelper");
            getAttachmentHelper = platform.getMethod("getDungeonDifficultyAttachmentHelper");
            getData = attachments.getMethod("getData", LivingEntity.class);
            difficultyName = data.getMethod("difficultyName");
            level = data.getMethod("level");
            available = true;
        } catch (Throwable t) {
            Healthify.LOGGER.warn("Dungeon Difficulty is installed but Healthify couldn't read it through Dynamic Difficulty", t);
        }
    }

    private static void disable(Throwable t) {
        if (!available) return;
        available = false;
        Healthify.LOGGER.warn("Healthify turned off Dungeon Difficulty support after an error", t);
    }
}
