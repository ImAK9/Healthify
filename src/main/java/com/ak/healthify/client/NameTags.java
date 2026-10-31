package com.ak.healthify.client;

import com.ak.healthify.Config;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class NameTags {
    public enum Action {
        HIDE,
        KEEP,
        RESET
    }

    private static final Map<Integer, Component> CAPTURED = new HashMap<>();

    private NameTags() {}

    public static Action handle(Entity entity, Component content) {
        if (BarVisibility.shouldShowBar(entity)) {
            if (content != null) {
                CAPTURED.put(entity.getId(), content);
            }
            return Config.HIDE_VANILLA_NAME_TAGS.get() ? Action.HIDE : Action.KEEP;
        }
        if (entity instanceof LivingEntity && !(entity instanceof Player)) {
            return Action.RESET;
        }
        return Action.KEEP;
    }

    public static Component nameFor(LivingEntity entity, boolean usePlainName) {
        Component captured = CAPTURED.get(entity.getId());
        return (!usePlainName && captured != null) ? captured : entity.getDisplayName();
    }

    public static void clear() {
        CAPTURED.clear();
    }
}
