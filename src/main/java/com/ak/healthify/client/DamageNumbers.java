package com.ak.healthify.client;

import com.ak.healthify.Config;
import com.ak.healthify.client.theme.Theme;
import com.ak.healthify.client.theme.Themes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class DamageNumbers {
    private static final float BASE_SCALE = 0.025F;
    private static final int MAX_POPUPS = 64;
    private static final float MIN_CHANGE = 0.05F;
    private static final float RISE = 0.7F;

    private static final float WORD_SCALE = 0.55F;

    private record Popup(Vec3 origin, Component text, Component word, int color, float size, long start, float drift) {}

    private static final List<Popup> POPUPS = new ArrayList<>();
    private static final Map<Integer, float[]> LAST_HEALTH = new HashMap<>();
    private static final Random RANDOM = new Random();

    private DamageNumbers() {}

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            LAST_HEALTH.clear();
            POPUPS.clear();
            return;
        }
        if (!Config.DAMAGE_NUMBERS.get()) {
            LAST_HEALTH.clear();
            return;
        }

        double maxDistance = Config.MAX_DISTANCE.get();
        Vec3 eye = mc.player.getEyePosition();
        Map<Integer, float[]> seen = new HashMap<>();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == mc.player || living instanceof ArmorStand) continue;
            if (living instanceof Player && !Config.SHOW_ON_PLAYERS.get()) continue;
            if (BarVisibility.isBlacklisted(living)) continue;
            if (living.distanceToSqr(eye) > maxDistance * maxDistance) continue;

            float health = living.getHealth();
            float maxHealth = living.getMaxHealth();
            seen.put(living.getId(), new float[] {health, maxHealth});

            float[] previous = LAST_HEALTH.get(living.getId());
            if (previous == null) continue;
            float change = health - previous[0];
            if (Math.abs(change) < MIN_CHANGE) continue;
            if (living.isInvisibleTo(mc.player)) continue;
            if (Config.REQUIRE_LINE_OF_SIGHT.get() && !mc.player.hasLineOfSight(living)) continue;

            if (change < 0) {
                spawn(living, -change, health <= 0.0F, maxHealth, false);
            } else if (Config.SHOW_HEAL_NUMBERS.get() && previous[1] == maxHealth) {
                spawn(living, change, false, maxHealth, true);
            }
        }

        LAST_HEALTH.clear();
        LAST_HEALTH.putAll(seen);
    }

    private static void spawn(LivingEntity entity, float amount, boolean lethal, float maxHealth, boolean heal) {
        Theme theme = Themes.current();
        int color;
        float size;
        List<String> words = List.of();
        if (heal) {
            color = theme.healColor();
            size = 1.0F;
        } else if (lethal) {
            color = theme.killColor();
            size = 1.2F;
            words = theme.killWords();
        } else if (amount >= maxHealth * Config.BIG_HIT_THRESHOLD.get()) {
            color = theme.bigHitColor();
            size = 1.12F;
            words = theme.bigHitWords();
        } else {
            color = theme.damageColor();
            size = 1.0F;
        }

        boolean flair = !heal && (lethal || amount >= maxHealth * Config.BIG_HIT_THRESHOLD.get());
        String text = (heal ? "+" : "") + Numbers.format(amount, Config.DAMAGE_NUMBER_DECIMALS.get());
        if (flair && Config.EXCLAIM_BIG_HITS.get()) text += "!";
        Component word = null;
        if (Config.HIT_WORDS.get() && !words.isEmpty()) {
            word = Component.literal(words.get(RANDOM.nextInt(words.size()))).withStyle(ChatFormatting.BOLD);
        }
        double spread = entity.getBbWidth() * 0.6;
        Vec3 origin = entity.position().add(
                (RANDOM.nextDouble() - 0.5) * spread,
                entity.getBbHeight() * 0.6 + RANDOM.nextDouble() * 0.3,
                (RANDOM.nextDouble() - 0.5) * spread);
        float drift = (RANDOM.nextFloat() - 0.5F) * 0.8F;

        POPUPS.add(new Popup(origin, Component.literal(text).withStyle(ChatFormatting.BOLD), word, color, size, System.currentTimeMillis(), drift));
        if (POPUPS.size() > MAX_POPUPS) {
            POPUPS.remove(0);
        }
    }

    public static List<Placed> collect(Vec3 camera) {
        List<Placed> out = new ArrayList<>();
        if (POPUPS.isEmpty()) return out;
        if (!Config.DAMAGE_NUMBERS.get()) {
            POPUPS.clear();
            return out;
        }

        Font font = Minecraft.getInstance().font;
        long now = System.currentTimeMillis();
        long duration = Math.max(50L, Math.round(Config.DAMAGE_NUMBER_SECONDS.get() * 1000.0));
        POPUPS.removeIf(p -> now - p.start() > duration);

        boolean outline = Config.DAMAGE_NUMBER_OUTLINE.get();
        int thickness = Config.DAMAGE_NUMBER_OUTLINE_THICKNESS.get();
        Integer themeOutline = Themes.current().numberOutline();
        float baseScale = BASE_SCALE * Config.DAMAGE_NUMBER_SCALE.get().floatValue();

        for (Popup popup : POPUPS) {
            float t = Numbers.clamp((now - popup.start()) / (float) duration, 0.0F, 1.0F);

            float rise = RISE * (1.0F - (1.0F - t) * (1.0F - t));
            float pop;
            if (t < 0.1F) pop = Numbers.lerp(t / 0.1F, 0.5F, 1.2F);
            else if (t < 0.25F) pop = Numbers.lerp((t - 0.1F) / 0.15F, 1.2F, 1.0F);
            else pop = 1.0F;
            float alpha = t < 0.65F ? 1.0F : 1.0F - (t - 0.65F) / 0.35F;
            int a = Math.round(alpha * 255.0F);
            if (a < 8) continue;

            DrawList draw = new DrawList();
            float x = -font.width(popup.text()) / 2.0F;
            float y = -font.lineHeight / 2.0F;
            int outlineColor = (a << 24) | (themeOutline != null ? themeOutline & 0x00FFFFFF : darken(popup.color()));
            int fillColor = (a << 24) | (popup.color() & 0x00FFFFFF);
            if (outline) outline(draw, popup.text(), x, y, outlineColor, 0.0F, 0.0F, 1.0F, thickness);
            draw.text(popup.text(), x, y, fillColor, false);
            if (popup.word() != null) {
                float wordY = y - font.lineHeight * WORD_SCALE - 1.0F;
                float wordX = -font.width(popup.word()) / 2.0F;
                if (outline) outline(draw, popup.word(), wordX, 0.0F, outlineColor, 0.0F, wordY, WORD_SCALE, thickness);
                draw.scaledText(popup.word(), wordX, 0.0F, fillColor, 0.0F, wordY, WORD_SCALE, false);
            }

            Vec3 pos = popup.origin().add(0.0, rise, 0.0);
            double dx = pos.x - camera.x;
            double dy = pos.y - camera.y;
            double dz = pos.z - camera.z;
            out.add(new Placed(pos.x, pos.y, pos.z, popup.drift() * t, baseScale * popup.size() * pop, draw, dx * dx + dy * dy + dz * dz));
        }
        out.sort((p1, p2) -> Double.compare(p2.distanceSq(), p1.distanceSq()));
        return out;
    }

    private static void outline(DrawList draw, Component text, float x, float y, int color, float originX, float originY, float scale, int thickness) {
        for (int dx = -thickness; dx <= thickness; dx++) {
            for (int dy = -thickness; dy <= thickness; dy++) {
                if (dx == 0 && dy == 0) continue;
                draw.underlay(text, x + dx, y + dy, color, originX, originY, scale);
            }
        }
    }

    private static int darken(int argb) {
        int r = ((argb >> 16) & 0xFF) / 6;
        int g = ((argb >> 8) & 0xFF) / 6;
        int b = (argb & 0xFF) / 6;
        return (r << 16) | (g << 8) | b;
    }
}
