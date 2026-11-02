package com.ak.healthify.client.gui;

import com.ak.healthify.client.BarShape;
import com.ak.healthify.client.DrawList;
import com.ak.healthify.client.theme.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}

public final class Ui {
    public static final int OUTLINE = 0xFF0A0A0D;
    public static final int GOLD = 0xFFF2C14E;
    public static final int GOLD_DARK = 0xFFB8862B;
    public static final int GOLD_DEEP = 0xFF6E4C1C;
    public static final int BORDER = 0xFF3C3C46;
    public static final int BORDER_LIGHT = 0xFF5A5A66;
    public static final int PANEL = 0xC0101014;
    public static final int PANEL_SOLID = 0xD8141418;
    public static final int ROW = 0x10FFFFFF;
    public static final int HEADER = 0x30F2C14E;
    public static final int BUTTON = 0xFF24242B;
    public static final int BUTTON_HOVER = 0xFF33333D;
    public static final int BUTTON_OFF = 0xA01A1A20;
    public static final int FIELD = 0xFF0E0E12;
    public static final int TEXT = 0xFFF0F0F0;
    public static final int TEXT_DIM = 0xFF9C9CA8;
    public static final int TITLE = 0xFFF2C14E;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BAR_FILL = 0xFF8C6A26;
    public static final int BAR_LIGHT = 0xFFB8903C;
    public static final int BAR_SHADE = 0xFF5E4618;
    public static final int BAR_EMPTY = 0xFF16161B;

    private Ui() {}

    public static final class Painter {
        //? if >=26.1 {
        /*private final GuiGraphicsExtractor graphics;

        public Painter(GuiGraphicsExtractor graphics) {
            this.graphics = graphics;
        }

        public void fill(int x0, int y0, int x1, int y1, int color) {
            graphics.fill(x0, y0, x1, y1, color);
        }

        public void text(Font font, Component text, int x, int y, int color) {
            graphics.text(font, text, x, y, color, true);
        }
        *///?} else {
        private final GuiGraphics graphics;

        public Painter(GuiGraphics graphics) {
            this.graphics = graphics;
        }

        public void fill(int x0, int y0, int x1, int y1, int color) {
            graphics.fill(x0, y0, x1, y1, color);
        }

        public void text(Font font, Component text, int x, int y, int color) {
            graphics.drawString(font, text, x, y, color, true);
        }
        //?}

        public void centered(Font font, Component text, int cx, int y, int color) {
            text(font, text, cx - font.width(text) / 2, y, color);
        }
    }

    public static void frame(Painter p, int x0, int y0, int x1, int y1, int fill, boolean bright) {
        p.fill(x0, y0, x1, y1, bright ? GOLD : BORDER);
        p.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, fill);
    }

    public static void panel(Painter p, int x0, int y0, int x1, int y1) {
        p.fill(x0, y0, x1, y1, BORDER);
        p.fill(x0 + 1, y0 + 1, x1 - 1, y1 - 1, PANEL);
    }

    public static void button(Painter p, Font font, int x, int y, int w, int h, Component message, boolean active, boolean hovered) {
        int fill = !active ? BUTTON_OFF : hovered ? BUTTON_HOVER : BUTTON;
        p.fill(x, y, x + w, y + h, active && hovered ? GOLD : BORDER_LIGHT);
        p.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        p.centered(font, message, x + w / 2, y + (h - 8) / 2, active ? WHITE : TEXT_DIM);
    }

    public static void tab(Painter p, Font font, int x, int y, int w, int h, Component message, boolean selected, boolean hovered) {
        if (selected) {
            p.fill(x, y, x + w, y + h, BUTTON_HOVER);
            p.fill(x, y + h - 2, x + w, y + h, GOLD);
            p.centered(font, message, x + w / 2, y + (h - 8) / 2 - 1, TITLE);
        } else {
            p.fill(x, y, x + w, y + h, hovered ? BUTTON : BUTTON_OFF);
            if (hovered) p.fill(x, y + h - 1, x + w, y + h, BORDER_LIGHT);
            p.centered(font, message, x + w / 2, y + (h - 8) / 2 - 1, hovered ? WHITE : TEXT_DIM);
        }
    }

    public static void slider(Painter p, Font font, int x, int y, int w, int h, Component message, double value, boolean hovered) {
        p.fill(x, y, x + w, y + h, hovered ? GOLD : BORDER_LIGHT);
        int left = x + 1;
        int right = x + w - 1;
        int top = y + 1;
        int bottom = y + h - 1;
        int split = left + (int) Math.round(Math.max(0.0, Math.min(1.0, value)) * (right - left));
        p.fill(left, top, right, bottom, BAR_EMPTY);
        if (split > left) {
            p.fill(left, top, split, bottom, BAR_FILL);
            p.fill(left, top, split, top + 2, BAR_LIGHT);
            p.fill(left, bottom - 2, split, bottom, BAR_SHADE);
        }
        int handle = Math.max(left, Math.min(right - 4, split - 2));
        p.fill(handle, y, handle + 4, y + h, hovered ? GOLD : WHITE);
        p.centered(font, message, x + w / 2, y + (h - 8) / 2, WHITE);
    }

    public static void divider(Painter p, Theme theme, int cx, int y, int width) {
        DrawList bar = new DrawList();
        BarShape.emit(bar, theme, cx - width / 2.0F, y, width, 1.0F, 1.0F);
        for (DrawList.Quad q : bar.quads) {
            p.fill(Math.round(q.x0()), Math.round(q.y0()), Math.round(q.x1()), Math.round(q.y1()), q.color());
        }
    }
}
