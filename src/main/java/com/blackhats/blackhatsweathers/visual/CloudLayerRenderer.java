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
 * Procedural 3D cloud formations.
 *
 * Cloud morphology is selected from atmospheric/vanilla weather conditions.
 * Graphics profiles only control mesh detail and the number of rendered
 * cloud components; they never change cloud formation selection.
 */
@EventBusSubscriber(modid = "blackhatsweathers", value = Dist.CLIENT)
public final class CloudLayerRenderer {
    private static final int CELL_SIZE = 48;
    private static final double CLOUD_HEIGHT = 188.0D;
    private static final float BASE_ALPHA = 166.0F;

    private enum CloudForm {
        CIRRUS,
        CUMULUS,
        STRATUS,
        CUMULONIMBUS
    }

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

        // Detail 0 is still rendered: it is the absolute-minimum PC preset.
        int detail = Mth.clamp(WeatherConfig.getQualityProfile().getCloudDetail(), 0, 5);
        float rain = level.getRainLevel(0.0F);
        float thunder = level.getThunderLevel(0.0F);
        Vec3 camera = event.getCamera().getPosition();

        if (camera.y > 252.0D || camera.y < 35.0D) {
            return;
        }

        float cover = Mth.clamp(
                0.38F + rain * 0.39F + thunder * 0.20F
                        + (float) Math.sin(camera.x * 0.0011D + camera.z * 0.0015D
                        + level.getDayTime() * 0.00002D) * 0.13F,
                0.12F, 0.98F);
        float opacity = Mth.clamp(0.60F + rain * 0.25F + thunder * 0.18F, 0.45F, 1.0F);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        var buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.translucent());
        Matrix4f pose = poseStack.last().pose();

        int radius = 1 + detail / 3;
        int centerX = Mth.floor(camera.x / CELL_SIZE);
        int centerZ = Mth.floor(camera.z / CELL_SIZE);
        double drift = level.getGameTime() * (0.0025D + rain * 0.0015D);

        // Same weather conditions choose the same formations at every quality.
        CloudForm dominantForm = chooseDominantForm(rain, thunder);

        for (int gx = centerX - radius; gx <= centerX + radius; gx++) {
            for (int gz = centerZ - radius; gz <= centerZ + radius; gz++) {
                long seed = ((long) gx * 341873128712L) ^ ((long) gz * 132897987541L);
                Random random = new Random(seed);
                float localCover = cover * (0.78F + random.nextFloat() * 0.42F);
                if (random.nextFloat() > localCover) {
                    continue;
                }

                double x = gx * (double) CELL_SIZE + CELL_SIZE * 0.5D - drift;
                double z = gz * (double) CELL_SIZE + CELL_SIZE * 0.5D + drift * 0.43D;

                CloudForm form = dominantForm;
                if (thunder < 0.08F && rain < 0.10F) {
                    form = random.nextFloat() < 0.52F ? CloudForm.CIRRUS : CloudForm.CUMULUS;
                } else if (thunder < 0.12F && random.nextFloat() < 0.28F) {
                    form = CloudForm.CUMULUS;
                }

                int shade = Mth.clamp((int) (255.0F * (1.0F - thunder * 0.36F - rain * 0.14F)), 105, 255);
                int red = shade;
                int green = Mth.clamp(shade + 3, 0, 255);
                int blue = Mth.clamp(shade + 8, 0, 255);
                int alpha = Mth.clamp((int) (BASE_ALPHA * opacity), 65, 190);

                renderFormation(consumer, pose, random, form, x, z, detail,
                        red, green, blue, alpha, rain, thunder);
            }
        }

        poseStack.popPose();
        buffers.endBatch(RenderType.translucent());
    }

    private static CloudForm chooseDominantForm(float rain, float thunder) {
        if (thunder >= 0.12F) {
            return CloudForm.CUMULONIMBUS;
        }
        if (rain >= 0.10F) {
            return CloudForm.STRATUS;
        }
        return CloudForm.CUMULUS;
    }

    private static void renderFormation(
            VertexConsumer consumer, Matrix4f pose, Random random, CloudForm form,
            double x, double z, int detail, int red, int green, int blue,
            int alpha, float rain, float thunder
    ) {
        int rings = 3 + detail;
        int sectors = 5 + detail * 2;
        int puffs = 1 + detail / 2;

        switch (form) {
            case CIRRUS -> {
                // High, thin, wind-stretched wisps.
                double y = CLOUD_HEIGHT + 29.0D;
                int wisps = 1 + detail / 2;
                for (int i = 0; i < wisps; i++) {
                    double offsetX = (random.nextDouble() - 0.5D) * 22.0D;
                    double offsetZ = (random.nextDouble() - 0.5D) * 18.0D;
                    double length = 18.0D + random.nextDouble() * 17.0D;
                    double width = 2.0D + random.nextDouble() * 2.0D;
                    drawEllipsoid(consumer, pose, x + offsetX, y + i * 1.1D,
                            z + offsetZ, length, width, 1.0D,
                            red, green, blue, alpha / 2, rings, sectors, random);
                }
            }
            case CUMULUS -> {
                // Separate rounded heaps with a broad, flatter base.
                double y = CLOUD_HEIGHT;
                drawEllipsoid(consumer, pose, x, y, z, 15.0D, 13.0D, 3.8D,
                        red, green, blue, alpha, rings, sectors, random);
                for (int i = 0; i < puffs; i++) {
                    double ox = (random.nextDouble() - 0.5D) * 21.0D;
                    double oz = (random.nextDouble() - 0.5D) * 18.0D;
                    double size = 5.0D + random.nextDouble() * (4.0D + detail);
                    drawEllipsoid(consumer, pose, x + ox, y + 2.0D + random.nextDouble() * 2.5D,
                            z + oz, size, size * (0.78D + random.nextDouble() * 0.25D),
                            size * 0.72D, red, green, blue, alpha, rings, sectors, random);
                }
            }
            case STRATUS -> {
                // Wide, continuous low cloud sheet with soft rolling undulations.
                drawEllipsoid(consumer, pose, x, CLOUD_HEIGHT - 3.0D, z,
                        34.0D, 31.0D, 3.0D, red, green, blue, alpha, rings, sectors, random);
                int ripples = 1 + detail;
                for (int i = 0; i < ripples; i++) {
                    double ox = (random.nextDouble() - 0.5D) * 34.0D;
                    double oz = (random.nextDouble() - 0.5D) * 34.0D;
                    drawEllipsoid(consumer, pose, x + ox, CLOUD_HEIGHT - 1.0D,
                            z + oz, 17.0D, 13.0D, 2.2D,
                            red, green, blue, alpha * 3 / 4, rings, sectors, random);
                }
            }
            case CUMULONIMBUS -> {
                // Deep convective tower, stacked turrets and a spreading anvil.
                double baseY = CLOUD_HEIGHT - 4.0D;
                drawEllipsoid(consumer, pose, x, baseY, z, 18.0D, 17.0D, 6.0D,
                        red, green, blue, alpha, rings, sectors, random);
                int towerLevels = 2 + detail / 2;
                for (int level = 0; level < towerLevels; level++) {
                    double y = baseY + 7.0D + level * (5.0D + thunder * 2.0D);
                    double scale = Math.max(5.5D, 12.0D - level * 1.25D);
                    int towerAlpha = Mth.clamp(alpha - level * 7, 55, 190);
                    drawEllipsoid(consumer, pose, x + (random.nextDouble() - 0.5D) * 3.0D,
                            y, z + (random.nextDouble() - 0.5D) * 3.0D,
                            scale, scale * 0.92D, 7.0D + thunder * 2.0D,
                            red, green, blue, towerAlpha, rings, sectors, random);
                }
                // Anvil spreads outward at the top of the storm tower.
                double anvilY = baseY + 9.0D + towerLevels * (5.0D + thunder * 2.0D);
                drawEllipsoid(consumer, pose, x + 2.0D, anvilY, z,
                        29.0D + thunder * 8.0D, 18.0D + thunder * 5.0D,
                        3.0D + thunder, red, green, blue,
                        Mth.clamp(alpha - 18, 50, 175), rings, sectors, random);
            }
        }
    }

    /**
     * Draws a shaded, two-sided ellipsoid mesh. Two-sided triangles keep the
     * cloud visible from below without relying on a texture or external asset.
     */
    private static void drawEllipsoid(
            VertexConsumer consumer, Matrix4f pose, double cx, double cy, double cz,
            double rx, double rz, double ry, int red, int green, int blue,
            int alpha, int rings, int sectors, Random random
    ) {
        double phase = random.nextDouble() * Math.PI * 2.0D;
        for (int ring = 0; ring < rings; ring++) {
            double lat0 = -Math.PI / 2.0D + Math.PI * ring / rings;
            double lat1 = -Math.PI / 2.0D + Math.PI * (ring + 1) / rings;
            for (int sector = 0; sector < sectors; sector++) {
                double lon0 = Math.PI * 2.0D * sector / sectors;
                double lon1 = Math.PI * 2.0D * (sector + 1) / sectors;
                CloudVertex a = point(cx, cy, cz, rx, rz, ry, lat0, lon0 + phase);
                CloudVertex b = point(cx, cy, cz, rx, rz, ry, lat1, lon0 + phase);
                CloudVertex c = point(cx, cy, cz, rx, rz, ry, lat1, lon1 + phase);
                CloudVertex d = point(cx, cy, cz, rx, rz, ry, lat0, lon1 + phase);

                addTriangle(consumer, pose, a, b, c, red, green, blue, alpha);
                addTriangle(consumer, pose, a, c, d, red, green, blue, alpha);
                addTriangle(consumer, pose, c, b, a, red, green, blue, alpha);
                addTriangle(consumer, pose, d, c, a, red, green, blue, alpha);
            }
        }
    }

    private record CloudVertex(float x, float y, float z, float nx, float ny, float nz) {
    }

    private static CloudVertex point(
            double cx, double cy, double cz, double rx, double rz, double ry,
            double latitude, double longitude
    ) {
        double cosLat = Math.cos(latitude);
        double nx = cosLat * Math.cos(longitude);
        double nz = cosLat * Math.sin(longitude);
        double ny = Math.sin(latitude);
        return new CloudVertex(
                (float) (cx + nx * rx),
                (float) (cy + ny * ry),
                (float) (cz + nz * rz),
                (float) (nx / rx),
                (float) (ny / ry),
                (float) (nz / rz)
        );
    }

    private static void addTriangle(
            VertexConsumer consumer, Matrix4f pose, CloudVertex a, CloudVertex b,
            CloudVertex c, int red, int green, int blue, int alpha
    ) {
        addVertex(consumer, pose, a, red, green, blue, alpha);
        addVertex(consumer, pose, b, red, green, blue, alpha);
        addVertex(consumer, pose, c, red, green, blue, alpha);
    }

    private static void addVertex(
            VertexConsumer consumer, Matrix4f pose, CloudVertex vertex,
            int red, int green, int blue, int alpha
    ) {
        float length = Mth.sqrt(vertex.nx * vertex.nx + vertex.ny * vertex.ny + vertex.nz * vertex.nz);
        float nx = vertex.nx / length;
        float ny = vertex.ny / length;
        float nz = vertex.nz / length;
        float light = Mth.clamp(0.72F + Math.max(0.0F, ny) * 0.28F
                + Math.max(0.0F, nx * 0.25F + nz * 0.35F) * 0.10F, 0.55F, 1.0F);

        consumer.addVertex(pose, vertex.x, vertex.y, vertex.z)
                .setColor(
                        Mth.clamp((int) (red * light), 0, 255),
                        Mth.clamp((int) (green * light), 0, 255),
                        Mth.clamp((int) (blue * light), 0, 255),
                        alpha)
                .setUv(0.0F, 0.0F)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(nx, ny, nz);
    }
}
