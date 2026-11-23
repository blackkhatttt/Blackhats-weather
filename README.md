# BLACKHATS WEATHERS

Advanced, physically inspired weather simulation for Minecraft Java 1.21.1 using NeoForge.

## Current version
- Mod version: 0.2.0
- Minecraft: 1.21.1
- NeoForge: 21.1.267
- Java: 21
- Mod ID: `blackhatsweathers`

## 0.2.0 — Dynamic Weather System
This milestone adds the first server-driven weather loop:
- Atmospheric conditions are updated on the server at the configured simulation interval.
- Each dimension maintains a deterministic regional climate grid.
- Temperature, humidity, pressure, wind and convective instability feed weather classification.
- Weather states: CLEAR, OVERCAST, RAIN, SNOW and THUNDERSTORM.
- Weather transitions are held for multi-minute intervals instead of being selected every tick.
- The controller applies precipitation and thunder through Minecraft's native weather system.
- Snow is selected under cold, humid conditions; Minecraft biome rules determine whether native precipitation appears as rain or snow.
- Six visual-quality presets remain independent from the simulation.

The visible precipitation in this milestone uses Minecraft's native weather rendering. Custom clouds, custom precipitation particles, regional weather rendering, fronts and extreme events are not implemented yet. One climate cell currently drives the visible weather for each dimension.

## Graphics profiles
1. PLAYING IN A MOLECULE OF OXYGEN
2. POTATO
3. BAD QUALITY
4. MEDIUM
5. HIGH
6. CINEMATOGRAPHIC

Changing a graphics profile is intended to affect rendering quality only, never the simulated atmospheric conditions.

## Build
Use Java 21 and Gradle 8.14:
```bash
gradle build
```
The mod JAR is generated in `build/libs/`.

## License
All Rights Reserved.
