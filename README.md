# BLACKHATS WEATHERS

Advanced, physically inspired weather simulation for Minecraft Java 1.21.1 using NeoForge.

## Current version
- Mod version: 0.8.3
- Minecraft: 1.21.1
- NeoForge: 21.1.267
- Java: 21
- Mod ID: `blackhatsweathers`

## 0.8.3 — Cloud Visual Refinement
- Increased mesh resolution at every graphics profile to reduce visibly polygonal cloud silhouettes.
- Added subtle deterministic shape deformation to avoid repetitive perfect ellipsoids.
- Refined cumulus clusters into broader, more connected cloud masses with softer-looking lobes.
- Reduced excessive opacity and overlapping-layer intensity to limit harsh bright patches.
- Refined cirrus wisps, stratus sheets and cumulonimbus towers while preserving their weather-based selection.
- Graphics profiles still affect rendering detail and range only; weather simulation and cloud formation selection remain independent.

## 0.8.2 — Untextured Cloud Rendering Fix
- Replaced the terrain-atlas translucent render type with a dedicated untextured POSITION_COLOR pipeline.
- Uses translucent blending, depth testing, two-sided geometry and color-only writes so procedural cloud meshes do not depend on block-atlas UV sampling.
- Preserves the 3D cirrus, cumulus, stratus and cumulonimbus formations and all six graphics profiles.
- No changes to server-side weather simulation.

## 0.8.1 — Real Cloud Morphology
- Replaced flat cloud cards with procedural, shaded 3D ellipsoid meshes.
- Added distinct visual formations: high thin cirrus wisps, rounded cumulus heaps, broad stratus sheets, and towering cumulonimbus with a spreading anvil.
- Weather conditions select the cloud formation; all graphics profiles render the same formations for the same conditions.
- The six graphics profiles scale mesh resolution, component count and render distance only.
- PLAYING IN A MOLECULE OF OXYGEN is the absolute-minimum rendering profile and still displays every cloud formation in simplified geometry.
- Cloud rendering is a procedural 3D visual approximation, not a physically simulated volumetric atmosphere.

## 0.7.0 — Dynamic Cloud Fields
- Custom cloud formations are now visible during fair weather, not only during rain.
- Cloud coverage changes gradually with world time and regional position.
- Deterministic regional patterns create more varied skies while keeping cloud placement stable.
- Rain and thunder increase cloud coverage, opacity and darkening.
- Thunderstorms continue to create taller, denser upper cloud layers.
- Cloud detail follows the selected graphics profile.
- Rendering remains a lightweight layered-card approximation, not volumetric clouds.
- Cloud cover is currently a visual estimate based on regional/time patterns and vanilla precipitation; it is not yet directly driven by synchronized humidity or pressure values.

## 0.6.0 — Atmospheric Dynamics
- Reworked atmospheric evolution around slowly changing pressure systems.
- Added smoother temperature and humidity targets with daily and regional cycles.
- Wind speed responds gradually to pressure and mesoscale atmospheric variation.
- Wind direction slowly veers rather than remaining almost static.
- Convective instability evolves smoothly from temperature, moisture, pressure and wind.
- Weather classification now considers wind in thunderstorm and overcast conditions.
- Simulation remains server-side and independent from graphics quality.
- This is a simplified deterministic atmospheric model, not a numerical weather prediction system.

## 0.5.0 — Advanced Cloud Formation
- Builds irregular cloud clusters from overlapping translucent cloud patches.
- Adds multiple cloud layers at different heights.
- Thunder intensity increases the number and vertical separation of upper cloud layers.
- Cloud coverage, opacity and shading respond to rain and thunder.
- Cloud detail follows the selected graphics profile.
- Uses layered translucent cards, not true volumetric cloud rendering.
- Client-only rendering; does not alter server weather simulation.

## 0.4.0 — Cloud Rendering System
- Introduced the first custom cloud layer in the Overworld during precipitation.
- Cloud coverage and opacity respond to rain and thunder.
- Cloud patches drift slowly and use deterministic regional seeds.
- Cloud detail follows the selected graphics profile.

## 0.3.0 — Atmospheric Visual Engine
- Rain and thunderstorms tint biome fog toward cool grey.
- Terrain visibility is reduced progressively according to rain and thunder.
- Six visual-quality profiles scale the fog effect.
- Client-side rendering; server-authoritative simulation.

## 0.2.0 — Dynamic Weather System
- Server-driven atmospheric updates at the configured simulation interval.
- Deterministic regional climate grid.
- Temperature, humidity, pressure, wind and convective instability.
- Weather states: CLEAR, OVERCAST, RAIN, SNOW and THUNDERSTORM.
- Multi-minute weather transitions.
- Applies precipitation and thunder through Minecraft's native weather system.
- Snow classification depends on atmospheric temperature and humidity; biome rules determine native precipitation appearance.
- One climate cell currently drives visible weather for each dimension.

## Graphics profiles
1. PLAYING IN A MOLECULE OF OXYGEN
2. POTATO
3. BAD QUALITY
4. MEDIUM
5. HIGH
6. CINEMATOGRAPHIC

Graphics profiles affect rendering quality only, never atmospheric simulation.

## Build
Use Java 21 and Gradle 8.14:
```bash
gradle build
```
The mod JAR is generated in `build/libs/`.

## License
All Rights Reserved.
