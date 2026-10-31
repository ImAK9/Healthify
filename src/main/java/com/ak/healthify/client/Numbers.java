package com.ak.healthify.client;

import java.util.Locale;

public final class Numbers {
    private Numbers() {}

    public static String format(float value) {
        float rounded = Math.round(value * 10.0F) / 10.0F;
        if (rounded == (int) rounded) {
            return Integer.toString((int) rounded);
        }
        return String.format(Locale.ROOT, "%.1f", rounded);
    }

    public static float clamp(float value, float min, float max) {
        return value < min ? min : Math.min(value, max);
    }

    public static int clamp(int value, int min, int max) {
        return value < min ? min : Math.min(value, max);
    }

    public static float lerp(float t, float a, float b) {
        return a + (b - a) * t;
    }
}
