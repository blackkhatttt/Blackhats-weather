package com.blackhats.blackhatsweathers.visual;

import com.blackhats.blackhatsweathers.config.WeatherConfig;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Procedural 3D cloud formations.
 * Graphics profiles change mesh density and visible range only.
 */
public final class CloudLayerRenderer {
    private static final int CELL_SIZE = 48;
    private static final double CLOUD_HEIGHT = 188.0D;
    private static final float BASE_ALPHA = 142.0F;

    private static final RenderType CLOUD_RENDER_TYPE = RenderType.create(
            "blackhatsweathers:procedural_clouds",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.TRIANGLES,
            262144,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false)
    );

    private enum CloudForm { CIRRUS, CUMULUS, STRATUS, CUMULONIMBUS }

    private CloudLayerRenderer() {}

    public static void renderClouds(
            ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
            double camX, double camY, double camZ
    ) {
        if (level == null) return;
        WeatherQualityProfile profile = WeatherConfig.getQualityProfile();
        if (profile.hasVolumetricClouds()
                && VolumetricCloudRenderer.renderClouds(
                        level, ticks, partialTick, poseStack, camX, camY, camZ, profile)) {
            return;
        }
        renderFallbackClouds(level, ticks, partialTick, poseStack, camX, camY, camZ);
    }

    private static void renderFallbackClouds(
            ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
            double camX, double camY, double camZ
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (level == null || camY > 320.0D || camY < -64.0D) return;

        int detail = Mth.clamp(WeatherConfig.getQualityProfile().getCloudDetail(), 0, 5);
        float rain = level.getRainLevel(partialTick);
        float thunder = level.getThunderLevel(partialTick);
        float cover = Mth.clamp(
                0.34F + rain * 0.42F + thunder * 0.20F
                        + (float) Math.sin(camX * 0.0011D + camZ * 0.0015D
                        + level.getDayTime() * 0.00002D) * 0.12F,
                0.10F, 0.96F);
        float opacity = Mth.clamp(0.54F + rain * 0.25F + thunder * 0.18F, 0.42F, 0.96F);

        poseStack.pushPose();
        poseStack.translate(-camX, -camY, -camZ);
        var buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(CLOUD_RENDER_TYPE);
        Matrix4f pose = poseStack.last().pose();

        // The lowest profile still gets smooth silhouettes, but fewer components.
        int radius = detail <= 1 ? 1 : (detail <= 3 ? 2 : 3);
        int rings = 7 + detail * 2;
        int sectors = 12 + detail * 4;
        int puffs = 2 + detail / 2;
        int centerX = Mth.floor(camX / CELL_SIZE);
        int centerZ = Mth.floor(camZ / CELL_SIZE);
        double drift = (ticks + partialTick) * (0.0022D + rain * 0.0012D);
        CloudForm dominant = chooseDominantForm(rain, thunder);

        for (int gx = centerX - radius; gx <= centerX + radius; gx++) {
            for (int gz = centerZ - radius; gz <= centerZ + radius; gz++) {
                long seed = ((long) gx * 341873128712L) ^ ((long) gz * 132897987541L);
                Random random = new Random(seed);
                float localCover = cover * (0.80F + random.nextFloat() * 0.38F);
                if (random.nextFloat() > localCover) continue;

                double x = gx * (double) CELL_SIZE + CELL_SIZE * 0.5D - drift;
                double z = gz * (double) CELL_SIZE + CELL_SIZE * 0.5D + drift * 0.43D;
                CloudForm form = dominant;
                if (thunder < 0.08F && rain < 0.10F) {
                    form = random.nextFloat() < 0.38F ? CloudForm.CIRRUS : CloudForm.CUMULUS;
                } else if (thunder < 0.12F && random.nextFloat() < 0.22F) {
                    form = CloudForm.CUMULUS;
                }

                int shade = Mth.clamp((int) (250.0F - thunder * 78.0F - rain * 32.0F), 112, 250);
                int red = shade;
                int green = Mth.clamp(shade + 3, 0, 255);
                int blue = Mth.clamp(shade + 7, 0, 255);
                int alpha = Mth.clamp((int) (BASE_ALPHA * opacity), 58, 166);

                renderFormation(consumer, pose, random, form, x, z, detail,
                        rings, sectors, puffs, red, green, blue, alpha, thunder);
            }
        }

        poseStack.popPose();
        buffers.endBatch(CLOUD_RENDER_TYPE);
    }

    private static CloudForm chooseDominantForm(float rain, float thunder) {
        if (thunder >= 0.12F) return CloudForm.CUMULONIMBUS;
        if (rain >= 0.10F) return CloudForm.STRATUS;
        return CloudForm.CUMULUS;
    }

    private static void renderFormation(
            VertexConsumer consumer, Matrix4f pose, Random random, CloudForm form,
            double x, double z, int detail, int rings, int sectors, int puffs,
            int red, int green, int blue, int alpha, float thunder
    ) {
        switch (form) {
            case CIRRUS -> {
                double y = CLOUD_HEIGHT + 30.0D;
                int wisps = 1 + detail / 2;
                for (int i = 0; i < wisps; i++) {
                    double ox = (random.nextDouble() - 0.5D) * 25.0D;
                    double oz = (random.nextDouble() - 0.5D) * 19.0D;
                    double length = 17.0D + random.nextDouble() * 20.0D;
                    double width = 1.8D + random.nextDouble() * 1.8D;
                    drawEllipsoid(consumer, pose, x + ox, y + i * 0.7D, z + oz,
                            length, width, 0.8D, red, green, blue, alpha / 3,
                            rings, sectors, random.nextLong());
                }
            }
            case CUMULUS -> {
                // Broad soft body with irregular lobes merging into one cloud mass.
                drawEllipsoid(consumer, pose, x, CLOUD_HEIGHT, z,
                        15.5D, 14.0D, 4.2D, red, green, blue, alpha * 3 / 4,
                        rings, sectors, random.nextLong());
                for (int i = 0; i < puffs + 1; i++) {
                    double angle = random.nextDouble() * Math.PI * 2.0D;
                    double distance = 5.0D + random.nextDouble() * 9.0D;
                    double size = 6.0D + random.nextDouble() * (4.0D + detail);
                    double ox = Math.cos(angle) * distance;
                    double oz = Math.sin(angle) * distance;
                    double oy = 2.0D + random.nextDouble() * 3.0D;
                    drawEllipsoid(consumer, pose, x + ox, CLOUD_HEIGHT + oy, z + oz,
                            size, size * (0.76D + random.nextDouble() * 0.22D),
                            size * (0.66D + random.nextDouble() * 0.16D),
                            red, green, blue, alpha, rings, sectors, random.nextLong());
                }
            }
            case STRATUS -> {
                drawEllipsoid(consumer, pose, x, CLOUD_HEIGHT - 4.0D, z,
                        36.0D, 34.0D, 2.7D, red, green, blue, alpha * 2 / 3,
                        rings, sectors, random.nextLong());
                int rolls = 1 + detail / 2;
                for (int i = 0; i < rolls; i++) {
                    double ox = (random.nextDouble() - 0.5D) * 25.0D;
                    double oz = (random.nextDouble() - 0.5D) * 25.0D;
                    drawEllipsoid(consumer, pose, x + ox, CLOUD_HEIGHT - 2.0D,
                            z + oz, 18.0D, 15.0D, 2.0D,
                            red, green, blue, alpha / 3, rings, sectors, random.nextLong());
                }
            }
            case CUMULONIMBUS -> {
                double baseY = CLOUD_HEIGHT - 4.0D;
                drawEllipsoid(consumer, pose, x, baseY, z,
                        18.0D, 17.0D, 5.5D, red, green, blue, alpha * 3 / 4,
                        rings, sectors, random.nextLong());
                int levels = 3 + detail / 2;
                for (int i = 0; i < levels; i++) {
                    double y = baseY + 6.0D + i * (5.0D + thunder * 2.0D);
                    double scale = Math.max(6.0D, 13.0D - i * 1.15D);
                    drawEllipsoid(consumer, pose, x + (random.nextDouble() - 0.5D) * 3.0D,
                            y, z + (random.nextDouble() - 0.5D) * 3.0D,
                            scale, scale * 0.92D, 6.0D + thunder * 2.0D,
                            red, green, blue, Mth.clamp(alpha - i * 5, 52, 166),
                            rings, sectors, random.nextLong());
                }
                double anvilY = baseY + 10.0D + levels * (5.0D + thunder * 2.0D);
                drawEllipsoid(consumer, pose, x + 2.0D, anvilY, z,
                        30.0D + thunder * 8.0D, 19.0D + thunder * 5.0D,
                        2.8D + thunder, red, green, blue, alpha / 2,
                        rings, sectors, random.nextLong());
            }
        }
    }

    /**
     * A gently deformed ellipsoid. Harmonic displacement removes the obvious
     * low-poly ball look without expensive per-pixel noise or textures.
     */
    private static void drawEllipsoid(
            VertexConsumer consumer, Matrix4f pose, double cx, double cy, double cz,
            double rx, double rz, double ry, int red, int green, int blue,
            int alpha, int rings, int sectors, long seed
    ) {
        double phase = (seed & 0xFFFFL) / 65535.0D * Math.PI * 2.0D;
        for (int ring = 0; ring < rings; ring++) {
            double lat0 = -Math.PI / 2.0D + Math.PI * ring / rings;
            double lat1 = -Math.PI / 2.0D + Math.PI * (ring + 1) / rings;
            for (int sector = 0; sector < sectors; sector++) {
                double lon0 = Math.PI * 2.0D * sector / sectors;
                double lon1 = Math.PI * 2.0D * (sector + 1) / sectors;
                CloudVertex a = point(cx, cy, cz, rx, rz, ry, lat0, lon0, phase);
                CloudVertex b = point(cx, cy, cz, rx, rz, ry, lat1, lon0, phase);
                CloudVertex c = point(cx, cy, cz, rx, rz, ry, lat1, lon1, phase);
                CloudVertex d = point(cx, cy, cz, rx, rz, ry, lat0, lon1, phase);
                addTriangle(consumer, pose, a, b, c, red, green, blue, alpha);
                addTriangle(consumer, pose, a, c, d, red, green, blue, alpha);
                addTriangle(consumer, pose, c, b, a, red, green, blue, alpha);
                addTriangle(consumer, pose, d, c, a, red, green, blue, alpha);
            }
        }
    }

    private record CloudVertex(float x, float y, float z, float nx, float ny, float nz) {}

    private static CloudVertex point(
            double cx, double cy, double cz, double rx, double rz, double ry,
            double latitude, double longitude, double phase
    ) {
        double cosLat = Math.cos(latitude);
        double wave = Math.sin(longitude * 3.0D + phase) * Math.cos(latitude * 2.0D) * 0.075D
                + Math.sin(longitude * 5.0D - latitude * 2.0D + phase * 0.7D) * 0.035D
                + Math.cos(longitude * 2.0D + latitude * 3.0D + phase) * 0.025D;
        double distortion = 1.0D + wave;
        double nx = cosLat * Math.cos(longitude);
        double nz = cosLat * Math.sin(longitude);
        double ny = Math.sin(latitude);
        return new CloudVertex(
                (float) (cx + nx * rx * distortion),
                (float) (cy + ny * ry * distortion),
                (float) (cz + nz * rz * distortion),
                (float) (nx / rx), (float) (ny / ry), (float) (nz / rz)
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
        float light = Mth.clamp(0.68F + Math.max(0.0F, ny) * 0.30F
                + Math.max(0.0F, nx * 0.25F + nz * 0.35F) * 0.08F, 0.52F, 1.0F);
        consumer.addVertex(pose, vertex.x, vertex.y, vertex.z)
                .setColor(
                        Mth.clamp((int) (red * light), 0, 255),
                        Mth.clamp((int) (green * light), 0, 255),
                        Mth.clamp((int) (blue * light), 0, 255),
                        alpha
                );
    }
}
