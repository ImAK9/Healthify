package com.ak.healthify.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.LivingEntity;

public final class HealthifyApi {
    private HealthifyApi() {}

    private static final List<Function<LivingEntity, Map<String, Object>>> DECORATORS = new CopyOnWriteArrayList<>();

    public static void addDecorator(Function<LivingEntity, Map<String, Object>> decorator) {
        DECORATORS.add(decorator);
    }

    public record Decoration(Component name, Component prefix, Component header, Component subtitle) {
        public static final Decoration NONE = new Decoration(null, null, null, null);
    }

    public static Decoration decorate(LivingEntity entity) {
        if (DECORATORS.isEmpty()) return Decoration.NONE;
        Component name = null;
        MutableComponent prefix = null, header = null, subtitle = null;
        for (Function<LivingEntity, Map<String, Object>> decorator : DECORATORS) {
            Map<String, Object> out;
            try {
                out = decorator.apply(entity);
            } catch (RuntimeException e) {
                continue;
            }
            if (out == null) continue;
            if (name == null && out.get("name") instanceof Component n) name = n;
            if (out.get("prefix") instanceof Component p) prefix = prefix == null ? p.copy() : prefix.append(p);
            if (out.get("header") instanceof Component h) {
                header = header == null ? h.copy() : header.append(Component.literal(" · ")).append(h);
            }
            if (out.get("subtitle") instanceof Component s) {
                subtitle = subtitle == null ? s.copy() : subtitle.append(Component.literal(" · ")).append(s);
            }
        }
        return name == null && prefix == null && header == null && subtitle == null ? Decoration.NONE
                : new Decoration(name, prefix, header, subtitle);
    }
}
