package com.blackhats.blackhatsweathers.visual;

import java.util.Locale;

public enum WeatherQualityProfile {
    PLAYING_IN_A_MOLECULE_OF_OXYGEN("PLAYING IN A MOLECULE OF OXYGEN", 0, 0, 0, false, false),
    POTATO("POTATO", 1, 1, 1, false, false),
    BAD_QUALITY("BAD QUALITY", 2, 2, 2, false, false),
    MEDIUM("MEDIUM", 3, 3, 3, false, false),
    HIGH("HIGH", 4, 4, 4, true, true),
    CINEMATOGRAPHIC("CINEMATOGRAPHIC", 5, 5, 5, true, true);

    private final String displayName;
    private final int cloudDetail;
    private final int particleDetail;
    private final int atmosphereDetail;
    private final boolean volumetricClouds;
    private final boolean advancedLighting;

    WeatherQualityProfile(String displayName, int cloudDetail, int particleDetail,
                          int atmosphereDetail, boolean volumetricClouds,
                          boolean advancedLighting) {
        this.displayName = displayName;
        this.cloudDetail = cloudDetail;
        this.particleDetail = particleDetail;
        this.atmosphereDetail = atmosphereDetail;
        this.volumetricClouds = volumetricClouds;
        this.advancedLighting = advancedLighting;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCloudDetail() {
        return cloudDetail;
    }

    public int getParticleDetail() {
        return particleDetail;
    }

    public int getAtmosphereDetail() {
        return atmosphereDetail;
    }

    public boolean hasVolumetricClouds() {
        return volumetricClouds;
    }

    public boolean hasAdvancedLighting() {
        return advancedLighting;
    }

    public static WeatherQualityProfile fromConfigName(String name) {
        if (name == null) return null;
        String normalized = name.trim().toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        for (WeatherQualityProfile profile : values()) {
            if (profile.name().equals(normalized)) return profile;
        }
        return null;
    }
}
