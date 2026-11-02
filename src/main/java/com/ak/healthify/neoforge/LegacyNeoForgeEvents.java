package com.ak.healthify.neoforge;

//? if neoforge && <26.1 {
import com.ak.healthify.Healthify;
import com.ak.healthify.client.NameTags;
import com.ak.healthify.client.compat.ChampionsCompat;
import com.ak.healthify.client.render.LegacyRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderNameTagEvent;
import net.neoforged.neoforge.common.util.TriState;

@EventBusSubscriber(modid = Healthify.MODID, value = Dist.CLIENT)
public final class LegacyNeoForgeEvents {
    private static final ResourceLocation CHAMPIONS_HUD = ResourceLocation.fromNamespaceAndPath(ChampionsCompat.MOD_ID, "health_overlay");

    private LegacyNeoForgeEvents() {}

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        if (event.getName().equals(CHAMPIONS_HUD) && ChampionsCompat.hideOwnHud()) event.setCanceled(true);
    }

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
        LegacyRenderer.render(event.getCamera().getPosition(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false), event.getModelViewMatrix());
    }
}
//?}
