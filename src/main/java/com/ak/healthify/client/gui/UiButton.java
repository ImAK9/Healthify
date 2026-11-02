package com.ak.healthify.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}

public class UiButton extends Button {
    private boolean tab;
    private boolean selected;
    public UiButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    //? if >=26.1 {
    /*@Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        draw(new Ui.Painter(graphics));
    }
    *///?} else {
    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        draw(new Ui.Painter(graphics));
    }
    //?}

    public UiButton asTab(boolean selected) {
        this.tab = true;
        this.selected = selected;
        return this;
    }

    private void draw(Ui.Painter painter) {
        if (tab) {
            Ui.tab(painter, Minecraft.getInstance().font, getX(), getY(), getWidth(), getHeight(), getMessage(), selected, isHoveredOrFocused());
        } else {
            Ui.button(painter, Minecraft.getInstance().font, getX(), getY(), getWidth(), getHeight(), getMessage(), this.active, isHoveredOrFocused());
        }
    }
}
