# BLACKHATS WEATHERS

Advanced, physically inspired weather simulation for Minecraft Java 1.21.1 using NeoForge.

## Current version
- Mod version: 0.1.0
- Minecraft: 1.21.1
- NeoForge: 21.1.267
- Java: 21
- Mod ID: `blackhatsweathers`

## 0.1.0 — Atmospheric Core
This first development milestone establishes the project foundation:
- Atmospheric state model: temperature, humidity, pressure, wind and instability.
- Deterministic regional climate grid.
- Six visual-quality presets, independent from the underlying weather simulation.
- GitHub Actions build workflow.

The initial release is an engineering foundation. Full in-world weather rendering, precipitation, storm formation and extreme events will arrive in later milestones.

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