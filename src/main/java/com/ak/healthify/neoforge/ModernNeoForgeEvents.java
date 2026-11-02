package com.ak.healthify.neoforge;

//? if neoforge && >=26.1 {
/*import com.ak.healthify.Healthify;
import com.ak.healthify.client.NameTags;
import com.ak.healthify.client.render.BarPipeline;
import com.ak.healthify.client.render.ModernRenderer;
import net.minecraft.util.TriState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;

@EventBusSubscriber(modid = Healthify.MODID, value = Dist.CLIENT)
public final class ModernNeoForgeEvents {
    private ModernNeoForgeEvents() {}

    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(BarPipeline.ON_TOP);
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
        ModernRenderer.submit(event.getSubmitNodeCollector(), event.getPoseStack(), event.getLevelRenderState().cameraRenderState);
    }
}
*///?}
