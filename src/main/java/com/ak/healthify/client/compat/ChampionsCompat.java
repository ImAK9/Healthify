package com.ak.healthify.client.compat;

import com.ak.healthify.Config;
import com.ak.healthify.Healthify;
import com.ak.healthify.client.DrawList;
import com.ak.healthify.platform.Platform;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class ChampionsCompat {
    public static final String MOD_ID = "champions";

    private static final int STAR_SIZE = 7;
    private static final int STAR_GAP = 1;
    private static final int MAX_STARS = 9;
    private static final int TEXT_HEIGHT = 7;
    private static final float COUNT_SCALE = 0.75F;
    private static final TextColor AFFIX_COLOR = TextColor.fromRgb(0xCFCFCF);

    public record Info(Component prefix, TextColor color, Component affixes, int stars, int starColor) {
        public static final Info NONE = new Info(null, null, null, 0, 0);
    }

    private record Raw(int tier, int color, Component rank, List<Component> affixes) {}

    private interface Backend {
        Raw read(LivingEntity entity) throws ReflectiveOperationException;

        ResourceLocation star();
    }

    private static boolean initialized;
    private static Backend backend;

    private ChampionsCompat() {}

    public static boolean isAvailable() {
        init();
        return backend != null;
    }

    public static Info forBar(LivingEntity entity) {
        Config.ChampionRank style = Config.CHAMPION_RANK.get();
        boolean showRank = style == Config.ChampionRank.NAME || style == Config.ChampionRank.BOTH;
        boolean showStars = style == Config.ChampionRank.STARS || style == Config.ChampionRank.BOTH;
        boolean colorName = Config.COLOR_CHAMPION_NAME.get();
        boolean showAffixes = Config.SHOW_CHAMPION_AFFIXES.get();
        if ((!showRank && !showStars && !colorName && !showAffixes) || !isAvailable()) return Info.NONE;
        Raw raw;
        try {
            raw = backend.read(entity);
        } catch (Throwable t) {
            disable(t);
            return Info.NONE;
        }
        if (raw == null) return Info.NONE;
        boolean champion = raw.tier() > 0;
        Component rank = champion && showRank && raw.rank() != null && !raw.rank().getString().isBlank() ? raw.rank() : null;
        TextColor tint = champion && colorName ? TextColor.fromRgb(raw.color() & 0xFFFFFF) : null;
        Component affixes = showAffixes ? join(raw.affixes()) : null;
        int stars = champion && showStars ? raw.tier() : 0;
        if (rank == null && tint == null && affixes == null && stars == 0) return Info.NONE;
        return new Info(rank, tint, affixes, stars, 0xFF000000 | (raw.color() & 0xFFFFFF));
    }

    public static boolean hideOwnHud() {
        return Config.HIDE_CHAMPIONS_HUD.get() && isAvailable();
    }

    public static Component decorateName(Component name, Info info) {
        if (info.prefix() == null && info.color() == null) return name;
        MutableComponent out = Component.empty();
        if (info.prefix() != null) out.append(info.prefix()).append(Component.literal(" "));
        out.append(name);
        if (info.color() != null) out.withStyle(style -> style.withColor(info.color()));
        return out;
    }

    public static float drawStars(DrawList out, int stars, int color, Font font, float centerX, float bottom, boolean shadow) {
        Backend current = backend;
        if (current == null) return bottom;
        ResourceLocation star = current.star();
        float top = bottom - STAR_SIZE;
        if (stars > MAX_STARS) {
            String count = "x" + stars;
            float countWidth = font.width(count) * COUNT_SCALE;
            float x = centerX - (STAR_SIZE + STAR_GAP + countWidth) / 2.0F;
            out.image(star, x, top, x + STAR_SIZE, top + STAR_SIZE, 0.0F, 0.0F, 1.0F, 1.0F, color);
            out.scaledText(Component.literal(count), 0.0F, -TEXT_HEIGHT / 2.0F, 0xFFFFFFFF,
                    x + STAR_SIZE + STAR_GAP, top + STAR_SIZE / 2.0F, COUNT_SCALE, shadow);
            return top;
        }
        float width = stars * STAR_SIZE + (stars - 1) * STAR_GAP;
        float x = centerX - width / 2.0F;
        for (int i = 0; i < stars; i++) {
            out.image(star, x, top, x + STAR_SIZE, top + STAR_SIZE, 0.0F, 0.0F, 1.0F, 1.0F, color);
            x += STAR_SIZE + STAR_GAP;
        }
        return top;
    }

    private static Component join(List<Component> parts) {
        if (parts == null || parts.isEmpty()) return null;
        MutableComponent line = Component.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) line.append(Component.literal(" · "));
            line.append(parts.get(i));
        }
        return line.withStyle(style -> style.withColor(AFFIX_COLOR));
    }

    private static void init() {
        if (initialized) return;
        initialized = true;
        if (!Platform.isModLoaded(MOD_ID)) return;
        Throwable failure = null;
        try {
            backend = new ApiBackend();
            return;
        } catch (Throwable t) {
            failure = t;
        }
        try {
            backend = new HelperBackend();
        } catch (Throwable t) {
            t.addSuppressed(failure);
            Healthify.LOGGER.warn("Champions is installed but Healthify couldn't hook into it", t);
        }
    }

    private static void disable(Throwable t) {
        if (backend == null) return;
        backend = null;
        Healthify.LOGGER.warn("Healthify turned off Champions support after an error", t);
    }

    private static final class ApiBackend implements Backend {
        private static final ResourceLocation STAR = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/staricon.png");

        private final Method get;
        private final Method getChampion;
        private final Method getAffixTypeId;
        private final Method tier;
        private final Method affixes;
        private final Method level;
        private final Method display;
        private final Method color;
        private final Method type;

        ApiBackend() throws ReflectiveOperationException {
            Class<?> api = Class.forName("top.theillusivec4.champions.api.ChampionsApi");
            Class<?> champion = Class.forName("top.theillusivec4.champions.api.champion.Champion");
            Class<?> championTier = Class.forName("top.theillusivec4.champions.api.champion.ChampionTier");
            Class<?> tierDisplay = Class.forName("top.theillusivec4.champions.api.champion.ChampionTier$TierDisplay");
            Class<?> affixInstance = Class.forName("top.theillusivec4.champions.api.affix.AffixInstance");
            Class<?> affixType = Class.forName("top.theillusivec4.champions.api.affix.AffixType");
            get = api.getMethod("get");
            getChampion = api.getMethod("getChampion", LivingEntity.class);
            getAffixTypeId = api.getMethod("getAffixTypeId", affixType);
            tier = champion.getMethod("tier");
            affixes = champion.getMethod("affixes");
            level = championTier.getMethod("level");
            display = championTier.getMethod("display");
            color = tierDisplay.getMethod("color");
            type = affixInstance.getMethod("type");
        }

        @Override
        public Raw read(LivingEntity entity) throws ReflectiveOperationException {
            Object api = get.invoke(null);
            if (api == null) return null;
            Optional<?> found = (Optional<?>) getChampion.invoke(api, entity);
            if (found == null || found.isEmpty()) return null;
            Object champion = found.get();
            Object championTier = tier.invoke(champion);
            int rankLevel = championTier == null ? 0 : (int) level.invoke(championTier);
            Object tierDisplay = championTier == null ? null : display.invoke(championTier);
            int rgb = tierDisplay == null ? 0xFFFFFF : (int) color.invoke(tierDisplay);
            Component rank = rankLevel > 0
                    ? Component.translatableWithFallback("rank.champions.title." + rankLevel, "Tier " + rankLevel) : null;
            List<Component> names = new ArrayList<>();
            Object list = affixes.invoke(champion);
            if (list instanceof Collection<?> instances) {
                for (Object instance : instances) {
                    Optional<?> id = (Optional<?>) getAffixTypeId.invoke(api, type.invoke(instance));
                    if (id != null && id.isPresent() && id.get() instanceof ResourceLocation key) {
                        names.add(Component.translatableWithFallback(
                                "affix." + key.getNamespace() + "." + key.getPath() + ".name", capitalize(key.getPath())));
                    }
                }
            }
            return new Raw(rankLevel, rgb, rank, names);
        }

        @Override
        public ResourceLocation star() {
            return STAR;
        }

        private static String capitalize(String path) {
            if (path.isEmpty()) return path;
            return path.substring(0, 1).toUpperCase(Locale.ROOT) + path.substring(1);
        }
    }

    private static final class HelperBackend implements Backend {
        private static final ResourceLocation STAR = ResourceLocation.fromNamespaceAndPath(MOD_ID, "textures/gui/star.png");

        private final Method getProperty;
        private final Method tier;
        private final Method color;
        private final Method prefix;
        private final Method getAffixes;
        private final Method entrySet;
        private final Method fullName;

        HelperBackend() throws ReflectiveOperationException {
            Class<?> helper = Class.forName("top.theillusivec4.champions.api.championmob.ChampionMobPropertyHelper");
            Class<?> property = Class.forName("top.theillusivec4.champions.api.championmob.ChampionMobProperty");
            Class<?> affixHelper = Class.forName("top.theillusivec4.champions.api.affix.AffixHelper");
            Class<?> entityAffixes = Class.forName("top.theillusivec4.champions.api.affix.EntityAffixes");
            Class<?> affix = Class.forName("top.theillusivec4.champions.api.affix.Affix");
            getProperty = helper.getMethod("get", Entity.class);
            tier = property.getMethod("tier");
            color = property.getMethod("color");
            prefix = property.getMethod("prefix");
            getAffixes = affixHelper.getMethod("get", Entity.class);
            entrySet = entityAffixes.getMethod("entrySet");
            Method found = null;
            for (Method method : affix.getMethods()) {
                if (method.getName().equals("getFullName") && method.getParameterCount() == 2
                        && method.getParameterTypes()[1] == int.class) {
                    found = method;
                    break;
                }
            }
            if (found == null) throw new NoSuchMethodException("Affix.getFullName");
            fullName = found;
        }

        @Override
        public Raw read(LivingEntity entity) throws ReflectiveOperationException {
            Object property = getProperty.invoke(null, entity);
            int rankTier = property == null ? 0 : (int) tier.invoke(property);
            int rgb = 0xFFFFFF;
            Component rank = null;
            if (rankTier > 0) {
                TextColor value = (TextColor) color.invoke(property);
                if (value != null) rgb = value.getValue();
                rank = (Component) prefix.invoke(property);
            }
            List<Component> names = new ArrayList<>();
            Object affixes = getAffixes.invoke(null, entity);
            Object entries = affixes == null ? null : entrySet.invoke(affixes);
            if (entries instanceof Collection<?> collection) {
                for (Object raw : collection) {
                    if (!(raw instanceof Map.Entry<?, ?> entry) || !(entry.getValue() instanceof Integer level)) continue;
                    Component part = (Component) fullName.invoke(null, entry.getKey(), level);
                    if (part != null) names.add(part);
                }
            }
            return new Raw(rankTier, rgb, rank, names);
        }

        @Override
        public ResourceLocation star() {
            return STAR;
        }
    }
}
