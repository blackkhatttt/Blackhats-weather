package com.blackhats.blackhatsweathers.config;

import com.blackhats.blackhatsweathers.visual.WeatherQualityProfile;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class WeatherConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<String> QUALITY_PROFILE = BUILDER
            .comment("Visual preset only. This setting must never change the underlying weather simulation.",
                    "Available: PLAYING_IN_A_MOLECULE_OF_OXYGEN, POTATO, BAD_QUALITY, MEDIUM, HIGH, CINEMATOGRAPHIC.")
            .define("qualityProfile", "HIGH",
                    value -> value instanceof String name && WeatherQualityProfile.fromConfigName(name) != null);

    public static final ModConfigSpec.IntValue CLIMATE_CELL_SIZE = BUILDER
            .comment("Horizontal width in blocks represented by one regional climate cell.")
            .defineInRange("climateCellSize", 256, 64, 1024);

    public static final ModConfigSpec.IntValue SIMULATION_INTERVAL_TICKS = BUILDER
            .comment("Number of server ticks between atmospheric simulation updates.")
            .defineInRange("simulationIntervalTicks", 20, 1, 1200);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private WeatherConfig() {
    }

    public static WeatherQualityProfile getQualityProfile() {
        WeatherQualityProfile profile = WeatherQualityProfile.fromConfigName(QUALITY_PROFILE.get());
        return profile == null ? WeatherQualityProfile.HIGH : profile;
    }
}
