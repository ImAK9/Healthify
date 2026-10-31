package com.ak.healthify.client;

import com.ak.healthify.client.theme.Theme;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BarShape {
    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private BarShape() {}

    private record Run(int y, int x0, int x1, int layer) {}

    private record Shape(int width, int[] insets, List<Run> runs, int interiorLeft, int interiorRight, int interiorTop, int interiorBottom) {}

    private static Shape cachedShape;

    private static Shape shapeFor(int width, int[] insets) {
        if (cachedShape == null || cachedShape.width() != width || !Arrays.equals(cachedShape.insets(), insets)) {
            cachedShape = buildShape(width, insets);
        }
        return cachedShape;
    }

    private static Shape buildShape(int width, int[] insets) {
        int height = insets.length;
        int[][] layer = new int[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean inside = x >= insets[y] && x < width - insets[y];
                layer[y][x] = inside ? -1 : 0;
            }
        }
        for (int ring = 1; ring <= 3; ring++) {
            List<int[]> hits = new ArrayList<>();
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if (layer[y][x] == -1 && touchesLayer(layer, width, x, y, ring - 1)) {
                        hits.add(new int[] {x, y});
                    }
                }
            }
            for (int[] p : hits) {
                layer[p[1]][p[0]] = ring;
            }
        }
        int interiorLeft = Integer.MAX_VALUE;
        int interiorRight = Integer.MIN_VALUE;
        int interiorTop = Integer.MAX_VALUE;
        int interiorBottom = Integer.MIN_VALUE;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (layer[y][x] == -1) {
                    layer[y][x] = 4;
                    interiorLeft = Math.min(interiorLeft, x);
                    interiorRight = Math.max(interiorRight, x + 1);
                    interiorTop = Math.min(interiorTop, y);
                    interiorBottom = Math.max(interiorBottom, y);
                }
            }
        }
        if (interiorLeft > interiorRight) {
            interiorLeft = interiorRight = width / 2;
            interiorTop = interiorBottom = height / 2;
        }
        List<Run> runs = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            int x = 0;
            while (x < width) {
                int l = layer[y][x];
                int start = x;
                while (x < width && layer[y][x] == l) {
                    x++;
                }
                if (l > 0) {
                    runs.add(new Run(y, start, x, l));
                }
            }
        }
        return new Shape(width, insets, runs, interiorLeft, interiorRight, interiorTop, interiorBottom);
    }

    private static boolean touchesLayer(int[][] layer, int width, int x, int y, int target) {
        int height = layer.length;
        for (int[] d : NEIGHBOURS) {
            int nx = x + d[0];
            int ny = y + d[1];
            int value = (nx < 0 || ny < 0 || nx >= width || ny >= height) ? 0 : layer[ny][nx];
            if (value == target) return true;
        }
        return false;
    }

    public static void emit(DrawList out, Theme theme, float left, float top, int width, float fraction, float trailFraction) {
        Shape s = shapeFor(width, theme.endInsets());
        int height = theme.height();
        boolean shaded = theme.shading();
        int outline = theme.outline();
        int frame = theme.frame();
        int inner = theme.inner();
        int fill = theme.fillFor(fraction);
        int empty = theme.empty();
        int trail = theme.damageTrail();

        int span = s.interiorRight() - s.interiorLeft();
        int fillEnd = s.interiorLeft() + Math.round(fraction * span);
        int trailEnd = Math.max(fillEnd, s.interiorLeft() + Math.round(trailFraction * span));
        int interiorRows = s.interiorBottom() - s.interiorTop() + 1;

        for (Run run : s.runs()) {
            float y0 = top + run.y();
            float y1 = y0 + 1;
            if (run.layer() == 1) {
                out.quad(left + run.x0(), y0, left + run.x1(), y1, outline);
            } else if (run.layer() == 2) {
                int color = shaded ? shade(frame, frameShade(run.y(), height)) : frame;
                out.quad(left + run.x0(), y0, left + run.x1(), y1, color);
            } else if (run.layer() == 3) {
                out.quad(left + run.x0(), y0, left + run.x1(), y1, inner);
            } else {
                int row = run.y() - s.interiorTop();
                int fillColor = shaded ? shade(fill, fillShade(row, interiorRows)) : fill;
                int trailColor = shaded ? shade(trail, fillShade(row, interiorRows)) : trail;
                int emptyColor = shaded ? shade(empty, emptyShade(row, interiorRows)) : empty;

                int a = run.x0();
                int b = Numbers.clamp(fillEnd, run.x0(), run.x1());
                int c = Numbers.clamp(trailEnd, run.x0(), run.x1());
                int d = run.x1();

                if (b > a) {
                    boolean edge = shaded && fraction < 1.0F && b - a > 1 && b == fillEnd;
                    int edgeStart = edge ? b - 1 : b;
                    out.quad(left + a, y0, left + edgeStart, y1, fillColor);
                    if (edge) {
                        out.quad(left + edgeStart, y0, left + b, y1, shade(fillColor, 0.3F));
                    }
                }
                if (c > b) {
                    out.quad(left + b, y0, left + c, y1, trailColor);
                }
                if (d > c) {
                    out.quad(left + c, y0, left + d, y1, emptyColor);
                }
            }
        }
    }

    private static float fillShade(int row, int rows) {
        if (rows <= 2) return row == 0 ? 0.3F : -0.2F;
        if (row == 0) return 0.45F;
        if (row == 1) return 0.18F;
        if (row == rows - 1) return -0.38F;
        if (row == rows - 2) return -0.18F;
        return 0.0F;
    }

    private static float emptyShade(int row, int rows) {
        if (row == 0) return -0.45F;
        if (row == 1 && rows > 3) return -0.2F;
        if (row == rows - 1) return 0.12F;
        return 0.0F;
    }

    private static float frameShade(int y, int height) {
        if (y <= 1) return 0.35F;
        if (y < height / 2) return 0.12F;
        if (y >= height - 2) return -0.4F;
        return -0.2F;
    }

    public static int shade(int argb, float amount) {
        int a = argb >>> 24;
        int r = (argb >> 16) & 0xFF;
        int g = (argb >> 8) & 0xFF;
        int b = argb & 0xFF;
        if (amount > 0) {
            r += Math.round((255 - r) * amount);
            g += Math.round((255 - g) * amount);
            b += Math.round((255 - b) * amount);
        } else if (amount < 0) {
            float k = 1.0F + amount;
            r = Math.round(r * k);
            g = Math.round(g * k);
            b = Math.round(b * k);
        }
        return (a << 24) | (Numbers.clamp(r, 0, 255) << 16) | (Numbers.clamp(g, 0, 255) << 8) | Numbers.clamp(b, 0, 255);
    }
}
