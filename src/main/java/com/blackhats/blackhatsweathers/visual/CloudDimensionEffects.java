package com.blackhats.blackhatsweathers.visual;

import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

/**
 * Replaces only the Overworld cloud pass while preserving vanilla Overworld
 * sky, fog, sunrise, and lighting behavior.
 */
@EventBusSubscriber(
        modid = "blackhatsweathers",
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class CloudDimensionEffects {
    private CloudDimensionEffects() {
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(
                ResourceLocation.withDefaultNamespace("overworld"),
                new BlackhatsOverworldEffects()
        );
    }

    private static final class BlackhatsOverworldEffects
            extends DimensionSpecialEffects.OverworldEffects {

        @Override
        public boolean renderClouds(
                net.minecraft.client.multiplayer.ClientLevel level,
                int ticks,
                float partialTick,
                com.mojang.blaze3d.vertex.PoseStack poseStack,
                double camX,
                double camY,
                double camZ,
                org.joml.Matrix4f modelViewMatrix,
                org.joml.Matrix4f projectionMatrix
        ) {
            CloudLayerRenderer.renderClouds(
                    level, ticks, partialTick, poseStack, camX, camY, camZ
            );

            // true tells NeoForge not to draw Minecraft's original cloud layer.
            return true;
        }
    }
}
