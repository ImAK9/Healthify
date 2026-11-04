package com.ak.healthify.client;

import com.ak.healthify.Config;
import com.ak.healthify.client.compat.ChampionsCompat;
import com.ak.healthify.client.compat.DungeonDifficultyCompat;
import com.ak.healthify.client.compat.DynamicDifficultyCompat;
import com.ak.healthify.client.compat.L2HostilityCompat;
import com.ak.healthify.client.theme.Theme;
import com.ak.healthify.client.theme.Themes;
import com.ak.healthify.config.ConfigSpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
//? if >=26.2 {
/*import net.minecraft.world.entity.EntityTypes;
*///?} else {
import net.minecraft.world.entity.EntityType;
//?}
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class HealthBars {
    private static final float BASE_SCALE = 0.025F;
    private static final int TEXT_HEIGHT = 7;

    public record BarInfo(Component name, float health, float maxHealth, String levelText, int levelColor, int trailKey,
                          Component prefix, Component header, Component subtitle, List<L2HostilityCompat.Trait> traits,
                          int stars, int starColor) {
        public BarInfo(Component name, float health, float maxHealth, String levelText, int levelColor, int trailKey) {
            this(name, health, maxHealth, levelText, levelColor, trailKey, null, null, null, List.of(), 0, 0);
        }
    }

    private static final float SUBTITLE_SCALE = 0.75F;

    private record Cached(int tick, int revision, Theme theme, BarInfo info, float trail, DrawList draw) {}

    private static Map<Integer, Cached> CACHE = new HashMap<>();

    private static DrawList cachedLayout(LivingEntity entity, Theme theme, Font font, int revision, Map<Integer, Cached> next) {
        Cached old = CACHE.get(entity.getId());
        boolean sameTick = old != null && old.tick() == entity.tickCount && old.revision() == revision;
        BarInfo info = sameTick ? old.info() : infoFor(entity);
        float trail = trailFor(info);
        DrawList draw;
        if (old != null && old.revision() == revision && old.theme() == theme && old.trail() == trail && old.info().equals(info)) {
            draw = old.draw();
        } else {
            draw = layout(info, theme, font, trail);
        }
        next.put(entity.getId(), new Cached(entity.tickCount, revision, theme, info, trail, draw));
        return draw;
    }
    private static final int HEADER_OUTLINE = 0xFF2A2A2A;

    private static final float HUSK_RENDER_SCALE = 1.0625F;

    private static float renderScale(LivingEntity entity) {
        //? if >=26.2 {
        /*return entity.getType() == EntityTypes.HUSK ? HUSK_RENDER_SCALE : 1.0F;
        *///?} else {
        return entity.getType() == EntityType.HUSK ? HUSK_RENDER_SCALE : 1.0F;
        //?}
    }

    private HealthBars() {}

    public static List<Placed> collect(float partialTick, Vec3 camera) {
        Minecraft mc = Minecraft.getInstance();
        List<Placed> bars = new ArrayList<>();
        if (mc.level == null) return bars;

        Theme theme = Themes.current();
        float scale = BASE_SCALE * Config.SCALE.get().floatValue();
        float heightAbove = Config.HEIGHT_ABOVE_MOB.get().floatValue();
        double keepSizeBeyond = Config.KEEP_SIZE_BEYOND.get();
        int revision = ConfigSpec.revision();
        Map<Integer, Cached> next = new HashMap<>(Math.max(16, CACHE.size() * 2));
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || !BarVisibility.shouldShowBar(living)) continue;

            Vec3 pos = living.getPosition(partialTick);
            double x = pos.x;
            double y = pos.y + living.getBbHeight() * renderScale(living) + heightAbove;
            double z = pos.z;
            double dx = x - camera.x;
            double dy = y - camera.y;
            double dz = z - camera.z;
            double distanceSq = dx * dx + dy * dy + dz * dz;
            float barScale = scale;
            if (keepSizeBeyond > 0.0 && distanceSq > keepSizeBeyond * keepSizeBeyond) {
                barScale = (float) (scale * Math.sqrt(distanceSq) / keepSizeBeyond);
            }
            bars.add(new Placed(x, y, z, 0.0F, barScale, cachedLayout(living, theme, mc.font, revision, next), distanceSq));
        }
        CACHE = next;
        bars.sort((a, b) -> Double.compare(b.distanceSq(), a.distanceSq()));
        return bars;
    }

    private static BarInfo infoFor(LivingEntity entity) {
        boolean weShowLevels = Config.SHOW_LEVEL.get() && Config.SHOW_DD_LEVEL.get() && DynamicDifficultyCompat.isAvailable();
        int level = weShowLevels ? DynamicDifficultyCompat.getLevel(entity) : -1;
        String levelText = level >= 0 ? formatLevel(level) : null;
        int levelColor = Config.argb(Config.LEVEL_CUSTOM_COLOR);
        if (levelText != null && Config.LEVEL_COLOR_MODE.get() == Config.LevelColor.DIFFICULTY) {
            levelColor = DynamicDifficultyCompat.getLevelColor(Minecraft.getInstance().player, entity, levelColor);
        }
        L2HostilityCompat.Info l2 = L2HostilityCompat.forBar(entity);
        if (levelText == null && l2.level() > 0) {
            levelText = formatLevel(l2.level());
            levelColor = l2.levelColor();
        }
        Component name = NameTags.nameFor(entity, weShowLevels);
        com.ak.healthify.api.HealthifyApi.Decoration extra = com.ak.healthify.api.HealthifyApi.decorate(entity);
        if (extra.name() != null && name.equals(entity.getDisplayName())) name = extra.name();
        ChampionsCompat.Info champion = ChampionsCompat.forBar(entity);
        name = ChampionsCompat.decorateName(name, champion);
        Component header = extra.header() != null ? extra.header() : champion.affixes();
        Component subtitle = extra.subtitle() != null ? extra.subtitle() : DungeonDifficultyCompat.forBar(entity);
        return new BarInfo(name, Math.max(0.0F, entity.getHealth()), Math.max(1.0F, entity.getMaxHealth()),
                levelText, levelColor, entity.getId(), extra.prefix(), header, subtitle, l2.traits(),
                champion.stars(), champion.starColor());
    }

    public static DrawList layout(BarInfo info, Theme theme, Font font) {
        return layout(info, theme, font, trailFor(info));
    }

    private static float trailFor(BarInfo info) {
        float fraction = Numbers.clamp(info.health() / info.maxHealth(), 0.0F, 1.0F);
        if (!Config.SHOW_BAR.get() || !Config.DAMAGE_TRAIL.get()) return fraction;
        return DamageTrail.update(info.trailKey(), fraction);
    }

    private static DrawList layout(BarInfo info, Theme theme, Font font, float trailFraction) {
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
        boolean showName = Config.SHOW_NAME.get();
        Config.LevelPosition levelPos = Config.LEVEL_POSITION.get();
        boolean levelInline = levelPos == Config.LevelPosition.AFTER_NAME || levelPos == Config.LevelPosition.BEFORE_NAME;
        boolean prefixFirst = info.prefix() != null && levelText != null && showName && levelPos == Config.LevelPosition.BEFORE_NAME;
        Component name = info.prefix() == null || prefixFirst ? info.name() : info.prefix().copy().append(info.name());
        int nameWidth = showName ? font.width(name) : 0;
        int prefixWidth = prefixFirst ? font.width(info.prefix()) : 0;
        float prefixX = 0;

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
                    start -= prefixWidth / 2.0F;
                    prefixX = start;
                    levelX = start + prefixWidth;
                    nameX = levelX + levelWidth + gap;
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
            if (prefixFirst) out.text(info.prefix(), prefixX + nameOffsetX, nameLineY + nameOffsetY, theme.nameColor(), shadow);
            out.text(name, nameX + nameOffsetX, nameLineY + nameOffsetY, theme.nameColor(), shadow);
        }
        if (info.header() != null) {
            float headerBottom = nameLineY + nameOffsetY - 2 - (levelText != null && levelPos == Config.LevelPosition.ABOVE_NAME ? font.lineHeight : 0);
            out.outlinedText(info.header(), -font.width(info.header()) / 2.0F, -TEXT_HEIGHT - 1, theme.nameColor(),
                    nameOffsetX, headerBottom, SUBTITLE_SCALE, HEADER_OUTLINE);
        }
        if (!info.traits().isEmpty() || info.stars() > 0) {
            boolean levelAbove = levelText != null && levelPos == Config.LevelPosition.ABOVE_NAME;
            boolean levelBesideBar = levelPos == Config.LevelPosition.LEFT_OF_BAR || levelPos == Config.LevelPosition.RIGHT_OF_BAR
                    || levelPos == Config.LevelPosition.INSIDE_BAR_LEFT || levelPos == Config.LevelPosition.INSIDE_BAR_RIGHT;
            boolean nameLine = showName || (levelText != null && !levelBesideBar);
            float stackTop = nameLine ? nameLineY + nameOffsetY : defaultTop + Config.BAR_Y.get();
            if (levelAbove) stackTop = Math.min(stackTop, nameLineY - font.lineHeight + Config.LEVEL_Y.get());
            if (info.header() != null) {
                float headerBottom = nameLineY + nameOffsetY - 2 - (levelAbove ? font.lineHeight : 0);
                stackTop = Math.min(stackTop, headerBottom - (TEXT_HEIGHT + 1) * SUBTITLE_SCALE);
            }
            float rowBottom = stackTop - 3.0F;
            if (info.stars() > 0) {
                rowBottom = ChampionsCompat.drawStars(out, info.stars(), info.starColor(), font, Config.BAR_X.get(), rowBottom, shadow) - 2.0F;
            }
            L2HostilityCompat.drawTraits(out, info.traits(), font, Config.BAR_X.get(), rowBottom, theme, shadow);
        }
        float belowBar = 2.0F + Config.BAR_Y.get();
        if (info.subtitle() != null) {
            out.scaledText(info.subtitle(), -font.width(info.subtitle()) / 2.0F, 0.0F, theme.nameColor(),
                    Config.BAR_X.get(), belowBar, SUBTITLE_SCALE, shadow);
            float bottom = belowBar + TEXT_HEIGHT * SUBTITLE_SCALE;
            if (bottom > 0.0F) out.shiftY(-bottom);
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
