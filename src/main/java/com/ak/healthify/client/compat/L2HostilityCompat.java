package com.ak.healthify.client.compat;

import com.ak.healthify.Config;
import com.ak.healthify.Healthify;
import com.ak.healthify.client.DrawList;
import com.ak.healthify.client.theme.Theme;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = Healthify.MODID, value = Dist.CLIENT)
public final class L2HostilityCompat {
    public static final String MOD_ID = "l2hostility";

    private static final int TEXT_HEIGHT = 7;
    private static final float NAMES_SCALE = 0.75F;
    private static final float RANK_SCALE = 0.6F;
    private static final int ICON_GAP = 2;

    public record Layer(float u0, float v0, float u1, float v1) {}

    public record Icon(ResourceLocation atlas, List<Layer> layers) {}

    public record Trait(Component name, int level, Icon icon) {}

    public record Info(int level, int levelColor, List<Trait> traits) {
        public static final Info NONE = new Info(0, 0, List.of());
    }

    private static boolean initialized;
    private static boolean available;
    private static Object holder;
    private static Method getExisting;
    private static Method getLevel;
    private static Field traitsField;
    private static Field fullDropField;
    private static Method asItem;
    private static Method getFullDesc;
    private static Method levelOf;
    private static Object clientConfig;
    private static ModConfigSpec.BooleanValue showLevelOverHead;
    private static ModConfigSpec.BooleanValue showTraitOverHead;
    private static ModConfigSpec.IntValue levelColor;
    private static ModConfigSpec.IntValue levelColorAbyss;
    private static boolean hidLevel;
    private static boolean hidTraits;

    private static final Map<Item, Optional<Icon>> ICONS = new HashMap<>();
    private static long iconsClearedAt;

    private L2HostilityCompat() {}

    public static boolean isAvailable() {
        init();
        return available;
    }

    public static Info forBar(LivingEntity entity) {
        boolean showLevel = Config.SHOW_LEVEL.get() && Config.SHOW_L2_LEVEL.get();
        boolean showTraits = Config.SHOW_L2_TRAITS.get();
        if ((!showLevel && !showTraits) || !isAvailable()) return Info.NONE;
        Info info = get(entity);
        if (info == null) return Info.NONE;
        int color = Config.LEVEL_COLOR_MODE.get() == Config.LevelColor.DIFFICULTY ? info.levelColor() : Config.argb(Config.LEVEL_CUSTOM_COLOR);
        return new Info(showLevel ? info.level() : 0, color, showTraits ? info.traits() : List.of());
    }

    public static void drawTraits(DrawList out, List<Trait> traits, Font font, float centerX, float bottom, Theme theme, boolean shadow) {
        if (traits.isEmpty()) return;
        boolean icons = Config.L2_TRAIT_ICONS.get() && traits.stream().allMatch(t -> t.icon() != null);
        if (!icons) {
            MutableComponent line = Component.empty();
            for (int i = 0; i < traits.size(); i++) {
                if (i > 0) line.append(Component.literal(" · "));
                line.append(traits.get(i).name());
            }
            out.scaledText(line, -font.width(line) / 2.0F, -font.lineHeight, theme.nameColor(), centerX, bottom, NAMES_SCALE, shadow);
            return;
        }
        int size = Config.L2_TRAIT_ICON_SIZE.get();
        float top = bottom - size;
        float width = traits.size() * size + (traits.size() - 1) * ICON_GAP;
        float x = centerX - width / 2.0F;
        for (Trait trait : traits) {
            Icon icon = trait.icon();
            for (Layer layer : icon.layers()) {
                out.image(icon.atlas(), x, top, x + size, top + size, layer.u0(), layer.v0(), layer.u1(), layer.v1());
            }
            if (trait.level() > 1) {
                String number = Integer.toString(trait.level());
                out.scaledText(Component.literal(number), -font.width(number), -TEXT_HEIGHT, 0xFFFFFFFF,
                        x + size + 0.5F, top + size + 0.5F, RANK_SCALE, true);
            }
            x += size + ICON_GAP;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level != null) updateOverheadText(Config.HIDE_L2_OVERHEAD.get());
    }

