package com.blackhats.blackhatsweathers.simulation;

/**
 * Weather categories produced by the atmospheric model.
 * These describe simulated conditions, not graphics settings.
 */
public enum WeatherType {
    CLEAR(false, false),
    OVERCAST(false, false),
    RAIN(true, false),
    SNOW(true, false),
    THUNDERSTORM(true, true);

    private final boolean precipitation;
    private final boolean thunder;

    WeatherType(boolean precipitation, boolean thunder) {
        this.precipitation = precipitation;
        this.thunder = thunder;
    }

    public boolean hasPrecipitation() {
        return precipitation;
    }

    public boolean hasThunder() {
        return thunder;
    }
}
