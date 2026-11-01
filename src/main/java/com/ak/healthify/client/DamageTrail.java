package com.ak.healthify.client;

import java.util.HashMap;
import java.util.Map;

public final class DamageTrail {
    private static final long HOLD_MS = 450;
    private static final float DRAIN_PER_SECOND = 0.8F;

    private static final class State {
        float fraction;
        float lastHealth;
        long holdUntil;
        long lastUpdate;
        long lastSeen;
    }

    private static final Map<Integer, State> TRAILS = new HashMap<>();
    private static boolean hadWorld;

    private DamageTrail() {}

    public static float update(int entityId, float fraction) {
        long now = System.currentTimeMillis();
        State trail = TRAILS.get(entityId);
        if (trail == null) {
            trail = new State();
            trail.fraction = fraction;
            trail.lastHealth = fraction;
            trail.lastUpdate = now;
            TRAILS.put(entityId, trail);
        }

        if (fraction < trail.lastHealth) {
            trail.fraction = Math.max(trail.fraction, trail.lastHealth);
            trail.holdUntil = now + HOLD_MS;
        }
        if (fraction >= trail.fraction) {
            trail.fraction = fraction;
        } else if (now > trail.holdUntil) {
            float seconds = (now - trail.lastUpdate) / 1000.0F;
            trail.fraction = Math.max(fraction, trail.fraction - DRAIN_PER_SECOND * seconds);
        }

        trail.lastHealth = fraction;
        trail.lastUpdate = now;
        trail.lastSeen = now;
        return trail.fraction;
    }

    public static void cleanUp(boolean noWorld) {
        if (noWorld && hadWorld) {
            TRAILS.clear();
        }
        hadWorld = !noWorld;
        long now = System.currentTimeMillis();
        TRAILS.values().removeIf(trail -> now - trail.lastSeen > 5000);
    }
}
