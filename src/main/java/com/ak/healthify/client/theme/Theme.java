package com.ak.healthify.client.theme;

import java.util.List;

public record Theme(
        String id,
        String name,
        String author,
        int height,
        int[] endInsets,
        boolean shading,
        int fill,
        int empty,
        int frame,
        int outline,
        int inner,
        int damageTrail,
        Integer lowHealthFill,
        float lowHealth,
        int nameColor,
        int healthColor,
        boolean textShadow,
        int damageColor,
        int bigHitColor,
        int killColor,
        int healColor,
        Integer numberOutline,
        List<String> bigHitWords,
        List<String> killWords) {
    public static final List<String> DEFAULT_BIG_HIT_WORDS = List.of("Crit!", "Boom!", "Smash!", "Pow!");
    public static final List<String> DEFAULT_KILL_WORDS = List.of("Slain!", "K.O.!", "Finished!");

    public static final int MIN_HEIGHT = 7;
    public static final int MAX_HEIGHT = 20;

    public int fillFor(float fraction) {
        return lowHealthFill != null && fraction <= lowHealth ? lowHealthFill : fill;
    }

    public static int[] ends(String style, int height) {
        int[] insets = new int[height];
        switch (style) {
            case "square" -> { }
            case "round" -> {
                double r = height / 2.0;
                double c = (height - 1) / 2.0;
                for (int y = 0; y < height; y++) {
                    double dy = Math.abs(y - c);
                    insets[y] = (int) Math.max(0, Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy))));
                }
            }
            case "pointed" -> {
                int k = height / 2 - 1;
                for (int y = 0; y < height; y++) {
                    insets[y] = Math.max(0, k - Math.min(y, height - 1 - y));
                }
            }
            default -> throw new IllegalArgumentException("\"ends\" must be \"pointed\", \"round\" or \"square\"");
        }
        return insets;
    }
}
