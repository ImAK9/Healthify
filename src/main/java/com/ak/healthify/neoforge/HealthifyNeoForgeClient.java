package com.ak.healthify.neoforge;

//? if neoforge {
import com.ak.healthify.Healthify;
import com.ak.healthify.client.ClientEvents;
import com.ak.healthify.client.theme.ThemeScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Healthify.MODID, dist = Dist.CLIENT)
public class HealthifyNeoForgeClient {
    public HealthifyNeoForgeClient(IEventBus modBus, ModContainer container) {
        ClientEvents.init();
        container.registerExtensionPoint(IConfigScreenFactory.class, (mod, parent) -> new ThemeScreen(parent));

        //? if >=26.1 {
        /*modBus.addListener(ModernNeoForgeEvents::registerPipelines);
        *///?}
    }
}
//?}
