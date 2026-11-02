//? if >=26.1 {
/*package com.ak.healthify.client.render;

import com.ak.healthify.Config;
import com.ak.healthify.Healthify;
import com.ak.healthify.client.DamageNumbers;
import com.ak.healthify.client.DrawList;
import com.ak.healthify.client.HealthBars;
import com.ak.healthify.client.NameTags;
import com.ak.healthify.client.Placed;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class ModernRenderer {
    private static final int LIGHT = 0xF000F0;
    private static final int FIRST_ORDER = 100;
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Healthify.MODID, "textures/misc/white.png");

    private ModernRenderer() {}

    public static void submit(SubmitNodeCollector collector, PoseStack pose, CameraRenderState camera) {
        Minecraft mc = Minecraft.getInstance();
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        List<Placed> bars = HealthBars.collect(partialTick, camera.pos);
        NameTags.clear();
        List<Placed> numbers = DamageNumbers.collect(camera.pos);
        if (bars.isEmpty() && numbers.isEmpty()) return;

        boolean onTop = Config.DRAW_ON_TOP.get();
        Function<ResourceLocation, RenderType> barType = onTop ? BarPipeline::onTop : RenderTypes::textPolygonOffset;
        Font.DisplayMode textMode = onTop ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET;

        int order = FIRST_ORDER;
        for (Placed bar : bars) {
            order = submit(collector, pose, bar, camera, barType, textMode, order);
        }
        Function<ResourceLocation, RenderType> numberType = RenderTypes::textSeeThrough;
        for (Placed number : numbers) {
            order = submit(collector, pose, number, camera, numberType, Font.DisplayMode.SEE_THROUGH, order);
        }
    }

    private static int submit(SubmitNodeCollector collector, PoseStack pose, Placed placed, CameraRenderState camera,
                              Function<ResourceLocation, RenderType> barType, Font.DisplayMode textMode, int order) {
        DrawList draw = placed.draw();
        pose.pushPose();
        pose.translate(placed.x() - camera.pos.x, placed.y() - camera.pos.y, placed.z() - camera.pos.z);
        pose.mulPose(new Matrix4f().rotation(camera.orientation));
        pose.translate(placed.driftX(), 0.0F, 0.0F);
        pose.scale(placed.scale(), -placed.scale(), placed.scale());

        if (!draw.quads.isEmpty()) {
            Map<ResourceLocation, List<DrawList.Quad>> byTexture = new LinkedHashMap<>();
            for (DrawList.Quad quad : draw.quads) {
                ResourceLocation texture = quad.texture() == null ? WHITE_TEXTURE : quad.texture();
                byTexture.computeIfAbsent(texture, k -> new ArrayList<>()).add(quad);
            }
            OrderedSubmitNodeCollector geometry = collector.order(order);
            for (Map.Entry<ResourceLocation, List<DrawList.Quad>> group : byTexture.entrySet()) {
                List<DrawList.Quad> quads = group.getValue();
                geometry.submitCustomGeometry(pose, barType.apply(group.getKey()), (p, consumer) -> {
                    for (DrawList.Quad quad : quads) {
                        quad(consumer, p, quad);
                    }
                });
            }
        }
        OrderedSubmitNodeCollector underlays = collector.order(order + 1);
        for (DrawList.Text text : draw.underlays) {
            text(underlays, pose, text, textMode);
        }
        OrderedSubmitNodeCollector texts = collector.order(order + 2);
        for (DrawList.Text text : draw.texts) {
            text(texts, pose, text, textMode);
        }
        pose.popPose();
        return order + 3;
    }

    private static void text(OrderedSubmitNodeCollector collector, PoseStack pose, DrawList.Text text, Font.DisplayMode mode) {
        pose.pushPose();
        pose.translate(text.originX(), text.originY(), 0.0F);
        pose.scale(text.scale(), text.scale(), 1.0F);
        collector.submitText(pose, text.x(), text.y(), text.text().getVisualOrderText(), false, mode, LIGHT, text.color(), 0, 0);
        if (mode == Font.DisplayMode.SEE_THROUGH) {
            collector.submitText(pose, text.x(), text.y(), text.text().getVisualOrderText(), false, Font.DisplayMode.NORMAL, LIGHT, text.color(), 0, 0);
        }
        pose.popPose();
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
*///?}
