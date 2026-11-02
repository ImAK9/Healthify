package com.ak.healthify.neoforge;

//? if neoforge {
import com.ak.healthify.Healthify;
import com.ak.healthify.client.ClientEvents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = Healthify.MODID, value = Dist.CLIENT)
public final class NeoForgeClientEvents {
    private NeoForgeClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientEvents.endTick();
    }
}
//?}
