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
        double days = worldTime / 24000.0;
        double regionalPhase = (regionSeed & 0x7fffffffL) * 0.000013;
        double synoptic = days * 0.31 + regionalPhase;
        double mesoscale = days * 1.17 + regionalPhase * 1.73;
        double turbulence = days * 4.9 + regionalPhase * 0.37;

        // Slowly moving pressure systems create broad, persistent trends.
        double pressureWave = Math.sin(synoptic) * 0.10
                + Math.sin(synoptic * 0.47 + 2.1) * 0.045;
        double pressureTarget = 1013.0 + pressureWave * 18.0;
        double pressure = approach(current.pressureHpa(), pressureTarget, 0.012)
                + Math.sin(mesoscale * 1.4) * 0.012;

        // Temperature has a slow daily cycle and a weaker regional wave.
        double dailyPhase = (worldTime % 24000L) / 24000.0 * Math.PI * 2.0;
        double temperatureTarget = 14.0
                + Math.sin(dailyPhase - Math.PI / 2.0) * 7.0
                + Math.sin(synoptic * 0.62) * 5.0;
        double temperature = approach(current.temperatureCelsius(), temperatureTarget, 0.0018);

        // Moisture responds to the moving pressure pattern and local oscillations.
        double humidityTarget = 0.59
                + Math.sin(synoptic * 0.82 + 1.2) * 0.17
                + Math.sin(mesoscale * 0.58) * 0.055;
        double humidity = approach(current.relativeHumidity(), humidityTarget, 0.00011);

        // Wind accelerates around lower pressure and slowly veers with the system.
        double pressureGradient = Math.max(0.0, 1013.0 - pressure);
        double windTarget = 2.0 + pressureGradient * 0.24
                + Math.max(0.0, Math.sin(mesoscale)) * 3.5;
        double windSpeed = approach(current.windSpeedMps(), windTarget, 0.0012)
                + Math.max(0.0, Math.sin(turbulence)) * 0.025;

        double directionDrift = Math.sin(synoptic * 0.71) * 0.018
                + Math.sin(mesoscale * 0.91) * 0.009;
        double windDirection = current.windDirectionDegrees() + directionDrift;

        double thermalContribution = Math.max(0.0, (temperature - 16.0) / 25.0);
        double moistureContribution = Math.max(0.0, humidity - 0.48);
        double pressureContribution = Math.max(0.0, (1015.0 - pressure) / 35.0);
        double windContribution = Math.max(0.0, windSpeed - 8.0) / 35.0;
        double instabilityTarget = Math.min(1.0,
                thermalContribution * 0.38
                        + moistureContribution * 0.78
                        + pressureContribution * 0.16
                        + windContribution * 0.12
                        + Math.max(0.0, Math.sin(turbulence)) * 0.025);
        double instability = approach(current.convectiveInstability(), instabilityTarget, 0.00008);

        return new AtmosphericCell(temperature, humidity, pressure, windSpeed,
                windDirection, instability);
    }

    private static double approach(double current, double target, double factor) {
        return current + (target - current) * factor;
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
        double wind = cell.windSpeedMps();

        if (humidity >= 0.82 && instability >= 0.68 && pressure <= 1002.0
                && wind >= 5.0) {
            return WeatherType.THUNDERSTORM;
        }
        if (humidity >= 0.78 && temperature <= 1.5 && pressure <= 1018.0) {
            return WeatherType.SNOW;
        }
        if (humidity >= 0.73 && pressure <= 1015.0) {
            return WeatherType.RAIN;
        }
        if (humidity >= 0.66 || pressure <= 1007.0 || wind >= 13.0) {
            return WeatherType.OVERCAST;
        }
        return WeatherType.CLEAR;
    }
}