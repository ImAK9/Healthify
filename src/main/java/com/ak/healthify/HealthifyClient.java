package com.ak.healthify;

import com.ak.healthify.client.theme.ThemeScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = Healthify.MODID, dist = Dist.CLIENT)
public class HealthifyClient {
    public HealthifyClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ThemeScreen::new);

        //? if >=26.1 {
        /*
        modBus.addListener(com.ak.healthify.client.render.ModernRenderer::registerPipelines);
        *///?}
    }
}
