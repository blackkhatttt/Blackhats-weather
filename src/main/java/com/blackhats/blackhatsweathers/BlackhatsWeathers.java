package com.blackhats.blackhatsweathers;

import com.blackhats.blackhatsweathers.config.WeatherConfig;
import com.blackhats.blackhatsweathers.simulation.WeatherController;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(BlackhatsWeathers.MOD_ID)
public final class BlackhatsWeathers {
    public static final String MOD_ID = "blackhatsweathers";
    public static final String VERSION = "0.4.0";
    public static final Logger LOGGER = LogUtils.getLogger();

    public BlackhatsWeathers(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, WeatherConfig.SPEC);
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new WeatherController());
        LOGGER.info("BLACKHATS WEATHERS {} initialized.", VERSION);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("BLACKHATS WEATHERS atmospheric visual systems are ready.");
    }
}