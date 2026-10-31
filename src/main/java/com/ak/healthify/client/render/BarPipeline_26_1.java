//? if >=26.1 && <26.3 {
/*package com.ak.healthify.client.render;

import com.ak.healthify.Healthify;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

final class BarPipeline {
    static final RenderPipeline ON_TOP = RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
            .withLocation(ResourceLocation.fromNamespaceAndPath(Healthify.MODID, "pipeline/bar_on_top"))
            .withVertexShader("core/rendertype_text_see_through")
            .withFragmentShader("core/rendertype_text_see_through")
            .withSampler("Sampler0")
            .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
            .build();

    private static final Map<ResourceLocation, RenderType> TYPES = new HashMap<>();

    private BarPipeline() {}

    static RenderType onTop(ResourceLocation texture) {
        return TYPES.computeIfAbsent(texture, t -> RenderType.create("healthify_bar_on_top",
                RenderSetup.builder(ON_TOP).withTexture("Sampler0", t).useLightmap().createRenderSetup()));
    }

    static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(ON_TOP);
    }
}
*///?}
