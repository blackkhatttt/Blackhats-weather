package com.blackhats.blackhatsweathers.core;

/**
 * Atmospheric state for one regional climate cell.
 *
 * @param temperatureCelsius air temperature in degrees Celsius
 * @param relativeHumidity relative humidity in the range 0..1
 * @param pressureHpa atmospheric pressure in hectopascals
 * @param windSpeedMps horizontal wind speed in metres per second
 * @param windDirectionDegrees direction the wind is travelling toward, clockwise from north
 * @param convectiveInstability simplified instability index in the range 0..1
 */
public record AtmosphericCell(
        double temperatureCelsius,
        double relativeHumidity,
        double pressureHpa,
        double windSpeedMps,
        double windDirectionDegrees,
        double convectiveInstability
) {
    public AtmosphericCell {
        relativeHumidity = clamp(relativeHumidity, 0.0, 1.0);
        pressureHpa = clamp(pressureHpa, 850.0, 1100.0);
        windSpeedMps = clamp(windSpeedMps, 0.0, 80.0);
        windDirectionDegrees = normalizeDegrees(windDirectionDegrees);
        convectiveInstability = clamp(convectiveInstability, 0.0, 1.0);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double normalizeDegrees(double degrees) {
        double normalized = degrees % 360.0;
        return normalized < 0.0 ? normalized + 360.0 : normalized;
    }
}
