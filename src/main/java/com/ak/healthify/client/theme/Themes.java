package com.ak.healthify.client.theme;

import com.ak.healthify.Config;
import com.ak.healthify.Healthify;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;

public final class Themes {
    private static final long CHECK_EVERY_MS = 1000;

    private static final Theme DEFAULT = parseBuiltinDefault();

    private static List<Theme> themes = List.of(DEFAULT);
    private static List<String> errors = List.of();
    private static String folderFingerprint;
    private static long lastCheck;
    private static boolean folderReady;

    private Themes() {}

    public static List<Theme> all() {
        refreshIfChanged();
        return themes;
    }

    public static List<String> errors() {
        refreshIfChanged();
        return errors;
    }

    public static Theme current() {
        String id = Config.THEME.get();
        for (Theme theme : all()) {
            if (theme.id().equals(id)) return theme;
        }
        return themes.isEmpty() ? DEFAULT : themes.get(0);
    }

    public static void select(Theme theme) {
        Config.THEME.set(theme.id());
        Config.THEME.save();
    }

    public static void reload() {
        lastCheck = 0;
        folderFingerprint = null;
        refreshIfChanged();
    }

    public static Path folder() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve(Healthify.MODID).resolve("themes");
    }

    private static void refreshIfChanged() {
        long now = System.currentTimeMillis();
        if (now - lastCheck < CHECK_EVERY_MS) return;
        lastCheck = now;
        try {
            Path folder = folder();
            if (!folderReady) {
                writeBuiltinsIfNew(folder);
                folderReady = true;
            }
            String fingerprint = fingerprint(folder);
            if (fingerprint.equals(folderFingerprint)) return;
            folderFingerprint = fingerprint;
            load(folder);
        } catch (Exception e) {
            Healthify.LOGGER.warn("Healthify: couldn't read the themes folder, using the default theme", e);
            themes = List.of(DEFAULT);
            errors = List.of("themes folder: " + e.getMessage());
        }
    }

    private static void writeBuiltinsIfNew(Path folder) throws IOException {
        if (Files.isDirectory(folder)) return;
        Files.createDirectories(folder);
        for (Map.Entry<String, String> file : BuiltinThemes.FILES.entrySet()) {
            Files.writeString(folder.resolve(file.getKey() + ".json"), file.getValue(), StandardCharsets.UTF_8);
        }
        Files.writeString(folder.resolve("README.txt"), BuiltinThemes.README, StandardCharsets.UTF_8);
        Healthify.LOGGER.info("Healthify: created {} with the built-in themes", folder);
    }

    private static String fingerprint(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) return "";
        List<String> parts = new ArrayList<>();
        try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.json")) {
            for (Path file : files) {
                parts.add(file.getFileName() + ":" + Files.size(file) + ":" + Files.getLastModifiedTime(file).toMillis());
            }
        }
        Collections.sort(parts);
        return String.join("|", parts);
    }

    private static void load(Path folder) throws IOException {
        List<Theme> loaded = new ArrayList<>();
        List<String> problems = new ArrayList<>();
        if (Files.isDirectory(folder)) {
            try (DirectoryStream<Path> files = Files.newDirectoryStream(folder, "*.json")) {
                for (Path file : files) {
                    String fileName = file.getFileName().toString();
                    String id = fileName.substring(0, fileName.length() - ".json".length()).toLowerCase(Locale.ROOT);
                    try {
                        JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                        loaded.add(parse(id, json, DEFAULT));
                    } catch (Exception e) {
                        String problem = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                        problems.add(fileName + ": " + problem);
                        Healthify.LOGGER.warn("Healthify: skipping theme {}: {}", fileName, problem);
                    }
                }
            }
        }
        if (loaded.stream().noneMatch(t -> t.id().equals(DEFAULT.id()))) {
            loaded.add(DEFAULT);
        }
        loaded.sort(Comparator.comparing((Theme t) -> !t.id().equals(DEFAULT.id()))
                .thenComparing(t -> t.name().toLowerCase(Locale.ROOT)));
        themes = Collections.unmodifiableList(loaded);
        errors = Collections.unmodifiableList(problems);
        Healthify.LOGGER.info("Healthify: {} theme(s) loaded{}", loaded.size(),
                problems.isEmpty() ? "" : ", " + problems.size() + " with errors");
    }

    private static Theme parseBuiltinDefault() {
        JsonObject json = JsonParser.parseString(BuiltinThemes.FILES.get(BuiltinThemes.DEFAULT_ID)).getAsJsonObject();
        return parse(BuiltinThemes.DEFAULT_ID, json, null);
    }

    static Theme parse(String id, JsonObject json, Theme base) {
        JsonObject bar = object(json, "bar");
        JsonObject text = object(json, "text");
        JsonObject numbers = object(json, "damageNumbers");

        int height = integer(bar, "height", base == null ? 10 : base.height());
        if (height < Theme.MIN_HEIGHT || height > Theme.MAX_HEIGHT) {
            throw new IllegalArgumentException("\"height\" must be between " + Theme.MIN_HEIGHT + " and " + Theme.MAX_HEIGHT);
        }
        int[] endInsets;
        if (bar.has("endInsets")) {
            JsonArray array = bar.getAsJsonArray("endInsets");
            if (array.size() != height) {
                throw new IllegalArgumentException("\"endInsets\" needs one number per pixel row (" + height + "), it has " + array.size());
            }
            endInsets = new int[height];
            for (int i = 0; i < height; i++) {
                endInsets[i] = Math.max(0, Math.min(20, array.get(i).getAsInt()));
            }
        } else if (bar.has("ends") || base == null || height != base.height()) {
            endInsets = Theme.ends(string(bar, "ends", "pointed"), height);
        } else {
            endInsets = base.endInsets();
        }

        float lowHealth = (float) number(bar, "lowHealth", base == null ? 0.25 : base.lowHealth());
        if (lowHealth < 0 || lowHealth > 1) {
            throw new IllegalArgumentException("\"lowHealth\" must be between 0 and 1");
        }

        return new Theme(
                id,
                string(json, "name", id),
                string(json, "author", ""),
                height,
                endInsets,
                bool(bar, "shading", base == null || base.shading()),
                color(bar, "fill", base == null ? null : base.fill()),
                color(bar, "empty", base == null ? null : base.empty()),
                color(bar, "frame", base == null ? null : base.frame()),
                color(bar, "outline", base == null ? null : base.outline()),
                color(bar, "inner", base == null ? null : base.inner()),
                color(bar, "damageTrail", base == null ? null : base.damageTrail()),
                bar.has("lowHealthFill") ? color(bar, "lowHealthFill", null) : null,
                lowHealth,
                color(text, "name", base == null ? null : base.nameColor()),
                color(text, "health", base == null ? null : base.healthColor()),
                bool(text, "shadow", base == null || base.textShadow()),
                color(numbers, "damage", base == null ? null : base.damageColor()),
                color(numbers, "bigHit", base == null ? null : base.bigHitColor()),
                color(numbers, "kill", base == null ? null : base.killColor()),
                color(numbers, "heal", base == null ? null : base.healColor()));
    }

    private static JsonObject object(JsonObject json, String key) {
        JsonElement value = json.get(key);
        if (value == null || value.isJsonNull()) return new JsonObject();
        if (!value.isJsonObject()) throw new IllegalArgumentException("\"" + key + "\" must be a { ... } section");
        return value.getAsJsonObject();
    }

    private static String string(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        return json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static int color(JsonObject json, String key, Integer fallback) {
        if (!json.has(key)) {
            if (fallback == null) throw new IllegalArgumentException("missing colour \"" + key + "\"");
            return fallback;
        }
        Integer argb = Config.parseColor(json.get(key).getAsString());
        if (argb == null) {
            throw new IllegalArgumentException("\"" + key + "\" is not a colour (use \"#RRGGBB\"): " + json.get(key));
        }
        return argb;
    }
}
