package com.blackhats.blackhatsweathers.simulation;

import com.blackhats.blackhatsweathers.BlackhatsWeathers;
import com.blackhats.blackhatsweathers.config.WeatherConfig;
import com.blackhats.blackhatsweathers.core.AtmosphericCell;
import com.blackhats.blackhatsweathers.core.ClimateGrid;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-authoritative bridge between the atmospheric model and Minecraft's
 * native precipitation/weather timers. One regional cell currently drives
 * the visible weather in each dimension; regional rendering comes later.
 */
public final class WeatherController {
    private static final int MIN_WEATHER_DURATION = 6_000;
    private static final int MAX_WEATHER_DURATION = 18_000;
    private final Map<ResourceKey<Level>, DimensionWeather> dimensions = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        int interval = WeatherConfig.SIMULATION_INTERVAL_TICKS.get();
        if (interval < 1) interval = 1;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            long gameTime = level.getGameTime();
            if (gameTime % interval != 0) continue;

            DimensionWeather runtime = dimensions.computeIfAbsent(
                    level.dimension(),
                    key -> new DimensionWeather(level.getSeed())
            );

            int cellSize = WeatherConfig.CLIMATE_CELL_SIZE.get();
            BlockPos spawn = level.getSharedSpawnPos();
            int cellX = Math.floorDiv(spawn.getX(), cellSize);
            int cellZ = Math.floorDiv(spawn.getZ(), cellSize);

            runtime.grid.updateCell(cellX, cellZ, gameTime);
            AtmosphericCell atmosphere = runtime.grid.getCell(cellX, cellZ);
            WeatherType candidate = WeatherSimulation.classify(atmosphere);

            if (runtime.remainingTicks > 0) {
                runtime.remainingTicks -= interval;
            }

            if (runtime.current == null || (runtime.remainingTicks <= 0 && candidate != runtime.current)) {
                runtime.current = candidate;
                runtime.remainingTicks = MIN_WEATHER_DURATION
                        + (int) Math.floorMod(gameTime ^ level.getSeed(), MAX_WEATHER_DURATION - MIN_WEATHER_DURATION + 1);
                applyVanillaWeather(level, candidate, runtime.remainingTicks);
                BlackhatsWeathers.LOGGER.info(
                        "Weather in {} changed to {} (T={} C, RH={}%, P={} hPa, wind={} m/s).",
                        level.dimension().location(),
                        candidate,
                        Math.round(atmosphere.temperatureCelsius() * 10.0) / 10.0,
                        Math.round(atmosphere.relativeHumidity() * 100.0),
                        Math.round(atmosphere.pressureHpa() * 10.0) / 10.0,
                        Math.round(atmosphere.windSpeedMps() * 10.0) / 10.0
                );
            }
        }
    }

    private static void applyVanillaWeather(ServerLevel level, WeatherType type, int duration) {
        boolean raining = type.hasPrecipitation();
        boolean thundering = type.hasThunder();
        int clearTime = raining ? 0 : duration;
        int rainTime = raining ? duration : 0;
        level.setWeatherParameters(clearTime, rainTime, raining, thundering);
    }

    private static final class DimensionWeather {
        private final ClimateGrid grid;
        private WeatherType current;
        private int remainingTicks;

        private DimensionWeather(long seed) {
            this.grid = new ClimateGrid(seed);
        }
    }
}
