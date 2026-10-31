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
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

@EventBusSubscriber(modid = Healthify.MODID, value = Dist.CLIENT)
public final class LegacyRenderer {
    private static final int LIGHT = 0xF000F0;
    private static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Healthify.MODID, "textures/misc/white.png");
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new ByteBufferBuilder(16384));

    private LegacyRenderer() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRenderNameTag(RenderNameTagEvent event) {
        switch (NameTags.handle(event.getEntity(), event.getContent())) {
            case HIDE -> event.setCanRender(TriState.FALSE);
            case RESET -> {
                event.setContent(event.getOriginalContent());
                event.setCanRender(TriState.DEFAULT);
            }
            case KEEP -> { }
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;

        Minecraft mc = Minecraft.getInstance();
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        List<Placed> bars = HealthBars.collect(partialTick, camera);
        NameTags.clear();
        List<Placed> numbers = DamageNumbers.collect(camera);
        if (bars.isEmpty() && numbers.isEmpty()) return;

        Quaternionf orientation = mc.getEntityRenderDispatcher().cameraOrientation();

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(event.getModelViewMatrix());
        RenderSystem.applyModelViewMatrix();

        boolean depthTestWasOn = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        try {
            mc.renderBuffers().bufferSource().endBatch();

            boolean onTop = Config.DRAW_ON_TOP.get();
            if (onTop) {
                RenderSystem.disableDepthTest();
            }
            RenderType barType = onTop ? RenderType.textSeeThrough(WHITE_TEXTURE) : RenderType.textPolygonOffset(WHITE_TEXTURE);
            Font.DisplayMode textMode = onTop ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET;
            for (Placed bar : bars) {
                draw(bar, camera, orientation, barType, textMode);
            }

            RenderSystem.disableDepthTest();
            RenderType numberType = RenderType.textSeeThrough(WHITE_TEXTURE);
            for (Placed number : numbers) {
                draw(number, camera, orientation, numberType, Font.DisplayMode.SEE_THROUGH);
            }
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

    private static void draw(Placed placed, Vec3 camera, Quaternionf orientation, RenderType barType, Font.DisplayMode textMode) {
        Font font = Minecraft.getInstance().font;
        PoseStack pose = new PoseStack();
        pose.translate(placed.x() - camera.x, placed.y() - camera.y, placed.z() - camera.z);
        pose.mulPose(orientation);
        pose.translate(placed.driftX(), 0.0F, 0.0F);
        pose.scale(placed.scale(), -placed.scale(), placed.scale());
        Matrix4f matrix = pose.last().pose();
        DrawList draw = placed.draw();

        if (!draw.quads.isEmpty()) {
            VertexConsumer consumer = BUFFERS.getBuffer(barType);
            for (DrawList.Quad quad : draw.quads) {
                quad(consumer, pose.last(), quad);
            }
        }
        for (DrawList.Text text : draw.underlays) {
            text(font, text, matrix, textMode);
        }
        BUFFERS.endBatch();
        for (DrawList.Text text : draw.texts) {
            text(font, text, matrix, textMode);
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
//?}
