package com.ak.healthify.client.theme;

import com.ak.healthify.Config;
import com.ak.healthify.client.DrawList;
import com.ak.healthify.client.compat.DynamicDifficultyCompat;
import com.ak.healthify.client.HealthBars;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}

public class ThemeScreen extends Screen {
    private static final int PREVIEW_SCALE = 2;
    private static final int PREVIEW_TRAIL_KEY = -1;
    private static final float[] PREVIEW_HEALTH = {20.0F, 14.0F, 9.0F, 4.0F, 20.0F};

    private final ModContainer container;
    private final Screen parent;
    private List<Theme> themes;
    private int index;

    public ThemeScreen(ModContainer container, Screen parent) {
        super(Component.translatable("healthify.themes.title"));
        this.container = container;
        this.parent = parent;
    }

    @Override
    protected void init() {
        Themes.reload();
        themes = Themes.all();
        index = Math.max(0, themes.indexOf(Themes.current()));

        int cx = this.width / 2;
        int buttonsY = panelBottom() + 12;
        addRenderableWidget(Button.builder(Component.translatable("healthify.themes.previous"), b -> step(-1))
                .bounds(cx - 155, buttonsY, 100, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(cx - 50, buttonsY, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("healthify.themes.next"), b -> step(1))
                .bounds(cx + 55, buttonsY, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("healthify.themes.open_folder"), b -> openThemesFolder())
                .bounds(cx - 155, buttonsY + 24, 150, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("healthify.themes.all_settings"),
                        b -> open(new ConfigurationScreen(container, this)))
                .bounds(cx + 5, buttonsY + 24, 150, 20).build());
    }

    private void step(int direction) {
        themes = Themes.all();
        if (themes.isEmpty()) return;
        index = Math.floorMod(index + direction, themes.size());
        Themes.select(themes.get(index));
    }

    @Override
    public void onClose() {
        open(parent);
    }

    private void open(Screen screen) {
        //? if >=26.3 {
        /*this.minecraft.gui.setScreen(screen);
        *///?} else {
        this.minecraft.setScreen(screen);
        //?}
    }

    private static void openThemesFolder() {
        Path folder = Themes.folder();
        try {
            Files.createDirectories(folder);
        } catch (Exception ignored) {
        }
        //? if >=26.3 {
        /*com.mojang.blaze3d.Blaze3D.openPath(folder);
        *///?}
        //? if >=26.1 && <26.3 {
        /*net.minecraft.util.Util.getPlatform().openPath(folder);
        *///?}
        //? if <26.1 {
        net.minecraft.Util.getPlatform().openPath(folder);
        //?}
    }

    private int panelTop() {
        return this.height / 2 - 62;
    }

    private int panelBottom() {
        return panelTop() + 72;
    }

    private void drawContents(Canvas canvas) {
        int cx = this.width / 2;
        canvas.centered(this.title, cx, 15, 0xFFFFFFFF);
        if (themes == null || themes.isEmpty()) return;
        index = Math.min(index, themes.size() - 1);

        Theme theme = themes.get(index);
        canvas.centered(Component.literal(theme.name()), cx, panelTop() - 34, 0xFFFFE08A);
        if (!theme.author().isEmpty()) {
            canvas.centered(Component.translatable("healthify.themes.by", theme.author()), cx, panelTop() - 22, 0xFFAAAAAA);
        }
        canvas.centered(Component.translatable("healthify.themes.count", index + 1, themes.size()), cx, panelTop() - 10, 0xFF888888);

        int left = cx - 130;
        int right = cx + 130;
        canvas.fill(left - 1, panelTop() - 1, right + 1, panelBottom() + 1, 0xFF6B5A3A);
        canvas.fill(left, panelTop(), right, panelBottom(), 0xE0181410);

        DrawList preview = HealthBars.layout(previewInfo(), theme, this.font);
        canvas.push(cx, panelBottom() - 16, PREVIEW_SCALE);
        drawList(canvas, preview);
        canvas.pop();

        List<String> errors = Themes.errors();
        if (!errors.isEmpty()) {
            canvas.centered(Component.translatable("healthify.themes.errors", errors.size()), cx, this.height - 28, 0xFFFF7766);
        }
        canvas.centered(Component.translatable("healthify.themes.hint"), cx, this.height - 16, 0xFF777777);
    }

    private static HealthBars.BarInfo previewInfo() {
        int step = (int) ((System.currentTimeMillis() / 1500L) % PREVIEW_HEALTH.length);
        String level = null;
        int levelColor = Config.argb(Config.LEVEL_CUSTOM_COLOR);
        if (Config.SHOW_LEVEL.get() && Config.SHOW_DD_LEVEL.get() && DynamicDifficultyCompat.isAvailable()) {
            level = HealthBars.formatLevel(7);
            if (Config.LEVEL_COLOR_MODE.get() == Config.LevelColor.DIFFICULTY) {
                levelColor = 0xFFFFFF55;
            }
        }
        return new HealthBars.BarInfo(Component.translatable("entity.minecraft.zombie"), PREVIEW_HEALTH[step], 20.0F,
                level, levelColor, PREVIEW_TRAIL_KEY);
    }

    private static void drawList(Canvas canvas, DrawList list) {
        for (DrawList.Quad q : list.quads) {
            if (q.texture() != null) continue;
            canvas.fill(Math.round(q.x0()), Math.round(q.y0()), Math.round(q.x1()), Math.round(q.y1()), q.color());
        }
        for (DrawList.Text t : list.underlays) {
            drawText(canvas, t);
        }
        for (DrawList.Text t : list.texts) {
            drawText(canvas, t);
        }
    }

    private static void drawText(Canvas canvas, DrawList.Text t) {
        canvas.push(t.originX(), t.originY(), t.scale());
        canvas.text(t.text(), Math.round(t.x()), Math.round(t.y()), t.color());
        canvas.pop();
    }

    private interface Canvas {
        void fill(int x0, int y0, int x1, int y1, int color);

        void text(Component text, int x, int y, int color);

        void centered(Component text, int x, int y, int color);

        void push(float x, float y, float scale);

        void pop();
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        drawContents(new Canvas() {
            @Override
            public void fill(int x0, int y0, int x1, int y1, int color) {
                graphics.fill(x0, y0, x1, y1, color);
            }

            @Override
            public void text(Component text, int x, int y, int color) {
                graphics.text(font, text, x, y, color, false);
            }

            @Override
            public void centered(Component text, int x, int y, int color) {
                graphics.text(font, text, x - font.width(text) / 2, y, color, true);
            }

            @Override
            public void push(float x, float y, float scale) {
                graphics.pose().pushMatrix();
                graphics.pose().translate(x, y);
                graphics.pose().scale(scale, scale);
            }

            @Override
            public void pop() {
                graphics.pose().popMatrix();
            }
        });
    }
    *///?} else {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawContents(new Canvas() {
            @Override
            public void fill(int x0, int y0, int x1, int y1, int color) {
                graphics.fill(x0, y0, x1, y1, color);
            }

            @Override
            public void text(Component text, int x, int y, int color) {
                graphics.drawString(font, text, x, y, color, false);
            }

            @Override
            public void centered(Component text, int x, int y, int color) {
                graphics.drawString(font, text, x - font.width(text) / 2, y, color, true);
            }

            @Override
            public void push(float x, float y, float scale) {
                graphics.pose().pushPose();
                graphics.pose().translate(x, y, 0.0F);
                graphics.pose().scale(scale, scale, 1.0F);
            }

            @Override
            public void pop() {
                graphics.pose().popPose();
            }
        });
    }
    //?}
}
