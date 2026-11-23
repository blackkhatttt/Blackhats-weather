package com.blackhats.blackhatsweathers.visual;

import com.blackhats.blackhatsweathers.config.WeatherConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * First client-side atmospheric rendering pass.
 *
 * It keeps the weather simulation server-authoritative and only changes
 * presentation: fog tint and terrain visibility during rain and storms.
 */
@EventBusSubscriber(modid = "blackhatsweathers", value = Dist.CLIENT)
public final class AtmosphericFogRenderer {
    private AtmosphericFogRenderer() {
    }

    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }

        float rain = minecraft.level.getRainLevel(event.getPartialTick());
        if (rain <= 0.01F) {
            return;
        }

        float thunder = minecraft.level.getThunderLevel(event.getPartialTick());
        float quality = getAtmosphereQuality();
        float intensity = Mth.clamp(rain * (0.16F + thunder * 0.22F) * quality, 0.0F, 0.42F);

        // A restrained cool-grey tint, blended with the biome's existing fog.
        event.setRed(Mth.lerp(intensity, event.getRed(), 0.58F));
        event.setGreen(Mth.lerp(intensity, event.getGreen(), 0.64F));
        event.setBlue(Mth.lerp(intensity, event.getBlue(), 0.70F));
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null
                || event.getCamera().getFluidInCamera() != FogType.NONE
                || event.getMode() != net.minecraft.client.renderer.FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }

        float rain = minecraft.level.getRainLevel(event.getPartialTick());
        if (rain <= 0.01F) {
            return;
        }

        float thunder = minecraft.level.getThunderLevel(event.getPartialTick());
        float quality = getAtmosphereQuality();
        float visibilityFactor = Mth.clamp(
                1.0F - rain * (0.16F + thunder * 0.12F) * quality,
                0.58F,
                1.0F
        );

        float originalFar = event.getFarPlaneDistance();
        float originalNear = event.getNearPlaneDistance();

        event.setNearPlaneDistance(Math.max(0.0F, originalNear * (1.0F - rain * 0.12F)));
        event.setFarPlaneDistance(Math.max(48.0F, originalFar * visibilityFactor));
        event.setCanceled(true);
    }

    private static float getAtmosphereQuality() {
        int detail = WeatherConfig.getQualityProfile().getAtmosphereDetail();
        return 0.55F + (detail / 5.0F) * 0.45F;
    }
}
