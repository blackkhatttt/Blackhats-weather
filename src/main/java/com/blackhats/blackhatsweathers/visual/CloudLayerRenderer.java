package com.blackhats.blackhatsweathers.visual;

import com.blackhats.blackhatsweathers.config.WeatherConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Dynamic layered cloud pass.
 *
 * Cloud fields are deterministic and vary gradually by region and world time.
 * Native rain and thunder intensify coverage and darkening. This remains a
 * lightweight visual approximation, not a volumetric cloud simulation.
 */
@EventBusSubscriber(modid = "blackhatsweathers", value = Dist.CLIENT)
public final class CloudLayerRenderer {
    private static final double CLOUD_BASE_HEIGHT = 190.0D;
    private static final int CELL_SIZE = 32;
    private static final float MAX_ALPHA = 112.0F;

    private CloudLayerRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null || level.dimension() != Level.OVERWORLD) {
            return;
        }

        int detail = WeatherConfig.getQualityProfile().getCloudDetail();
        if (detail <= 0) {
            return;
        }

        float rain = level.getRainLevel(0.0F);
        float thunder = level.getThunderLevel(0.0F);
        Vec3 camera = event.getCamera().getPosition();
        if (camera.y >= CLOUD_BASE_HEIGHT + 24.0D) {
            return;
        }

        // Slowly varying regional cloud fields keep clouds visible in fair weather.
        double worldPhase = level.getDayTime() * 0.000025D;
        double regionalWave = Math.sin(camera.x * 0.0017D + camera.z * 0.0011D
                + worldPhase) * 0.5D
                + Math.sin(camera.x * 0.0007D - camera.z * 0.0019D
                - worldPhase * 0.63D) * 0.5D;
        float cloudCover = Mth.clamp(
                0.28F + (float) regionalWave * 0.16F + rain * 0.43F + thunder * 0.18F,
                0.10F, 0.98F);
        float opacity = Mth.clamp(
                0.42F + rain * 0.40F + thunder * 0.25F,
                0.25F, 1.0F);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        var buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
        Matrix4f pose = poseStack.last().pose();

        int radius = 2 + detail;
        int centerX = Mth.floor(camera.x / CELL_SIZE);
        int centerZ = Mth.floor(camera.z / CELL_SIZE);
        double drift = level.getGameTime() * (0.004D + thunder * 0.002D);

        float shade = Mth.clamp(1.0F - thunder * 0.34F - rain * 0.13F, 0.48F, 1.0F);
        int red = Mth.clamp((int) (239.0F * shade), 0, 255);
        int green = Mth.clamp((int) (243.0F * shade), 0, 255);
        int blue = Mth.clamp((int) (250.0F * shade), 0, 255);

        for (int gx = centerX - radius; gx <= centerX + radius; gx++) {
            for (int gz = centerZ - radius; gz <= centerZ + radius; gz++) {
                long seed = ((long) gx * 341873128712L) ^ ((long) gz * 132897987541L);
                Random random = new Random(seed);

                // Each region has a stable cloud pattern; cover changes gradually over time.
                float regionalVariation = 0.82F + random.nextFloat() * 0.36F;
                if (random.nextFloat() > cloudCover * regionalVariation) {
                    continue;
                }

                double cellX = gx * (double) CELL_SIZE + CELL_SIZE * 0.5D - drift;
                double cellZ = gz * (double) CELL_SIZE + CELL_SIZE * 0.5D + drift * 0.38D;

                // Broad lower deck with irregular overlapping cloud puffs.
                int lowerPuffs = 2 + detail;
                for (int i = 0; i < lowerPuffs; i++) {
                    double x = cellX + (random.nextDouble() - 0.5D) * 25.0D;
                    double z = cellZ + (random.nextDouble() - 0.5D) * 25.0D;
                    float width = 17.0F + random.nextFloat() * (9.0F + detail * 2.0F);
                    float depth = 13.0F + random.nextFloat() * (8.0F + detail);
                    double y = CLOUD_BASE_HEIGHT + random.nextDouble() * 1.8D;
                    int alpha = cloudAlpha(random, opacity, 0.62F, 1.0F);
                    drawCloudQuad(consumer, pose, x, y, z, width, depth,
                            red, green, blue, alpha);
                }

                // Convective weather builds taller, denser upper formations.
                int upperLayers = 1 + (int) Math.ceil(thunder * (1.0F + detail * 0.8F));
                for (int layer = 0; layer < upperLayers; layer++) {
                    int puffs = 1 + detail / 2;
                    for (int i = 0; i < puffs; i++) {
                        double x = cellX + (random.nextDouble() - 0.5D) * (18.0D + layer * 3.0D);
                        double z = cellZ + (random.nextDouble() - 0.5D) * (18.0D + layer * 3.0D);
                        float scale = 0.78F + random.nextFloat() * 0.42F;
                        float width = (14.0F + random.nextFloat() * 12.0F) * scale;
                        float depth = (11.0F + random.nextFloat() * 9.0F) * scale;
                        double y = CLOUD_BASE_HEIGHT + 2.0D + layer * (2.5D + thunder * 2.0D)
                                + random.nextDouble() * 1.2D;
                        int alpha = cloudAlpha(random, opacity, 0.48F, 0.78F);
                        int layerRed = Mth.clamp((int) (red * (1.0F - layer * 0.035F)), 0, 255);
                        int layerGreen = Mth.clamp((int) (green * (1.0F - layer * 0.035F)), 0, 255);
                        int layerBlue = Mth.clamp((int) (blue * (1.0F - layer * 0.025F)), 0, 255);
                        drawCloudQuad(consumer, pose, x, y, z, width, depth,
                                layerRed, layerGreen, layerBlue, alpha);
                    }
                }
            }
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.translucent());
    }

    private static int cloudAlpha(Random random, float opacity, float minScale, float maxScale) {
        float variation = minScale + random.nextFloat() * (maxScale - minScale);
        return Mth.clamp((int) (MAX_ALPHA * opacity * variation), 0, 145);
    }

    private static void drawCloudQuad(
            VertexConsumer consumer, Matrix4f pose, double x, double y, double z,
            float width, float depth, int red, int green, int blue, int alpha
    ) {
        float left = (float) (x - width * 0.5F);
        float right = (float) (x + width * 0.5F);
        float front = (float) (z - depth * 0.5F);
        float back = (float) (z + depth * 0.5F);
        float height = (float) y;

        addVertex(consumer, pose, left, height, front, red, green, blue, alpha);
        addVertex(consumer, pose, left, height, back, red, green, blue, alpha);
        addVertex(consumer, pose, right, height, back, red, green, blue, alpha);
        addVertex(consumer, pose, right, height, front, red, green, blue, alpha);
    }

    private static void addVertex(
            VertexConsumer consumer, Matrix4f pose, float x, float y, float z,
            int red, int green, int blue, int alpha
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}
