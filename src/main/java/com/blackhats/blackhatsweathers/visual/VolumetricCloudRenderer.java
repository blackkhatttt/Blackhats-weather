package com.blackhats.blackhatsweathers.visual;

import com.blackhats.blackhatsweathers.BlackhatsWeathers;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import org.joml.Matrix4f;

import java.io.IOException;

/**
 * Full-screen cloud-slab renderer. The fragment shader integrates procedural
 * density along the view ray; the mesh is only a camera-centred coverage plane.
 */
@EventBusSubscriber(
        modid = BlackhatsWeathers.MOD_ID,
        value = Dist.CLIENT,
        bus = EventBusSubscriber.Bus.MOD
)
public final class VolumetricCloudRenderer {
    private static final float CLOUD_BASE = 154.0F;
    private static final float CLOUD_TOP = 276.0F;
    private static final float COVERAGE_BASE = 0.38F;

    private static volatile ShaderInstance cloudShader;

    private static final RenderType VOLUMETRIC_CLOUDS = RenderType.create(
            "blackhatsweathers:volumetric_clouds",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            256,
            false,
            false,
            RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> cloudShader))
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(false)
    );

    private VolumetricCloudRenderer() {
    }

    @SubscribeEvent
    public static void registerShaders(RegisterShadersEvent event) throws IOException {
        event.registerShader(
                new ShaderInstance(
                        event.getResourceProvider(),
                        ResourceLocation.fromNamespaceAndPath(
                                BlackhatsWeathers.MOD_ID, "volumetric_clouds"),
                        DefaultVertexFormat.POSITION_COLOR
                ),
                shader -> cloudShader = shader
        );
    }

    public static boolean renderClouds(
            ClientLevel level, int ticks, float partialTick, PoseStack poseStack,
            double camX, double camY, double camZ, WeatherQualityProfile profile
    ) {
        ShaderInstance shader = cloudShader;
        if (shader == null || level == null || profile == null
                || !profile.hasVolumetricClouds() || camY >= CLOUD_TOP) {
            return false;
        }

        float rain = level.getRainLevel(partialTick);
        float thunder = level.getThunderLevel(partialTick);
        float regionalVariation = (float) Math.sin(
                camX * 0.0011D + camZ * 0.0015D + level.getDayTime() * 0.00002D
        ) * 0.10F;
        float coverage = Mth.clamp(
                COVERAGE_BASE + regionalVariation + rain * 0.24F + thunder * 0.24F,
                0.12F, 0.94F
        );

        setUniform(shader, "CameraPosition", (float) camX, (float) camY, (float) camZ);
        setUniform(shader, "GameTime", (ticks + partialTick) * 0.0007F);
        setUniform(shader, "CloudCoverage", coverage);
        setUniform(shader, "StormFactor", Mth.clamp(thunder * 0.85F + rain * 0.25F, 0.0F, 1.0F));
        setUniform(shader, "CloudBase", CLOUD_BASE);
        setUniform(shader, "CloudTop", CLOUD_TOP);
        setUniform(shader, "RaySteps", (float) profile.getVolumetricSteps());
        setUniform(shader, "LightingQuality", profile.hasAdvancedLighting() ? 1.0F : 0.0F);

        poseStack.pushPose();
        poseStack.translate(-camX, -camY, -camZ);

        float extent = 4096.0F;
        float left = (float) camX - extent;
        float right = (float) camX + extent;
        float near = (float) camZ - extent;
        float far = (float) camZ + extent;

        Matrix4f pose = poseStack.last().pose();
        VertexConsumer consumer = Minecraft.getInstance()
                .renderBuffers().bufferSource().getBuffer(VOLUMETRIC_CLOUDS);

        consumer.addVertex(pose, left, CLOUD_BASE, near).setColor(255, 255, 255, 255);
        consumer.addVertex(pose, left, CLOUD_BASE, far).setColor(255, 255, 255, 255);
        consumer.addVertex(pose, right, CLOUD_BASE, far).setColor(255, 255, 255, 255);
        consumer.addVertex(pose, right, CLOUD_BASE, near).setColor(255, 255, 255, 255);

        poseStack.popPose();
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch(VOLUMETRIC_CLOUDS);
        return true;
    }

    private static void setUniform(ShaderInstance shader, String name, float value) {
        var uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void setUniform(
            ShaderInstance shader, String name, float x, float y, float z
    ) {
        var uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y, z);
    }
}
