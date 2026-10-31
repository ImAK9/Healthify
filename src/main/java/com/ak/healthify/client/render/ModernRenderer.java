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
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.TriState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = Healthify.MODID, value = Dist.CLIENT)
public final class ModernRenderer {
    private static final int LIGHT = 0xF000F0;
    private static final int FIRST_ORDER = 100;
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Healthify.MODID, "textures/misc/white.png");

    private ModernRenderer() {}

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        BarPipeline.register(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onNameTag(RenderNameTagEvent.CanRender event) {
        switch (NameTags.handle(event.getEntity(), event.getContent())) {
            case HIDE -> event.setCanRender(TriState.FALSE);
            case RESET -> {
                if (event.getOriginalContent() != null) {
                    event.setContent(event.getOriginalContent());
                }
                event.setCanRender(TriState.DEFAULT);
            }
            case KEEP -> { }
        }
    }

    @SubscribeEvent
    public static void onSubmitCustomGeometry(SubmitCustomGeometryEvent event) {
        Minecraft mc = Minecraft.getInstance();
        CameraRenderState camera = event.getLevelRenderState().cameraRenderState;
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        List<Placed> bars = HealthBars.collect(partialTick, camera.pos);
        NameTags.clear();
        List<Placed> numbers = DamageNumbers.collect(camera.pos);
        if (bars.isEmpty() && numbers.isEmpty()) return;

        SubmitNodeCollector collector = event.getSubmitNodeCollector();
        PoseStack pose = event.getPoseStack();
        boolean onTop = Config.DRAW_ON_TOP.get();
        RenderType barType = onTop ? BarPipeline.onTop(WHITE_TEXTURE) : RenderTypes.textPolygonOffset(WHITE_TEXTURE);
        Font.DisplayMode textMode = onTop ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET;

        int order = FIRST_ORDER;
        for (Placed bar : bars) {
            order = submit(collector, pose, bar, camera, barType, textMode, order);
        }
        RenderType numberType = RenderTypes.textSeeThrough(WHITE_TEXTURE);
        for (Placed number : numbers) {
            order = submit(collector, pose, number, camera, numberType, Font.DisplayMode.SEE_THROUGH, order);
        }
    }

    private static int submit(SubmitNodeCollector collector, PoseStack pose, Placed placed, CameraRenderState camera,
                              RenderType barType, Font.DisplayMode textMode, int order) {
        DrawList draw = placed.draw();
        pose.pushPose();
        pose.translate(placed.x() - camera.pos.x, placed.y() - camera.pos.y, placed.z() - camera.pos.z);
        pose.mulPose(new Matrix4f().rotation(camera.orientation));
        pose.translate(placed.driftX(), 0.0F, 0.0F);
        pose.scale(placed.scale(), -placed.scale(), placed.scale());

        if (!draw.quads.isEmpty()) {
            collector.order(order).submitCustomGeometry(pose, barType, (p, consumer) -> {
                for (DrawList.Quad quad : draw.quads) {
                    quad(consumer, p, quad);
                }
            });
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
        pose.popPose();
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose pose, DrawList.Quad q) {
        vertex(vc, pose, q.x0(), q.y1(), q.color());
        vertex(vc, pose, q.x1(), q.y1(), q.color());
        vertex(vc, pose, q.x1(), q.y0(), q.color());
        vertex(vc, pose, q.x0(), q.y0(), q.color());

        vertex(vc, pose, q.x0(), q.y0(), q.color());
        vertex(vc, pose, q.x1(), q.y0(), q.color());
        vertex(vc, pose, q.x1(), q.y1(), q.color());
        vertex(vc, pose, q.x0(), q.y1(), q.color());
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, int color) {
        vc.addVertex(pose, x, y, 0.0F).setColor(color).setUv(0.5F, 0.5F).setLight(LIGHT);
    }
}
*///?}