    private static Info get(LivingEntity entity) {
        if (!isAvailable()) return null;
        try {
            Optional<?> cap = (Optional<?>) getExisting.invoke(holder, entity);
            if (cap.isEmpty()) return null;
            Object data = cap.get();
            int level = (int) getLevel.invoke(data);
            Map<?, ?> traitMap = (Map<?, ?>) traitsField.get(data);
            if (level <= 0 && traitMap.isEmpty()) return null;

            List<Trait> traits = new ArrayList<>();
            for (Map.Entry<?, ?> entry : traitMap.entrySet()) {
                int traitLevel = entry.getValue() instanceof Integer i ? i : 1;
                if (traitLevel <= 0) continue;
                Component name = (Component) getFullDesc.invoke(entry.getKey(), traitLevel);
                Item item = (Item) asItem.invoke(entry.getKey());
                traits.add(new Trait(name, traitLevel, iconFor(item)));
            }
            boolean fullDrop = fullDropField != null && fullDropField.getBoolean(data);
            return new Info(level, colorFor(level, fullDrop), traits);
        } catch (Throwable t) {
            disable("reading a mob's level and traits", t);
            return null;
        }
    }

    private static int colorFor(int level, boolean fullDrop) {
        try {
            if (fullDrop && levelColorAbyss != null) return 0xFF000000 | (levelColorAbyss.get() & 0xFFFFFF);
        } catch (Throwable ignored) {
        }
        Integer relative = relativeColor(level);
        if (relative != null) return relative;
        try {
            if (levelColor != null) return 0xFF000000 | (levelColor.get() & 0xFFFFFF);
        } catch (Throwable ignored) {
        }
        return 0xFFFFFFFF;
    }

    private static Integer relativeColor(int level) {
        net.minecraft.client.player.LocalPlayer player = Minecraft.getInstance().player;
        if (levelOf == null || player == null) return null;
        try {
            int playerLevel = (int) levelOf.invoke(null, player);
            int margin = Math.max(5, Math.round(playerLevel * 0.15F));
            int diff = level - playerLevel;
            if (diff < -margin) return 0xFF55FF55;
            if (diff <= margin) return 0xFFFFFF55;
            if (diff <= margin * 3) return 0xFFFFAA00;
            return 0xFFFF5555;
        } catch (Throwable t) {
            levelOf = null;
            Healthify.LOGGER.warn("Healthify: couldn't read your L2 Hostility difficulty, using its level colour instead", t);
            return null;
        }
    }

    private static void updateOverheadText(boolean hide) {
        if (!isAvailable() || clientConfig == null) return;
        try {
            hidLevel = apply(showLevelOverHead, hide, hidLevel);
            hidTraits = apply(showTraitOverHead, hide, hidTraits);
        } catch (Throwable t) {
            Healthify.LOGGER.warn("Healthify: couldn't change L2 Hostility's overhead text setting", t);
            clientConfig = null;
        }
    }

    private static boolean apply(ModConfigSpec.BooleanValue value, boolean hide, boolean hidByUs) {
        if (value == null) return false;
        if (hide) {
            if (value.get()) {
                value.set(false);
                return true;
            }
            return hidByUs;
        }
        if (hidByUs) value.set(true);
        return false;
    }

    private static Icon iconFor(Item item) {
        long now = System.currentTimeMillis();
        if (now - iconsClearedAt > 10_000) {
            ICONS.clear();
            iconsClearedAt = now;
        }
        return ICONS.computeIfAbsent(item, L2HostilityCompat::lookUpIcon).orElse(null);
    }

