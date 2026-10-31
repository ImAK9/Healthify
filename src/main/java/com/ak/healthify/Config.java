package com.ak.healthify;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private Config() {}

    public enum ShowMode {
        ALWAYS,
        WHEN_DAMAGED,
        LOOKING_AT,
        LOOKING_AT_OR_DAMAGED
    }

    public enum HealthFormat { CURRENT_AND_MAX, CURRENT, PERCENT }

    public enum LevelPosition { BEFORE_NAME, AFTER_NAME, ABOVE_NAME, LEFT_OF_BAR, RIGHT_OF_BAR, INSIDE_BAR_LEFT, INSIDE_BAR_RIGHT }

    public enum LevelColor {
        DIFFICULTY,
        CUSTOM
    }

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static { BUILDER.push("visibility"); }

    public static final ModConfigSpec.EnumValue<ShowMode> SHOW_MODE = BUILDER
            .comment("When health bars are shown: ALWAYS, WHEN_DAMAGED, LOOKING_AT or LOOKING_AT_OR_DAMAGED")
            .defineEnum("showMode", ShowMode.LOOKING_AT);

    public static final ModConfigSpec.DoubleValue MAX_DISTANCE = BUILDER
            .comment("Bars are only shown for mobs within this many blocks")
            .defineInRange("maxDistance", 8.0, 1.0, 256.0);

    public static final ModConfigSpec.DoubleValue LOOK_LINGER_SECONDS = BUILDER
            .comment("LOOKING_AT modes: how many seconds a bar stays after you look away from a mob (looking at another mob switches instantly)")
            .defineInRange("lookLingerSeconds", 0.0, 0.0, 60.0);

    public static final ModConfigSpec.BooleanValue REQUIRE_LINE_OF_SIGHT = BUILDER
            .comment("Hide bars for mobs you can't see (behind walls)")
            .define("requireLineOfSight", true);

    public static final ModConfigSpec.BooleanValue DRAW_ON_TOP = BUILDER
            .comment("Draw bars on top of blocks so walls and ceilings never cut into them")
            .define("drawOnTop", true);

    public static final ModConfigSpec.BooleanValue SHOW_ON_PLAYERS = BUILDER
            .comment("Show bars on other players too")
            .define("showOnPlayers", false);

    public static final ModConfigSpec.BooleanValue SHOW_ON_PASSIVE = BUILDER
            .comment("Show bars on passive mobs (cows, pigs, villagers, fish...)")
            .define("showOnPassive", false);

    public static final ModConfigSpec.BooleanValue SHOW_ON_NEUTRAL = BUILDER
            .comment("Show bars on neutral mobs that only fight back when provoked (wolves, iron golems, bees, polar bears...)")
            .define("showOnNeutral", true);

    public static final ModConfigSpec.BooleanValue HIDE_VANILLA_NAME_TAGS = BUILDER
            .comment("Hide the vanilla name tag on mobs that have a Healthify bar (the bar shows the name instead)")
            .define("hideVanillaNameTags", true);

    static { BUILDER.pop(); }

    static { BUILDER.push("placement"); }

    public static final ModConfigSpec.DoubleValue HEIGHT_ABOVE_MOB = BUILDER
            .comment("Gap in blocks between the top of the mob and the bottom of the bar")
            .defineInRange("heightAboveMob", 0.15, -5.0, 5.0);

    public static final ModConfigSpec.DoubleValue SCALE = BUILDER
            .comment("Overall size of the bar, name and text (1.0 = name tag size)")
            .defineInRange("scale", 1.0, 0.1, 5.0);

    static { BUILDER.pop(); }

    static { BUILDER.push("bar"); }

    public static final ModConfigSpec.ConfigValue<String> THEME = BUILDER
            .comment("Health bar theme: a file name (without .json) from config/healthify/themes.",
                     "Themes set the bar's colours and shape, text colours and damage number colours.",
                     "Easiest to pick in game: Mods > Healthify > Config.")
            .define("theme", "healthify");

    public static final ModConfigSpec.BooleanValue SHOW_BAR = BUILDER
            .comment("Show the bar itself")
            .define("showBar", true);

    public static final ModConfigSpec.IntValue BAR_WIDTH = BUILDER
            .comment("Bar width in pixels")
            .defineInRange("barWidth", 110, 20, 400);

    public static final ModConfigSpec.IntValue BAR_X = BUILDER
            .comment("Bar X offset")
            .defineInRange("barX", 0, -500, 500);

    public static final ModConfigSpec.IntValue BAR_Y = BUILDER
            .comment("Bar Y offset")
            .defineInRange("barY", 0, -500, 500);

    public static final ModConfigSpec.BooleanValue DAMAGE_TRAIL = BUILDER
            .comment("When a mob is hit, the lost health flashes in the trail colour and then drains away")
            .define("damageTrail", true);

    static { BUILDER.pop(); }

    static { BUILDER.push("name"); }

    public static final ModConfigSpec.BooleanValue SHOW_NAME = BUILDER
            .comment("Show the mob's name")
            .define("showName", true);

    public static final ModConfigSpec.IntValue NAME_X = BUILDER
            .comment("Name X offset")
            .defineInRange("nameX", 0, -500, 500);

    public static final ModConfigSpec.IntValue NAME_Y = BUILDER
            .comment("Name Y offset")
            .defineInRange("nameY", 0, -500, 500);

    static { BUILDER.pop(); }

    static { BUILDER.push("healthText"); }

    public static final ModConfigSpec.BooleanValue SHOW_HEALTH_TEXT = BUILDER
            .comment("Show the health numbers")
            .define("showHealthText", true);

    public static final ModConfigSpec.EnumValue<HealthFormat> HEALTH_FORMAT = BUILDER
            .comment("CURRENT_AND_MAX = 9/10, CURRENT = 9, PERCENT = 90%")
            .defineEnum("healthFormat", HealthFormat.CURRENT_AND_MAX);

    public static final ModConfigSpec.DoubleValue HEALTH_TEXT_SCALE = BUILDER
            .comment("Size of the health numbers compared to the name")
            .defineInRange("healthTextScale", 0.75, 0.25, 3.0);

    public static final ModConfigSpec.IntValue HEALTH_TEXT_X = BUILDER
            .comment("Health text X offset")
            .defineInRange("healthTextX", 0, -500, 500);

    public static final ModConfigSpec.IntValue HEALTH_TEXT_Y = BUILDER
            .comment("Health text Y offset")
            .defineInRange("healthTextY", 0, -500, 500);

    static { BUILDER.pop(); }

    static { BUILDER.push("level"); }

    public static final ModConfigSpec.BooleanValue SHOW_LEVEL = BUILDER
            .comment("Show the mob's level from Dynamic Difficulty (does nothing if that mod isn't installed)")
            .define("showLevel", true);

    public static final ModConfigSpec.EnumValue<LevelPosition> LEVEL_POSITION = BUILDER
            .comment("Where the level goes: AFTER_NAME, BEFORE_NAME, ABOVE_NAME, LEFT_OF_BAR, RIGHT_OF_BAR, INSIDE_BAR_LEFT, INSIDE_BAR_RIGHT")
            .defineEnum("levelPosition", LevelPosition.BEFORE_NAME);

    public static final ModConfigSpec.IntValue LEVEL_X = BUILDER
            .comment("Level X offset (added on top of levelPosition)")
            .defineInRange("levelX", 0, -500, 500);

    public static final ModConfigSpec.IntValue LEVEL_Y = BUILDER
            .comment("Level Y offset (added on top of levelPosition)")
            .defineInRange("levelY", 0, -500, 500);

    public static final ModConfigSpec.ConfigValue<String> LEVEL_FORMAT = BUILDER
            .comment("Level text; %s is replaced by the level number")
            .define("levelFormat", "Lv. %s");

    public static final ModConfigSpec.EnumValue<LevelColor> LEVEL_COLOR_MODE = BUILDER
            .comment("DIFFICULTY = green/yellow/red like Dynamic Difficulty, CUSTOM = levelCustomColor")
            .defineEnum("levelColorMode", LevelColor.DIFFICULTY);

    public static final ModConfigSpec.ConfigValue<String> LEVEL_CUSTOM_COLOR = color("levelCustomColor", "#FFD700", "Level colour when levelColorMode is CUSTOM");

    static { BUILDER.pop(); }

    static { BUILDER.push("damageNumbers"); }

    public static final ModConfigSpec.BooleanValue DAMAGE_NUMBERS = BUILDER
            .comment("RPG-style floating numbers when mobs take damage")
            .define("damageNumbers", true);

    public static final ModConfigSpec.BooleanValue SHOW_HEAL_NUMBERS = BUILDER
            .comment("Also show green numbers when mobs heal")
            .define("showHealNumbers", true);

    public static final ModConfigSpec.DoubleValue DAMAGE_NUMBER_SECONDS = BUILDER
            .comment("How long each number stays on screen, in seconds")
            .defineInRange("damageNumberSeconds", 1.0, 0.2, 5.0);

    public static final ModConfigSpec.DoubleValue DAMAGE_NUMBER_SCALE = BUILDER
            .comment("Size of the numbers (1.0 = name tag size)")
            .defineInRange("damageNumberScale", 0.8, 0.1, 5.0);

    public static final ModConfigSpec.BooleanValue DAMAGE_NUMBER_OUTLINE = BUILDER
            .comment("Dark outline around the numbers")
            .define("damageNumberOutline", true);

    public static final ModConfigSpec.DoubleValue BIG_HIT_THRESHOLD = BUILDER
            .comment("A hit counts as big (bigger, orange number) when it takes at least this share of the mob's max health (0.25 = 25%)")
            .defineInRange("bigHitThreshold", 0.25, 0.0, 1.0);

    static { BUILDER.pop(); }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static ModConfigSpec.ConfigValue<String> color(String key, String def, String comment) {
        return BUILDER.comment(comment + " (hex, e.g. " + def + ")").define(key, def, Config::isColor);
    }

    private static boolean isColor(Object value) {
        return value instanceof String s && parseColor(s) != null;
    }

    public static Integer parseColor(String s) {
        String hex = s.trim();
        if (hex.startsWith("#")) hex = hex.substring(1);
        if (hex.length() != 6 && hex.length() != 8) return null;
        try {
            long v = Long.parseLong(hex, 16);
            return hex.length() == 6 ? (int) (0xFF000000L | v) : (int) v;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int argb(ModConfigSpec.ConfigValue<String> value) {
        Integer c = parseColor(value.get());
        return c != null ? c : 0xFFFFFFFF;
    }
}
