package com.ak.healthify.client.gui;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}

public class UiSlider extends AbstractSliderButton {
    private final double min;
    private final double max;
    private final double step;
    private final DoubleConsumer apply;

    public UiSlider(int x, int y, int width, int height, double min, double max, double step, double current, DoubleConsumer apply) {
        super(x, y, width, height, Component.empty(), max > min ? (current - min) / (max - min) : 0.0);
        this.min = min;
        this.max = max;
        this.step = step;
        this.apply = apply;
        updateMessage();
    }

    public double current() {
        double raw = min + this.value * (max - min);
        double snapped = Math.round(raw / step) * step;
        return Math.max(min, Math.min(max, snapped));
    }

    @Override
    protected void updateMessage() {
        if (apply == null) return;
        setMessage(Component.literal(format(current())));
    }

    @Override
    protected void applyValue() {
        if (apply != null) apply.accept(current());
    }

    private String format(double v) {
        if (step >= 1.0) return Long.toString(Math.round(v));
        if (step >= 0.1) return String.format(Locale.ROOT, "%.1f", v);
        return String.format(Locale.ROOT, "%.2f", v);
    }

    //? if >=26.1 {
    /*@Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        draw(new Ui.Painter(graphics));
    }
    *///?} else {
    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        draw(new Ui.Painter(graphics));
    }
    //?}

    private void draw(Ui.Painter painter) {
        Ui.slider(painter, Minecraft.getInstance().font, getX(), getY(), getWidth(), getHeight(), getMessage(), this.value, isHoveredOrFocused());
    }
}