    private static Optional<Icon> lookUpIcon(Item item) {
        //? if <26.1 {
        try {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.client.resources.model.BakedModel model = mc.getItemRenderer().getModel(new ItemStack(item), mc.level, null, 0);
            java.util.LinkedHashSet<net.minecraft.client.renderer.texture.TextureAtlasSprite> sprites = new java.util.LinkedHashSet<>();
            for (net.minecraft.client.renderer.block.model.BakedQuad quad : model.getQuads(null, null, net.minecraft.util.RandomSource.create(42L))) {
                sprites.add(quad.getSprite());
            }
            if (sprites.isEmpty()) sprites.add(model.getParticleIcon());
            ResourceLocation atlas = null;
            List<Layer> layers = new ArrayList<>();
            for (net.minecraft.client.renderer.texture.TextureAtlasSprite sprite : sprites) {
                if (atlas == null) atlas = sprite.atlasLocation();
                if (!sprite.atlasLocation().equals(atlas)) continue;
                layers.add(new Layer(sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1()));
            }
            return Optional.of(new Icon(atlas, List.copyOf(layers)));
        } catch (Throwable t) {
            return Optional.empty();
        }
        //?} else {
        /*return Optional.empty();
        *///?}
    }

    private static void init() {
        if (initialized) return;
        initialized = true;
        if (!ModList.get().isLoaded(MOD_ID)) return;
        try {
            Class<?> miscs = Class.forName("dev.xkmc.l2hostility.init.registrate.LHMiscs");
            Object mob = miscs.getField("MOB").get(null);
            Class<?> attVal = Class.forName("dev.xkmc.l2core.init.reg.simple.AttVal");
            holder = attVal.getMethod("type").invoke(mob);
            Class<?> attachmentHolder = Class.forName("net.neoforged.neoforge.attachment.IAttachmentHolder");
            getExisting = Class.forName("dev.xkmc.l2core.capability.attachment.GeneralCapabilityHolder")
                    .getMethod("getExisting", attachmentHolder);
            Class<?> cap = Class.forName("dev.xkmc.l2hostility.content.capability.mob.MobTraitCap");
            getLevel = cap.getMethod("getLevel");
            traitsField = cap.getField("traits");
            try {
                fullDropField = cap.getField("fullDrop");
            } catch (NoSuchFieldException e) {
                fullDropField = null;
            }
            Class<?> trait = Class.forName("dev.xkmc.l2hostility.content.traits.base.MobTrait");
            asItem = trait.getMethod("asItem");
            getFullDesc = trait.getMethod("getFullDesc", Integer.class);
            try {
                levelOf = Class.forName("dev.xkmc.l2hostility.content.logic.DifficultyLevel").getMethod("ofAny", LivingEntity.class);
            } catch (Throwable t) {
                levelOf = null;
            }
            available = true;
            Healthify.LOGGER.info("Healthify: L2 Hostility found, showing mob levels and traits in health bars");
        } catch (Throwable t) {
            Healthify.LOGGER.warn("Healthify: L2 Hostility is installed but couldn't be read; its levels and traits won't be shown", t);
            return;
        }
        try {
            clientConfig = Class.forName("dev.xkmc.l2hostility.init.data.LHConfig").getField("CLIENT").get(null);
            showLevelOverHead = (ModConfigSpec.BooleanValue) clientConfig.getClass().getField("showLevelOverHead").get(clientConfig);
            showTraitOverHead = (ModConfigSpec.BooleanValue) clientConfig.getClass().getField("showTraitOverHead").get(clientConfig);
            levelColor = (ModConfigSpec.IntValue) clientConfig.getClass().getField("overHeadLevelColor").get(clientConfig);
            levelColorAbyss = (ModConfigSpec.IntValue) clientConfig.getClass().getField("overHeadLevelColorAbyss").get(clientConfig);
        } catch (Throwable t) {
            clientConfig = null;
        }
    }

    private static void disable(String what, Throwable t) {
        available = false;
        Healthify.LOGGER.warn("Healthify: L2 Hostility error while {}; turning its support off", what, t);
    }
}
