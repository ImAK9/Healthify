package com.ak.healthify.client.settings;

import com.ak.healthify.Config;
import com.ak.healthify.client.theme.ThemeScreen;
import com.ak.healthify.client.theme.Themes;
import com.ak.healthify.config.ConfigSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}

public class SettingsScreen extends Screen {
    private static final int ROW = 24;
    private static final int TOP = 30;
    private static final int SIDEBAR = 110;
    private static final int GAP = 10;
    private static final int CONTENT = 220;
    private static final int HEIGHT = 20;
    private static final int VALID = 0xFFE0E0E0;
    private static final int INVALID = 0xFFFF5555;

    private record Page(Component title, List<ConfigSpec.ConfigValue<?>> values) {}

    private record Label(Component text, int x, int y) {}

    private final Screen parent;
    private final List<Page> pages;
    private final List<Label> labels = new ArrayList<>();
    private int page;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("healthify.settings.title"));
        this.parent = parent;
        this.pages = buildPages();
    }

    private static List<Page> buildPages() {
        Map<List<String>, List<ConfigSpec.ConfigValue<?>>> grouped = new LinkedHashMap<>();
        for (ConfigSpec.ConfigValue<?> value : Config.SPEC.values()) {
            grouped.computeIfAbsent(value.path(), k -> new ArrayList<>()).add(value);
        }
        List<Page> out = new ArrayList<>();
        for (Map.Entry<List<String>, List<ConfigSpec.ConfigValue<?>>> group : grouped.entrySet()) {
            List<String> path = group.getKey();
            String name = path.isEmpty() ? "general" : path.get(path.size() - 1);
            out.add(new Page(Component.translatable("healthify.configuration." + name), group.getValue()));
        }
        return out;
    }

    @Override
    protected void init() {
        labels.clear();
        if (pages.isEmpty()) return;
        page = Math.floorMod(page, pages.size());
        int contentWidth = Math.max(150, Math.min(CONTENT, this.width - SIDEBAR - GAP - 20));
        int left = (this.width - (SIDEBAR + GAP + contentWidth)) / 2;
        int contentLeft = left + SIDEBAR + GAP;
        int rowHeight = rowHeight();

        for (int i = 0; i < pages.size(); i++) {
            int index = i;
            Button tab = Button.builder(pages.get(i).title(), b -> select(index))
                    .bounds(left, TOP + i * rowHeight, SIDEBAR, HEIGHT).build();
            tab.active = i != page;
            addRenderableWidget(tab);
        }

        int y = TOP;
        for (ConfigSpec.ConfigValue<?> value : pages.get(page).values()) {
            AbstractWidget widget = createWidget(value, contentLeft, y, contentWidth);
            if (widget != null) {
                List<String> comment = value.comment();
                if (!comment.isEmpty()) widget.setTooltip(Tooltip.create(Component.literal(String.join(" ", comment))));
                addRenderableWidget(widget);
            }
            y += rowHeight;
        }

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, this.height - 26, 200, HEIGHT).build());
    }

    private int rowHeight() {
        int rows = pages.size();
        for (Page p : pages) rows = Math.max(rows, p.values().size());
        int available = this.height - TOP - 32;
        return Math.max(HEIGHT + 1, Math.min(ROW, available / Math.max(1, rows)));
    }

    private void select(int index) {
        page = index;
        rebuildWidgets();
    }

    private AbstractWidget createWidget(ConfigSpec.ConfigValue<?> value, int x, int y, int w) {
        Component label = Component.translatable(value.translationKey());
        if (value == Config.THEME) {
            return Button.builder(withValue(label, Component.literal(Themes.current().name())), b -> open(new ThemeScreen(this)))
                    .bounds(x, y, w, HEIGHT).build();
        }
        if (value instanceof ConfigSpec.BooleanValue bool) {
            return Button.builder(withValue(label, onOff(bool.get())), b -> {
                bool.set(!bool.get());
                bool.save();
                b.setMessage(withValue(label, onOff(bool.get())));
            }).bounds(x, y, w, HEIGHT).build();
        }
        if (value instanceof ConfigSpec.EnumValue<?> choice) {
            return enumButton(choice, label, x, y, w);
        }
        if (value instanceof ConfigSpec.IntValue number) {
            if ((long) number.max() - number.min() > 400) {
                return textField(label, x, y, w, Integer.toString(number.get()), text -> {
                    try {
                        int parsed = Integer.parseInt(text.trim());
                        if (parsed < number.min() || parsed > number.max()) return false;
                        number.set(parsed);
                        number.save();
                        return true;
                    } catch (NumberFormatException e) {
                        return false;
                    }
                });
            }
            return new ValueSlider(x, y, w, label, number.min(), number.max(), 1.0, number.get(), v -> {
                number.set((int) Math.round(v));
                number.save();
            });
        }
        if (value instanceof ConfigSpec.DoubleValue number) {
            return new ValueSlider(x, y, w, label, number.min(), number.max(), stepFor(number.max() - number.min()), number.get(), v -> {
                number.set(v);
                number.save();
            });
        }
        if (value instanceof ConfigSpec.StringValue text) {
            return textField(label, x, y, w, text.get(), input -> {
                if (!text.isValid(input)) return false;
                text.set(input);
                text.save();
                return true;
            });
        }
        if (value instanceof ConfigSpec.ListValue list) {
            return textField(label, x, y, w, String.join(", ", list.get()), input -> {
                List<String> entries = new ArrayList<>();
                for (String part : input.split(",")) {
                    String entry = part.trim();
                    if (entry.isEmpty()) continue;
                    if (!list.isValidElement(entry)) return false;
                    entries.add(entry);
                }
                list.set(entries);
                list.save();
                return true;
            });
        }
        return null;
    }

    private <E extends Enum<E>> Button enumButton(ConfigSpec.EnumValue<E> choice, Component label, int x, int y, int w) {
        return Button.builder(withValue(label, Component.literal(pretty(choice.get()))), b -> {
            E[] options = choice.options();
            choice.set(options[(choice.get().ordinal() + 1) % options.length]);
            choice.save();
            b.setMessage(withValue(label, Component.literal(pretty(choice.get()))));
        }).bounds(x, y, w, HEIGHT).build();
    }

    private interface TextHandler {
        boolean accept(String text);
    }

    private EditBox textField(Component label, int x, int y, int w, String current, TextHandler handler) {
        int labelWidth = Math.min(this.font.width(label) + 8, w / 2);
        labels.add(new Label(label, x, y + (HEIGHT - 8) / 2));
        EditBox box = new EditBox(this.font, x + labelWidth, y, w - labelWidth, HEIGHT, label);
        box.setMaxLength(1024);
        box.setValue(current);
        box.setResponder(text -> box.setTextColor(handler.accept(text) ? VALID : INVALID));
        return box;
    }

    private static double stepFor(double span) {
        if (span <= 1.5) return 0.01;
        if (span <= 10.0) return 0.05;
        if (span <= 100.0) return 0.5;
        return 1.0;
    }

    private static MutableComponent withValue(Component label, Component value) {
        return label.copy().append(": ").append(value);
    }

    private static Component onOff(boolean on) {
        return Component.translatable(on ? "options.on" : "options.off");
    }

    private static String pretty(Enum<?> value) {
        StringBuilder out = new StringBuilder();
        for (String word : value.name().toLowerCase(Locale.ROOT).split("_")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    private static final class ValueSlider extends AbstractSliderButton {
        private final Component label;
        private final double min;
        private final double max;
        private final double step;
        private final DoubleConsumer apply;

        ValueSlider(int x, int y, int w, Component label, double min, double max, double step, double current, DoubleConsumer apply) {
            super(x, y, w, HEIGHT, Component.empty(), max > min ? (current - min) / (max - min) : 0.0);
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.apply = apply;
            updateMessage();
        }

        private double current() {
            double raw = min + this.value * (max - min);
            double snapped = Math.round(raw / step) * step;
            return Math.max(min, Math.min(max, snapped));
        }

        @Override
        protected void updateMessage() {
            if (label == null) return;
            setMessage(withValue(label, Component.literal(format(current()))));
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

    private interface Canvas {
        void text(Component text, int x, int y, int color);

        void centered(Component text, int x, int y, int color);
    }

    private void drawContents(Canvas canvas) {
        int cx = this.width / 2;
        canvas.centered(this.title, cx, 11, 0xFFFFFFFF);
        for (Label label : labels) {
            canvas.text(label.text(), label.x(), label.y(), 0xFFFFFFFF);
        }
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        drawContents(new Canvas() {
            @Override
            public void text(Component text, int x, int y, int color) {
                graphics.text(font, text, x, y, color, true);
            }

            @Override
            public void centered(Component text, int x, int y, int color) {
                graphics.text(font, text, x - font.width(text) / 2, y, color, true);
            }
        });
    }
    *///?} else {
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawContents(new Canvas() {
            @Override
            public void text(Component text, int x, int y, int color) {
                graphics.drawString(font, text, x, y, color, true);
            }

            @Override
            public void centered(Component text, int x, int y, int color) {
                graphics.drawString(font, text, x - font.width(text) / 2, y, color, true);
            }
        });
    }
    //?}
}
