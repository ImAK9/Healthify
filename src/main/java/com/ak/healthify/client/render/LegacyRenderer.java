//? if <26.1 {
package com.ak.healthify.client.render;

import com.ak.healthify.Config;
import com.ak.healthify.Healthify;
import com.ak.healthify.client.DamageNumbers;
import com.ak.healthify.client.DrawList;
import com.ak.healthify.client.HealthBars;
import com.ak.healthify.client.NameTags;
import com.ak.healthify.client.Placed;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public final class LegacyRenderer {
    private static final int LIGHT = 0xF000F0;
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Healthify.MODID, "textures/misc/white.png");
    private static final double MIN_DEPTH = 0.05;
    private static final int MAX_BATCHED_TYPES = 32;
    private static final MultiBufferSource.BufferSource BUFFERS = new Batched();

    private static final class Batched extends MultiBufferSource.BufferSource {
        Batched() {
            super(new ByteBufferBuilder(16384), new LinkedHashMap<>());
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            if (!fixedBuffers.containsKey(type) && fixedBuffers.size() < MAX_BATCHED_TYPES) {
                fixedBuffers.put(type, new ByteBufferBuilder(type.bufferSize()));
            }
            return super.getBuffer(type);
        }
    }

    private LegacyRenderer() {}

    public static void render(Vec3 camera, float partialTick, Matrix4f modelViewMatrix) {
        Minecraft mc = Minecraft.getInstance();
        List<Placed> bars = HealthBars.collect(partialTick, camera);
        NameTags.clear();
        List<Placed> numbers = DamageNumbers.collect(camera);
        if (bars.isEmpty() && numbers.isEmpty()) return;

        Quaternionf orientation = mc.getEntityRenderDispatcher().cameraOrientation();

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(modelViewMatrix);
        RenderSystem.applyModelViewMatrix();

        boolean depthTestWasOn = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        try {
            mc.renderBuffers().bufferSource().endBatch();

            boolean onTop = Config.DRAW_ON_TOP.get();
            if (onTop) {
                RenderSystem.disableDepthTest();
            }
            Function<ResourceLocation, RenderType> barType = onTop ? RenderType::textSeeThrough : RenderType::textPolygonOffset;
            Font.DisplayMode textMode = onTop ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET;
            drawAll(bars, camera, orientation, barType, textMode);

            RenderSystem.disableDepthTest();
            Function<ResourceLocation, RenderType> numberType = RenderType::textSeeThrough;
            drawAll(numbers, camera, orientation, numberType, Font.DisplayMode.SEE_THROUGH);
        } finally {
            if (depthTestWasOn) {
                RenderSystem.enableDepthTest();
            } else {
                RenderSystem.disableDepthTest();
            }
            modelView.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private record Prepared(DrawList draw, PoseStack.Pose pose, boolean onScreen,
                            float left, float top, float right, float bottom) {
        boolean overlaps(Prepared other) {
            if (!onScreen || !other.onScreen) return false;
            return left < other.right && other.left < right && top < other.bottom && other.top < bottom;
        }
    }

    private static void drawAll(List<Placed> placed, Vec3 camera, Quaternionf orientation,
                                Function<ResourceLocation, RenderType> barType, Font.DisplayMode textMode) {
        if (placed.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        Camera view = mc.gameRenderer.getMainCamera();
        Vector3f look = view.getLookVector();
        Vector3f up = view.getUpVector();
        Vector3f leftAxis = view.getLeftVector();
        List<Prepared> group = new ArrayList<>();
        for (Placed item : placed) {
            Prepared next = prepare(item, camera, orientation, font, look, up, leftAxis);
            for (Prepared earlier : group) {
                if (earlier.overlaps(next)) {
                    drawGroup(group, font, barType, textMode);
                    group.clear();
                    break;
                }
            }
            group.add(next);
        }
        drawGroup(group, font, barType, textMode);
    }

    private static Prepared prepare(Placed placed, Vec3 camera, Quaternionf orientation, Font font,
                                    Vector3f look, Vector3f up, Vector3f leftAxis) {
        PoseStack pose = new PoseStack();
        double dx = placed.x() - camera.x;
        double dy = placed.y() - camera.y;
        double dz = placed.z() - camera.z;
        pose.translate(dx, dy, dz);
        pose.mulPose(orientation);
        pose.translate(placed.driftX(), 0.0F, 0.0F);
        pose.scale(placed.scale(), -placed.scale(), placed.scale());
        DrawList draw = placed.draw();

        double depth = dx * look.x() + dy * look.y() + dz * look.z();
        if (depth < MIN_DEPTH) return new Prepared(draw, pose.last(), false, 0, 0, 0, 0);
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (DrawList.Quad q : draw.quads) {
            minX = Math.min(minX, Math.min(q.x0(), q.x1()));
            maxX = Math.max(maxX, Math.max(q.x0(), q.x1()));
            minY = Math.min(minY, Math.min(q.y0(), q.y1()));
            maxY = Math.max(maxY, Math.max(q.y0(), q.y1()));
        }
        for (DrawList.Text t : draw.texts) {
            float x0 = t.originX() + (t.x() - 1) * t.scale();
            float y0 = t.originY() + (t.y() - 1) * t.scale();
            minX = Math.min(minX, x0);
            minY = Math.min(minY, y0);
            maxX = Math.max(maxX, x0 + (font.width(t.text()) + 2) * t.scale());
            maxY = Math.max(maxY, y0 + (font.lineHeight + 2) * t.scale());
        }
        if (minX > maxX) return new Prepared(draw, pose.last(), false, 0, 0, 0, 0);
        double screenX = -(dx * leftAxis.x() + dy * leftAxis.y() + dz * leftAxis.z()) + placed.driftX();
        double screenY = -(dx * up.x() + dy * up.y() + dz * up.z());
        float k = (float) (1.0 / depth);
        float s = placed.scale();
        return new Prepared(draw, pose.last(), true,
                (float) ((screenX + minX * s) * k), (float) ((screenY + minY * s) * k),
                (float) ((screenX + maxX * s) * k), (float) ((screenY + maxY * s) * k));
    }

    private static void drawGroup(List<Prepared> group, Font font, Function<ResourceLocation, RenderType> barType, Font.DisplayMode textMode) {
        if (group.isEmpty()) return;
        Map<ResourceLocation, List<Prepared>> byTexture = new LinkedHashMap<>();
        for (Prepared item : group) {
            for (DrawList.Quad quad : item.draw().quads) {
                ResourceLocation texture = quad.texture() == null ? WHITE_TEXTURE : quad.texture();
                List<Prepared> users = byTexture.computeIfAbsent(texture, k -> new ArrayList<>());
                if (users.isEmpty() || users.get(users.size() - 1) != item) users.add(item);
            }
        }
        for (Map.Entry<ResourceLocation, List<Prepared>> entry : byTexture.entrySet()) {
            ResourceLocation texture = entry.getKey();
            VertexConsumer consumer = BUFFERS.getBuffer(barType.apply(texture));
            for (Prepared item : entry.getValue()) {
                for (DrawList.Quad quad : item.draw().quads) {
                    ResourceLocation own = quad.texture() == null ? WHITE_TEXTURE : quad.texture();
                    if (own.equals(texture)) quad(consumer, item.pose(), quad);
                }
            }
        }
        BUFFERS.endBatch();
        for (Prepared item : group) {
            Matrix4f matrix = item.pose().pose();
            for (DrawList.Text text : item.draw().underlays) {
                text(font, text, matrix, textMode);
            }
        }
        BUFFERS.endBatch();
        for (Prepared item : group) {
            Matrix4f matrix = item.pose().pose();
            for (DrawList.Text text : item.draw().texts) {
                text(font, text, matrix, textMode);
            }
        }
        BUFFERS.endBatch();
    }

    private static void text(Font font, DrawList.Text text, Matrix4f matrix, Font.DisplayMode mode) {
        Matrix4f m = matrix;
        if (text.scale() != 1.0F || text.originX() != 0.0F || text.originY() != 0.0F) {
            m = new Matrix4f(matrix).translate(text.originX(), text.originY(), 0.0F).scale(text.scale(), text.scale(), 1.0F);
        }
        font.drawInBatch(text.text(), text.x(), text.y(), text.color(), false, m, BUFFERS, mode, 0, LIGHT);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, DrawList.Quad q) {
        vertex(vc, pose, q.x0(), q.y1(), q.color(), q.u0(), q.v1());
        vertex(vc, pose, q.x1(), q.y1(), q.color(), q.u1(), q.v1());
        vertex(vc, pose, q.x1(), q.y0(), q.color(), q.u1(), q.v0());
        vertex(vc, pose, q.x0(), q.y0(), q.color(), q.u0(), q.v0());

        vertex(vc, pose, q.x0(), q.y0(), q.color(), q.u0(), q.v0());
        vertex(vc, pose, q.x1(), q.y0(), q.color(), q.u1(), q.v0());
        vertex(vc, pose, q.x1(), q.y1(), q.color(), q.u1(), q.v1());
        vertex(vc, pose, q.x0(), q.y1(), q.color(), q.u0(), q.v1());
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, int color, float u, float v) {
        vc.addVertex(pose, x, y, 0.0F).setColor(color).setUv(u, v).setLight(LIGHT);
    }
}
//?}
