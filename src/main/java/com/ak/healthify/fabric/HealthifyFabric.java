package com.ak.healthify.fabric;

//? if fabric && <26.1 {
/*import com.ak.healthify.client.ClientEvents;
import com.ak.healthify.client.render.LegacyRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public class HealthifyFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientEvents.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientEvents.endTick());
        WorldRenderEvents.END.register(context -> LegacyRenderer.render(context.camera().getPosition(),
                context.tickCounter().getGameTimeDeltaPartialTick(false), context.positionMatrix()));
    }
}
*///?}
//? if fabric && >=26.1 {
/*import com.ak.healthify.client.ClientEvents;
import com.ak.healthify.client.render.ModernRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

public class HealthifyFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientEvents.init();
        ClientTickEvents.END_CLIENT_TICK.register(client -> ClientEvents.endTick());
        LevelRenderEvents.COLLECT_SUBMITS.register(context -> ModernRenderer.submit(context.submitNodeCollector(),
                context.poseStack(), context.levelState().cameraRenderState));
    }
}
*///?}
