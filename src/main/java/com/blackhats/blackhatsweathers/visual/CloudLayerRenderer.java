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
 * First custom cloud-layer pass.
 *
 * The renderer is client-only and deliberately independent from the
 * server-side atmospheric simulation. It draws deterministic, drifting
 * cloud patches during precipitation. These are layered cloud cards,
 * not volumetric clouds.
 */
@EventBusSubscriber(modid = "blackhatsweathers", value = Dist.CLIENT)
public final class CloudLayerRenderer {
    private static final double CLOUD_HEIGHT = 192.0D;
    private static final int CELL_SIZE = 32;
    private static final float MAX_ALPHA = 118.0F;

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

        WeatherQualityProfile profile = WeatherConfig.getQualityProfile();
        int detail = profile.getCloudDetail();
        if (detail <= 0) {
            return;
        }

        float rain = level.getRainLevel(0.0F);
        if (rain < 0.035F) {
            return;
        }

        float thunder = level.getThunderLevel(0.0F);
        float cover = Mth.clamp(0.28F + rain * 0.54F + thunder * 0.18F, 0.0F, 0.96F);
        float alphaScale = Mth.clamp(rain * (0.38F + thunder * 0.42F), 0.0F, 1.0F);
        if (alphaScale <= 0.01F) {
            return;
        }

        Vec3 camera = event.getCamera().getPosition();
        if (camera.y >= CLOUD_HEIGHT - 3.0D) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        Minecraft.getInstance().renderBuffers().bufferSource();
        var buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
        Matrix4f pose = poseStack.last().pose();

        int radius = 2 + detail;
        int centerX = Mth.floor(camera.x / CELL_SIZE);
        int centerZ = Mth.floor(camera.z / CELL_SIZE);
        double drift = event.getRenderTick() * 0.006D;

        float stormShade = 1.0F - thunder * 0.30F - rain * 0.12F;
        int red = Mth.clamp((int) (238.0F * stormShade), 0, 255);
        int green = Mth.clamp((int) (242.0F * stormShade), 0, 255);
        int blue = Mth.clamp((int) (248.0F * stormShade), 0, 255);

        for (int gx = centerX - radius; gx <= centerX + radius; gx++) {
            for (int gz = centerZ - radius; gz <= centerZ + radius; gz++) {
                long seed = ((long) gx * 341873128712L) ^ ((long) gz * 132897987541L);
                Random random = new Random(seed);

                if (random.nextFloat() > cover) {
                    continue;
                }

                double baseX = gx * (double) CELL_SIZE + CELL_SIZE * 0.5D - drift;
                double baseZ = gz * (double) CELL_SIZE + CELL_SIZE * 0.5D + drift * 0.42D;
                int puffs = 1 + Math.max(1, detail / 2);

                for (int puff = 0; puff < puffs; puff++) {
                    double offsetX = (random.nextDouble() - 0.5D) * 20.0D;
                    double offsetZ = (random.nextDouble() - 0.5D) * 20.0D;
                    float width = 15.0F + random.nextFloat() * (8.0F + detail * 2.0F);
                    float depth = 12.0F + random.nextFloat() * (7.0F + detail);
                    double y = CLOUD_HEIGHT + puff * 0.035D + random.nextDouble() * 0.025D;
                    int alpha = Mth.clamp((int) (MAX_ALPHA * alphaScale * (0.72F + random.nextFloat() * 0.28F)), 0, 150);

                    drawCloudQuad(consumer, pose,
                            baseX + offsetX, y, baseZ + offsetZ,
                            width, depth, red, green, blue, alpha);
                }
            }
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.translucent());
    }

    private static void drawCloudQuad(
            VertexConsumer consumer,
            Matrix4f pose,
            double x,
            double y,
            double z,
            float width,
            float depth,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        float left = (float) (x - width * 0.5F);
        float right = (float) (x + width * 0.5F);
        float front = (float) (z - depth * 0.5F);
        float back = (float) (z + depth * 0.5F);
        float height = (float) y;

        addVertex(consumer, pose, left, height, front, 0.0F, 0.0F, red, green, blue, alpha);
        addVertex(consumer, pose, left, height, back, 0.0F, 1.0F, red, green, blue, alpha);
        addVertex(consumer, pose, right, height, back, 1.0F, 1.0F, red, green, blue, alpha);
        addVertex(consumer, pose, right, height, front, 1.0F, 0.0F, red, green, blue, alpha);
    }

    private static void addVertex(
            VertexConsumer consumer,
            Matrix4f pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            int red,
            int green,
            int blue,
            int alpha
    ) {
        consumer.addVertex(pose, x, y, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(0.0F, 1.0F, 0.0F);
    }
}
