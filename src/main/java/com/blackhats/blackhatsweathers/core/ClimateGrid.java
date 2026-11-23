package com.blackhats.blackhatsweathers.core;

import com.blackhats.blackhatsweathers.simulation.WeatherSimulation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.SplittableRandom;

/**
 * Sparse regional climate grid. Cells are created only when requested.
 * Coordinates are climate-grid coordinates, not block coordinates.
 */
public final class ClimateGrid {
    private final long worldSeed;
    private final Map<Long, AtmosphericCell> cells = new ConcurrentHashMap<>();

    public ClimateGrid(long worldSeed) {
        this.worldSeed = worldSeed;
    }

    public AtmosphericCell getCell(int gridX, int gridZ) {
        long key = pack(gridX, gridZ);
        return cells.computeIfAbsent(key, ignored -> createInitialCell(gridX, gridZ));
    }

    public void updateCell(int gridX, int gridZ, long worldTime) {
        long key = pack(gridX, gridZ);
        AtmosphericCell current = getCell(gridX, gridZ);
        long regionSeed = worldSeed ^ key;
        cells.put(key, WeatherSimulation.advance(current, worldTime, regionSeed));
    }

    public int getLoadedCellCount() {
        return cells.size();
    }

    private AtmosphericCell createInitialCell(int gridX, int gridZ) {
        long seed = worldSeed ^ (((long) gridX) * 341873128712L) ^ (((long) gridZ) * 132897987541L);
        SplittableRandom random = new SplittableRandom(seed);
        double temperature = 12.0 + random.nextDouble() * 18.0;
        double humidity = 0.25 + random.nextDouble() * 0.55;
        double pressure = 990.0 + random.nextDouble() * 35.0;
        double windSpeed = random.nextDouble() * 8.0;
        double windDirection = random.nextDouble() * 360.0;
        double instability = random.nextDouble() * 0.35;
        return new AtmosphericCell(temperature, humidity, pressure, windSpeed, windDirection, instability);
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }
}
