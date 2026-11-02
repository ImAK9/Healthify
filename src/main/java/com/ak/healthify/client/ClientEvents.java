package com.ak.healthify.client;

import com.ak.healthify.Config;
import com.ak.healthify.client.compat.L2HostilityCompat;
import com.ak.healthify.platform.Platform;

public final class ClientEvents {
    private ClientEvents() {}

    public static void init() {
        Config.SPEC.load(Platform.configDir());
    }

    public static void endTick() {
        Config.SPEC.tick();
        BarVisibility.tick();
        DamageNumbers.tick();
        L2HostilityCompat.tick();
    }
}
