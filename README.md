# BLACKHATS WEATHERS

Advanced, physically inspired weather simulation for Minecraft Java 1.21.1 using NeoForge.

## Current version
- Mod version: 0.3.0
- Minecraft: 1.21.1
- NeoForge: 21.1.267
- Java: 21
- Mod ID: `blackhatsweathers`

## 0.3.0 — Atmospheric Visual Engine (First Pass)
This milestone introduces the first client-side atmospheric rendering layer:
- Rain and thunderstorms tint the existing biome fog toward a cool grey atmosphere.
- Terrain visibility is reduced progressively according to rain and thunder intensity.
- The six visual-quality profiles scale the strength of the atmospheric fog effect.
- The renderer runs only on the logical client; weather simulation remains server-authoritative.
- The visual layer does not change the underlying weather simulation.

This is the first visual pass, not the final cloud renderer. Custom cloud geometry, custom sky rendering, custom precipitation particles, regional weather rendering, fronts and extreme events are not implemented yet.

## 0.2.0 — Dynamic Weather System
The server-driven weather loop remains the simulation foundation:
- Atmospheric conditions are updated on the server at the configured simulation interval.
- Each dimension maintains a deterministic regional climate grid.
- Temperature, humidity, pressure, wind and convective instability feed weather classification.
- Weather states: CLEAR, OVERCAST, RAIN, SNOW and THUNDERSTORM.
- Weather transitions are held for multi-minute intervals instead of being selected every tick.
- The controller applies precipitation and thunder through Minecraft's native weather system.
- Snow is selected under cold, humid conditions; Minecraft biome rules determine whether native precipitation appears as rain or snow.
- One climate cell currently drives the visible weather for each dimension.

## Graphics profiles
1. PLAYING IN A MOLECULE OF OXYGEN
2. POTATO
3. BAD QUALITY
4. MEDIUM
5. HIGH
6. CINEMATOGRAPHIC

Changing a graphics profile affects rendering quality only, never the simulated atmospheric conditions.

## Build
Use Java 21 and Gradle 8.14:
```bash
gradle build
```
The mod JAR is generated in `build/libs/`.

## License
All Rights Reserved.