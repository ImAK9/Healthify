package com.ak.healthify.client;

import com.ak.healthify.Config;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class BarVisibility {
    private static final Map<Integer, Long> LAST_LOOKED_AT = new HashMap<>();

    private static final Map<Integer, Boolean> LINE_OF_SIGHT = new HashMap<>();
    private static long lineOfSightTick = Long.MIN_VALUE;

    private static List<? extends String> blacklistSource;
    private static Set<ResourceLocation> blacklistIds = Set.of();
    private static Set<String> blacklistMods = Set.of();
    private static List<TagKey<EntityType<?>>> blacklistTags = List.of();

    private BarVisibility() {}

    public static boolean shouldShowBar(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || isHudHidden(mc)) return false;
        if (!(entity instanceof LivingEntity living)) return false;
        if (living == mc.getCameraEntity() || living instanceof ArmorStand) return false;
        if (living instanceof Player && !Config.SHOW_ON_PLAYERS.get()) return false;
        if (isBlacklisted(living)) return false;
        if (!(living instanceof Player) && !mobTypeAllowed(living)) return false;
        if (!living.isAlive()) return false;

        Entity camera = mc.getCameraEntity() != null ? mc.getCameraEntity() : player;
        double maxDistance = Config.MAX_DISTANCE.get();
        if (living.distanceToSqr(camera.getEyePosition()) > maxDistance * maxDistance) return false;

        boolean damaged = living.getHealth() < living.getMaxHealth();
        boolean lookedAt = LAST_LOOKED_AT.containsKey(living.getId());
        boolean modeAllows = switch (Config.SHOW_MODE.get()) {
            case ALWAYS -> true;
            case WHEN_DAMAGED -> damaged;
            case LOOKING_AT -> lookedAt;
            case LOOKING_AT_OR_DAMAGED -> lookedAt || damaged;
        };
        if (!modeAllows) return false;

        if (living.isInvisibleTo(player)) return false;
        return !Config.REQUIRE_LINE_OF_SIGHT.get() || canSee(player, living);
    }

    private static boolean isHudHidden(Minecraft mc) {
        //? if >=26.2 {
        /*return mc.gameRenderer.gameRenderState().guiRenderState.isHudHidden;
        *///?} else {
        return mc.options.hideGui;
        //?}
    }

    public static boolean mobTypeAllowed(LivingEntity entity) {
        if (entity instanceof Enemy) return Config.SHOW_ON_HOSTILE.get();
        if (entity instanceof NeutralMob) return Config.SHOW_ON_NEUTRAL.get();
        return Config.SHOW_ON_PASSIVE.get();
    }

    public static boolean isBlacklisted(Entity entity) {
        List<? extends String> list = Config.BLACKLIST.get();
        if (list.isEmpty()) return false;
        if (list != blacklistSource) readBlacklist(list);
        EntityType<?> type = entity.getType();
        ResourceLocation id = EntityType.getKey(type);
        if (blacklistIds.contains(id) || blacklistMods.contains(id.getNamespace())) return true;
        for (TagKey<EntityType<?>> tag : blacklistTags) {
            if (type.builtInRegistryHolder().is(tag)) return true;
        }
        return false;
    }

    private static void readBlacklist(List<? extends String> list) {
        Set<ResourceLocation> ids = new HashSet<>();
        Set<String> mods = new HashSet<>();
        List<TagKey<EntityType<?>>> tags = new ArrayList<>();
        for (String entry : list) {
            String s = entry.trim();
            if (s.startsWith("#")) {
                ResourceLocation tag = ResourceLocation.tryParse(s.substring(1));
                if (tag != null) tags.add(TagKey.create(Registries.ENTITY_TYPE, tag));
            } else if (s.endsWith(":*")) {
                mods.add(s.substring(0, s.length() - 2));
            } else {
                ResourceLocation id = ResourceLocation.tryParse(s);
                if (id != null) ids.add(id);
            }
        }
        blacklistIds = ids;
        blacklistMods = mods;
        blacklistTags = tags;
        blacklistSource = list;
    }

    private static boolean canSee(LocalPlayer player, LivingEntity entity) {
        long now = player.level().getGameTime();
        if (now != lineOfSightTick) {
            LINE_OF_SIGHT.clear();
            lineOfSightTick = now;
        }
        return LINE_OF_SIGHT.computeIfAbsent(entity.getId(), id -> player.hasLineOfSight(entity));
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        DamageTrail.cleanUp(mc.level == null);
        Config.ShowMode mode = Config.SHOW_MODE.get();
        boolean needsLooking = mode == Config.ShowMode.LOOKING_AT || mode == Config.ShowMode.LOOKING_AT_OR_DAMAGED;
        if (mc.level == null || mc.player == null || !needsLooking) {
            LAST_LOOKED_AT.clear();
            return;
        }

        long now = mc.level.getGameTime();
        Entity target = findLookedAtEntity(mc.player, Config.MAX_DISTANCE.get());
        if (target != null) {
            int id = target.getId();
            LAST_LOOKED_AT.keySet().removeIf(other -> other != id);
            LAST_LOOKED_AT.put(id, now);
        }
        long lingerTicks = Math.round(Config.LOOK_LINGER_SECONDS.get() * 20.0);
        LAST_LOOKED_AT.values().removeIf(time -> now - time > lingerTicks || time > now);
    }

    private static Entity findLookedAtEntity(Player player, double range) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.scale(range));

        BlockHitResult blockHit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        double distance = eye.distanceTo(end);

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(distance)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, searchBox,
                e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable(), distance * distance);
        return hit != null ? hit.getEntity() : null;
    }
}
