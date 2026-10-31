package com.ak.healthify.client;

import com.ak.healthify.Config;
import com.ak.healthify.client.theme.Theme;
import com.ak.healthify.client.theme.Themes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class HealthBars {
    private static final float BASE_SCALE = 0.025F;
    private static final int TEXT_HEIGHT = 7;

    public record BarInfo(Component name, float health, float maxHealth, String levelText, int levelColor, int trailKey) {}

    private HealthBars() {}

    public static List<Placed> collect(float partialTick, Vec3 camera) {
        Minecraft mc = Minecraft.getInstance();
        List<Placed> bars = new ArrayList<>();
        if (mc.level == null) return bars;

        Theme theme = Themes.current();
        float scale = BASE_SCALE * Config.SCALE.get().floatValue();
        float heightAbove = Config.HEIGHT_ABOVE_MOB.get().floatValue();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !BarVisibility.shouldShowBar(living)) continue;

            Vec3 pos = living.getPosition(partialTick);
            double x = pos.x;
            double y = pos.y + living.getBbHeight() + heightAbove;
            double z = pos.z;
            double dx = x - camera.x;
            double dy = y - camera.y;
            double dz = z - camera.z;
            bars.add(new Placed(x, y, z, 0.0F, scale, layout(infoFor(living), theme, mc.font), dx * dx + dy * dy + dz * dz));
        }
        bars.sort((a, b) -> Double.compare(b.distanceSq(), a.distanceSq()));
        return bars;
    }

    private static BarInfo infoFor(LivingEntity entity) {
        boolean weShowLevels = Config.SHOW_LEVEL.get() && DynamicDifficultyCompat.isAvailable();
        int level = weShowLevels ? DynamicDifficultyCompat.getLevel(entity) : -1;
        String levelText = level >= 0 ? formatLevel(level) : null;
        int levelColor = Config.argb(Config.LEVEL_CUSTOM_COLOR);
        if (levelText != null && Config.LEVEL_COLOR_MODE.get() == Config.LevelColor.DIFFICULTY) {
            levelColor = DynamicDifficultyCompat.getLevelColor(Minecraft.getInstance().player, entity, levelColor);
        }
        Component name = NameTags.nameFor(entity, weShowLevels);
        return new BarInfo(name, Math.max(0.0F, entity.getHealth()), Math.max(1.0F, entity.getMaxHealth()),
                levelText, levelColor, entity.getId());
    }

    public static DrawList layout(BarInfo info, Theme theme, Font font) {
        DrawList out = new DrawList();
        boolean shadow = theme.textShadow();

        int barWidth = Config.BAR_WIDTH.get();
        int barHeight = theme.height();
        float defaultLeft = -barWidth / 2.0F;
        float defaultRight = barWidth / 2.0F;
        float defaultTop = -barHeight;
        float insideBarTextY = defaultTop + (barHeight - TEXT_HEIGHT) / 2.0F;
        float nameLineY = defaultTop - font.lineHeight - 1;

        float health = info.health();
        float maxHealth = info.maxHealth();
        if (Config.SHOW_BAR.get()) {
            float fraction = Numbers.clamp(health / maxHealth, 0.0F, 1.0F);
            float trailFraction = Config.DAMAGE_TRAIL.get() ? DamageTrail.update(info.trailKey(), fraction) : fraction;
            float left = defaultLeft + Config.BAR_X.get();
            float top = defaultTop + Config.BAR_Y.get();
            BarShape.emit(out, theme, left, top, barWidth, fraction, trailFraction);
        }

        if (Config.SHOW_HEALTH_TEXT.get()) {
            String text = formatHealthText(health, maxHealth);
            out.scaledText(Component.literal(text), -font.width(text) / 2.0F, -TEXT_HEIGHT / 2.0F, theme.healthColor(),
                    Config.HEALTH_TEXT_X.get(), defaultTop + barHeight / 2.0F + Config.HEALTH_TEXT_Y.get(),
                    Config.HEALTH_TEXT_SCALE.get().floatValue(), shadow);
        }

        String levelText = info.levelText();
        int levelWidth = levelText != null ? font.width(levelText) : 0;
        Component name = info.name();
        boolean showName = Config.SHOW_NAME.get();
        int nameWidth = showName ? font.width(name) : 0;

        Config.LevelPosition levelPos = Config.LEVEL_POSITION.get();
        boolean levelInline = levelPos == Config.LevelPosition.AFTER_NAME || levelPos == Config.LevelPosition.BEFORE_NAME;

        float nameX = -nameWidth / 2.0F;
        int nameOffsetX = Config.NAME_X.get();
        int nameOffsetY = Config.NAME_Y.get();

        if (levelText != null) {
            float levelX;
            float levelY;
            if (levelInline && showName) {
                int gap = font.width(" ");
                float start = -(nameWidth + gap + levelWidth) / 2.0F;
                if (levelPos == Config.LevelPosition.AFTER_NAME) {
                    nameX = start;
                    levelX = start + nameWidth + gap;
                } else {
                    levelX = start;
                    nameX = start + levelWidth + gap;
                }
                levelX += nameOffsetX;
                levelY = nameLineY + nameOffsetY;
            } else {
                switch (levelPos) {
                    case LEFT_OF_BAR -> { levelX = defaultLeft - 3 - levelWidth; levelY = insideBarTextY; }
                    case RIGHT_OF_BAR -> { levelX = defaultRight + 3; levelY = insideBarTextY; }
                    case INSIDE_BAR_LEFT -> { levelX = defaultLeft + 6; levelY = insideBarTextY; }
                    case INSIDE_BAR_RIGHT -> { levelX = defaultRight - 6 - levelWidth; levelY = insideBarTextY; }
                    case ABOVE_NAME -> { levelX = -levelWidth / 2.0F; levelY = nameLineY - font.lineHeight; }
                    default -> { levelX = -levelWidth / 2.0F; levelY = nameLineY; }
                }
            }
            out.text(Component.literal(levelText), levelX + Config.LEVEL_X.get(), levelY + Config.LEVEL_Y.get(), info.levelColor(), shadow);
        }

        if (showName) {
            out.text(name, nameX + nameOffsetX, nameLineY + nameOffsetY, theme.nameColor(), shadow);
        }
        return out;
    }

    private static String formatHealthText(float health, float maxHealth) {
        return switch (Config.HEALTH_FORMAT.get()) {
            case CURRENT_AND_MAX -> Numbers.format(health) + "/" + Numbers.format(maxHealth);
            case CURRENT -> Numbers.format(health);
            case PERCENT -> Math.round(health / maxHealth * 100.0F) + "%";
        };
    }

    public static String formatLevel(int level) {
        try {
            return String.format(Locale.ROOT, Config.LEVEL_FORMAT.get(), level);
        } catch (RuntimeException e) {
            return "Lv. " + level;
        }
    }
}
