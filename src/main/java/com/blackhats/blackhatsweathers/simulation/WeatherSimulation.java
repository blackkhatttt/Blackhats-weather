package com.blackhats.blackhatsweathers.simulation;

import com.blackhats.blackhatsweathers.core.AtmosphericCell;

/**
 * Deterministic, lightweight atmospheric evolution model.
 * This is the foundation for later advection, fronts and convective storm systems.
 */
public final class WeatherSimulation {
    private WeatherSimulation() {
    }

    public static AtmosphericCell advance(AtmosphericCell current, long worldTime, long regionSeed) {
        double phase = (worldTime / 24000.0) + (regionSeed * 0.000013);
        double slowWave = Math.sin(phase * Math.PI * 2.0);
        double fastWave = Math.sin(phase * Math.PI * 5.0 + 1.7);

        double temperature = current.temperatureCelsius() + slowWave * 0.035;
        double humidity = current.relativeHumidity() + fastWave * 0.0015;
        double pressure = current.pressureHpa() + Math.sin(phase * 1.4) * 0.025;
        double windSpeed = current.windSpeedMps() + Math.sin(phase * 2.1 + 0.4) * 0.025;
        double windDirection = current.windDirectionDegrees() + Math.sin(phase) * 0.15;

        double thermalContribution = Math.max(0.0, (temperature - 18.0) / 22.0);
        double moistureContribution = Math.max(0.0, humidity - 0.45);
        double instability = Math.min(1.0, thermalContribution * 0.45 + moistureContribution * 0.9);

        return new AtmosphericCell(
                temperature,
                humidity,
                pressure,
                windSpeed,
                windDirection,
                instability
        );
    }
}
