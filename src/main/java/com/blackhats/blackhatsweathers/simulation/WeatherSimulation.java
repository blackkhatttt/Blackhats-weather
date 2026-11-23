package com.blackhats.blackhatsweathers.simulation;

import com.blackhats.blackhatsweathers.core.AtmosphericCell;

/**
 * Deterministic atmospheric evolution and weather classification.
 * All calculations are server-side and independent from visual quality.
 */
public final class WeatherSimulation {
    private WeatherSimulation() {
    }

    public static AtmosphericCell advance(AtmosphericCell current, long worldTime, long regionSeed) {
        double phase = (worldTime / 24000.0) + (regionSeed * 0.000013);
        double slowWave = Math.sin(phase * Math.PI * 2.0);
        double mediumWave = Math.sin(phase * Math.PI * 0.73 + 1.7);
        double fastWave = Math.sin(phase * Math.PI * 5.0 + 0.9);

        double temperature = current.temperatureCelsius() + slowWave * 0.018;
        double humidity = current.relativeHumidity() + mediumWave * 0.0012;
        double pressure = current.pressureHpa() + Math.sin(phase * 1.4) * 0.035;
        double windSpeed = current.windSpeedMps() + Math.sin(phase * 2.1 + 0.4) * 0.045;
        double windDirection = current.windDirectionDegrees() + Math.sin(phase * 0.8) * 0.35;

        double thermalContribution = Math.max(0.0, (temperature - 16.0) / 25.0);
        double moistureContribution = Math.max(0.0, humidity - 0.48);
        double pressureContribution = Math.max(0.0, (1015.0 - pressure) / 35.0);
        double instability = Math.min(1.0,
                thermalContribution * 0.42
                        + moistureContribution * 0.85
                        + pressureContribution * 0.18
                        + Math.max(0.0, fastWave) * 0.04);

        return new AtmosphericCell(temperature, humidity, pressure, windSpeed,
                windDirection, instability);
    }

    /**
     * Converts atmospheric conditions into a broad weather state.
     * Biome precipitation rules still determine the native particle type.
     */
    public static WeatherType classify(AtmosphericCell cell) {
        double humidity = cell.relativeHumidity();
        double pressure = cell.pressureHpa();
        double temperature = cell.temperatureCelsius();
        double instability = cell.convectiveInstability();

        if (humidity >= 0.82 && instability >= 0.68 && pressure <= 1002.0) {
            return WeatherType.THUNDERSTORM;
        }
        if (humidity >= 0.78 && temperature <= 1.5) {
            return WeatherType.SNOW;
        }
        if (humidity >= 0.73 && pressure <= 1012.0) {
            return WeatherType.RAIN;
        }
        if (humidity >= 0.66 || pressure <= 1005.0) {
            return WeatherType.OVERCAST;
        }
        return WeatherType.CLEAR;
    }
}
