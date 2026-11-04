package com.ak.healthify.client.settings;

import com.ak.healthify.Config;
import com.ak.healthify.client.gui.Ui;
import com.ak.healthify.client.gui.UiButton;
import com.ak.healthify.client.gui.UiSlider;
import com.ak.healthify.client.theme.ThemeScreen;
import com.ak.healthify.client.theme.Themes;
import com.ak.healthify.config.ConfigSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else {
import net.minecraft.client.gui.GuiGraphics;
//?}

public class SettingsScreen extends Screen {
    private static final int ROW = 24;
    private static final int HEIGHT = 20;
    private static final int TAB_HEIGHT = 18;
    private static final int CONTROL = 150;
    private static final int MAX_WIDTH = 440;
    private static final int DESCRIPTION_LINES = 3;
    private static final int INVALID = 0xFFFF5555;
    private static final String MOD_SUPPORT = "modSupport";

    private record Page(Component title, List<ConfigSpec.ConfigValue<?>> values) {}

    private record Row(ConfigSpec.ConfigValue<?> value, Component label, AbstractWidget widget, int offset, boolean field) {
        boolean header() {
            return widget == null;
        }
    }

    private final Screen parent;
    private final List<Page> pages;
    private final Map<ConfigSpec.ConfigValue<?>, Object> undo = new LinkedHashMap<>();
    private final List<Row> rows = new ArrayList<>();
    private int page;
    private int scroll;
    private int left;
    private int right;
    private int listTop;
    private int listBottom;
    private int descriptionTop;

    public SettingsScreen(Screen parent) {
        super(Component.translatable("healthify.configuration.title"));
        this.parent = parent;
        this.pages = buildPages();
        snapshot();
    }

    private void snapshot() {
        undo.clear();
        if (pages.isEmpty()) return;
        for (ConfigSpec.ConfigValue<?> value : pages.get(Math.floorMod(page, pages.size())).values()) {
            undo.put(value, value.get());
        }
    }

    private static List<Page> buildPages() {
        Map<String, List<ConfigSpec.ConfigValue<?>>> grouped = new LinkedHashMap<>();
        List<ConfigSpec.ConfigValue<?>> modSupport = new ArrayList<>();
        for (ConfigSpec.ConfigValue<?> value : Config.SPEC.values()) {
            String key = value.path().isEmpty() ? "general" : value.path().get(0);
            if (key.equals(MOD_SUPPORT) || key.equals("level")) {
                modSupport.add(value);
            } else {
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            }
        }
        List<Page> out = new ArrayList<>();
        for (Map.Entry<String, List<ConfigSpec.ConfigValue<?>>> group : grouped.entrySet()) {
            out.add(new Page(Component.translatable("healthify.configuration." + group.getKey()), group.getValue()));
        }
        if (!modSupport.isEmpty()) out.add(new Page(Component.translatable("healthify.configuration." + MOD_SUPPORT), modSupport));
        return out;
    }

    @Override
    protected void init() {
        rows.clear();
        if (pages.isEmpty()) return;
        page = Math.floorMod(page, pages.size());
        int width = Math.min(MAX_WIDTH, this.width - 20);
        left = (this.width - width) / 2;
        right = left + width;

        int tabY = 24;
        int x = left;
        for (int i = 0; i < pages.size(); i++) {
            Component title = pages.get(i).title();
            int tabWidth = this.font.width(title) + 14;
            if (x + tabWidth > right && x > left) {
                x = left;
                tabY += TAB_HEIGHT + 2;
            }
            int index = i;
            addRenderableWidget(new UiButton(x, tabY, tabWidth, TAB_HEIGHT, title, b -> select(index)).asTab(i == page));
            x += tabWidth + 2;
        }

        listTop = tabY + TAB_HEIGHT + 12;
        int buttonsY = this.height - 26;
        int descriptionHeight = DESCRIPTION_LINES * 10 + 18;
        descriptionTop = buttonsY - 6 - descriptionHeight;
        listBottom = descriptionTop - 10;

        int controlX = right - 8 - CONTROL;
        int offset = 0;
        List<String> lastPath = null;
        List<ConfigSpec.ConfigValue<?>> values = pages.get(page).values();
        boolean grouped = values.stream().map(ConfigSpec.ConfigValue::path).distinct().count() > 1;
        for (ConfigSpec.ConfigValue<?> value : values) {
            List<String> path = value.path();
            if (grouped && !path.isEmpty() && !path.equals(lastPath)) {
                rows.add(new Row(null, Component.translatable("healthify.configuration." + path.get(path.size() - 1)), null, offset, false));
                offset += ROW;
            }
            lastPath = path;
            AbstractWidget widget = createWidget(value, controlX, CONTROL);
            if (widget == null) continue;
            addRenderableWidget(widget);
            rows.add(new Row(value, Component.translatable(value.translationKey()), widget, offset, widget instanceof EditBox));
            offset += ROW;
        }

        int cx = this.width / 2;
        UiButton undoButton = new UiButton(cx - 155, buttonsY, 100, HEIGHT, Component.translatable("healthify.settings.undo"), b -> undoChanges());
        undoButton.setTooltip(Tooltip.create(Component.translatable("healthify.settings.undo.tooltip")));
        addRenderableWidget(undoButton);
        UiButton resetButton = new UiButton(cx - 50, buttonsY, 100, HEIGHT, Component.translatable("healthify.settings.reset"), b -> resetToDefaults());
        resetButton.setTooltip(Tooltip.create(Component.translatable("healthify.settings.reset.tooltip")));
        addRenderableWidget(resetButton);
        addRenderableWidget(new UiButton(cx + 55, buttonsY, 100, HEIGHT, CommonComponents.GUI_DONE, b -> onClose()));

        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        layoutRows();
    }

    private void select(int index) {
        if (index == page) return;
        page = index;
        scroll = 0;
        snapshot();
        rebuildWidgets();
    }

    private int maxScroll() {
        return Math.max(0, rows.size() * ROW - (listBottom - listTop));
    }

    private void layoutRows() {
        for (Row row : rows) {
            if (row.header()) continue;
            int y = rowY(row);
            row.widget().setY(row.field() ? y + 6 : y);
            row.widget().visible = visible(row);
        }
    }

    private int rowY(Row row) {
        return listTop + row.offset() - scroll;
    }

    private boolean visible(Row row) {
        int y = rowY(row);
        return y >= listTop && y + HEIGHT <= listBottom;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int max = maxScroll();
        if (max > 0 && mouseY >= listTop && mouseY <= listBottom) {
            scroll = Math.max(0, Math.min(max, scroll - (int) Math.round(scrollY * ROW)));
            layoutRows();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @SuppressWarnings("unchecked")
    private void undoChanges() {
        for (Map.Entry<ConfigSpec.ConfigValue<?>, Object> entry : undo.entrySet()) {
            ((ConfigSpec.ConfigValue<Object>) entry.getKey()).set(entry.getValue());
        }
        Config.SPEC.save();
        rebuildWidgets();
    }

    @SuppressWarnings("unchecked")
    private void resetToDefaults() {
        for (ConfigSpec.ConfigValue<?> value : pages.get(page).values()) {
            ConfigSpec.ConfigValue<Object> v = (ConfigSpec.ConfigValue<Object>) value;
            v.set(v.getDefault());
        }
        Config.SPEC.save();
        rebuildWidgets();
    }

    private AbstractWidget createWidget(ConfigSpec.ConfigValue<?> value, int x, int w) {
        Component label = Component.translatable(value.translationKey());
        if (value == Config.THEME) {
            return new UiButton(x, 0, w, HEIGHT, Component.literal(Themes.current().name()), b -> open(new ThemeScreen(this)));
        }
        if (value instanceof ConfigSpec.BooleanValue bool) {
            return new UiButton(x, 0, w, HEIGHT, onOff(bool.get()), b -> {
                bool.set(!bool.get());
                bool.save();
                b.setMessage(onOff(bool.get()));
            });
        }
        if (value instanceof ConfigSpec.EnumValue<?> choice) {
            return enumButton(choice, x, w);
        }
        if (value instanceof ConfigSpec.IntValue number) {
            if ((long) number.max() - number.min() > 400) {
                return textField(label, x, w, Integer.toString(number.get()), text -> {
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
            return new UiSlider(x, 0, w, HEIGHT, number.min(), number.max(), 1.0, number.get(), v -> {
                number.set((int) Math.round(v));
                number.save();
            });
        }
        if (value instanceof ConfigSpec.DoubleValue number) {
            return new UiSlider(x, 0, w, HEIGHT, number.min(), number.max(), stepFor(number.max() - number.min()), number.get(), v -> {
                number.set(v);
                number.save();
            });
        }
        if (value instanceof ConfigSpec.StringValue text) {
            return textField(label, x, w, text.get(), input -> {
                if (!text.isValid(input)) return false;
                text.set(input);
                text.save();
                return true;
            });
        }
        if (value instanceof ConfigSpec.ListValue list) {
            return textField(label, x, w, String.join(", ", list.get()), input -> {
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

    private <E extends Enum<E>> UiButton enumButton(ConfigSpec.EnumValue<E> choice, int x, int w) {
        return new UiButton(x, 0, w, HEIGHT, Component.literal(pretty(choice.get())), b -> {
            E[] options = choice.options();
            choice.set(options[(choice.get().ordinal() + 1) % options.length]);
            choice.save();
            b.setMessage(Component.literal(pretty(choice.get())));
        });
    }

    private interface TextHandler {
        boolean accept(String text);
    }

    private EditBox textField(Component label, int x, int w, String current, TextHandler handler) {
        EditBox box = new EditBox(this.font, x + 5, 0, w - 10, 12, label);
        box.setBordered(false);
        box.setMaxLength(1024);
        box.setValue(current);
        box.setTextColor(Ui.TEXT);
        box.setResponder(text -> box.setTextColor(handler.accept(text) ? Ui.TEXT : INVALID));
        return box;
    }

    private static double stepFor(double span) {
        if (span <= 1.5) return 0.01;
        if (span <= 10.0) return 0.05;
        if (span <= 100.0) return 0.5;
        return 1.0;
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

    private Row hoveredRow(int mouseX, int mouseY) {
        for (Row row : rows) {
            if (row.header() || !visible(row)) continue;
            int y = rowY(row);
            if (mouseX >= left && mouseX < right && mouseY >= y - 2 && mouseY < y + HEIGHT + 2) return row;
        }
        for (Row row : rows) {
            if (!row.header() && visible(row) && row.widget().isFocused()) return row;
        }
        return null;
    }

    private List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isEmpty()) continue;
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (this.font.width(candidate) > width && line.length() > 0) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    @Override
    public void onClose() {
        open(parent);
    }

    private void open(Screen screen) {
        //? if >=26.2 {
        /*this.minecraft.gui.setScreen(screen);
        *///?} else {
        this.minecraft.setScreen(screen);
        //?}
    }

    private void drawBackdrop(Ui.Painter p) {
        if (pages.isEmpty()) return;
        Ui.panel(p, left, listTop - 6, right, listBottom + 4);
        int index = 0;
        for (Row row : rows) {
            if (!visible(row)) continue;
            int y = rowY(row);
            if (row.header()) {
                p.fill(left + 4, y - 2, right - 4, y + HEIGHT + 2, Ui.HEADER);
                p.fill(left + 4, y + HEIGHT + 1, right - 4, y + HEIGHT + 2, Ui.GOLD_DARK);
                index = 0;
                continue;
            }
            if (index % 2 == 0) p.fill(left + 4, y - 2, right - 4, y + HEIGHT + 2, Ui.ROW);
            index++;
        }
        for (Row row : rows) {
            if (row.header() || !row.field() || !row.widget().visible) continue;
            AbstractWidget w = row.widget();
            Ui.frame(p, w.getX() - 5, w.getY() - 6, w.getX() + w.getWidth() + 5, w.getY() + HEIGHT - 6, Ui.FIELD, w.isFocused());
        }
        Ui.frame(p, left, descriptionTop, right, descriptionTop + DESCRIPTION_LINES * 10 + 18, Ui.PANEL_SOLID, false);
    }

    private void drawContents(Ui.Painter p, int mouseX, int mouseY) {
        p.centered(this.font, this.title, this.width / 2, 9, Ui.TITLE);
        for (Row row : rows) {
            if (!visible(row)) continue;
            p.text(this.font, row.label(), left + 10, rowY(row) + (HEIGHT - 8) / 2, row.header() ? Ui.TITLE : Ui.TEXT);
        }
        int textX = left + 8;
        int y = descriptionTop + 6;
        Row hovered = hoveredRow(mouseX, mouseY);
        if (hovered == null) {
            p.text(this.font, Component.translatable("healthify.settings.hint"), textX, y, Ui.TEXT_DIM);
            return;
        }
        p.text(this.font, hovered.label(), textX, y, Ui.TITLE);
        List<String> parts = new ArrayList<>(hovered.value().comment());
        for (String extra : hovered.value().extraComment()) parts.add(extra.trim() + ".");
        List<String> lines = wrap(String.join(" ", parts), right - left - 16);
        for (int i = 0; i < Math.min(DESCRIPTION_LINES, lines.size()); i++) {
            p.text(this.font, Component.literal(lines.get(i)), textX, y + 11 + i * 10, Ui.TEXT);
        }
    }

    //? if >=26.1 {
    /*@Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawBackdrop(new Ui.Painter(graphics));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        drawContents(new Ui.Painter(graphics), mouseX, mouseY);
    }
    *///?} else {
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        drawBackdrop(new Ui.Painter(graphics));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        drawContents(new Ui.Painter(graphics), mouseX, mouseY);
    }
    //?}
}
