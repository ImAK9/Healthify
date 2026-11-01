package com.ak.healthify.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public final class DrawList {
    public record Quad(float x0, float y0, float x1, float y1, int color,
                       ResourceLocation texture, float u0, float v0, float u1, float v1) {}

    public record Text(Component text, float x, float y, int color, float originX, float originY, float scale) {}

    public final List<Quad> quads = new ArrayList<>();
    public final List<Text> underlays = new ArrayList<>();
    public final List<Text> texts = new ArrayList<>();

    public void quad(float x0, float y0, float x1, float y1, int color) {
        quads.add(new Quad(x0, y0, x1, y1, color, null, 0.5F, 0.5F, 0.5F, 0.5F));
    }

    public void image(ResourceLocation texture, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1) {
        quads.add(new Quad(x0, y0, x1, y1, 0xFFFFFFFF, texture, u0, v0, u1, v1));
    }

    public void text(Component text, float x, float y, int color, boolean shadow) {
        scaledText(text, x, y, color, 0.0F, 0.0F, 1.0F, shadow);
    }

    public void scaledText(Component text, float x, float y, int color, float originX, float originY, float scale, boolean shadow) {
        if (shadow) {
            underlays.add(new Text(shadowOf(text), x + 1, y + 1, shadowColor(color), originX, originY, scale));
        }
        texts.add(new Text(text, x, y, color, originX, originY, scale));
    }

    public void underlay(Component text, float x, float y, int color, float originX, float originY, float scale) {
        underlays.add(new Text(text, x, y, color, originX, originY, scale));
    }

    private static Component shadowOf(Component text) {
        net.minecraft.network.chat.MutableComponent out = Component.empty();
        text.visit((style, string) -> {
            out.append(Component.literal(string).withStyle(style.withColor((net.minecraft.network.chat.TextColor) null)));
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        return out;
    }

    public static int shadowColor(int argb) {
        int r = ((argb >> 16) & 0xFF) / 4;
        int g = ((argb >> 8) & 0xFF) / 4;
        int b = (argb & 0xFF) / 4;
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
