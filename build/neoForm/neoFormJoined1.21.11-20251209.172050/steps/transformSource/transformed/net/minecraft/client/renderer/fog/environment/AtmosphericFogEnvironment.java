package net.minecraft.client.renderer.fog.environment;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.PanoramicScreenshotParameters;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class AtmosphericFogEnvironment extends FogEnvironment {
    private static final int MIN_RAIN_FOG_SKY_LIGHT = 8;
    private static final float RAIN_FOG_START_OFFSET = -160.0F;
    private static final float RAIN_FOG_END_OFFSET = -256.0F;
    private float rainFogMultiplier;

    @Override
    public int getBaseColor(ClientLevel p_461053_, Camera p_461207_, int p_460725_, float p_460769_) {
        int i = p_461207_.attributeProbe().getValue(EnvironmentAttributes.FOG_COLOR, p_460769_);
        if (p_460725_ >= 4) {
            float f = p_461207_.attributeProbe().getValue(EnvironmentAttributes.SUN_ANGLE, p_460769_) * (float) (Math.PI / 180.0);
            float f1 = Mth.sin(f) > 0.0F ? -1.0F : 1.0F;
            PanoramicScreenshotParameters panoramicscreenshotparameters = Minecraft.getInstance().gameRenderer.getPanoramicScreenshotParameters();
            Vector3fc vector3fc = panoramicscreenshotparameters != null ? panoramicscreenshotparameters.forwardVector() : p_461207_.forwardVector();
            float f2 = vector3fc.dot(f1, 0.0F, 0.0F);
            if (f2 > 0.0F) {
                int j = p_461207_.attributeProbe().getValue(EnvironmentAttributes.SUNRISE_SUNSET_COLOR, p_460769_);
                float f3 = ARGB.alphaFloat(j);
                if (f3 > 0.0F) {
                    i = ARGB.srgbLerp(f2 * f3, i, ARGB.opaque(j));
                }
            }
        }

        int k = p_461207_.attributeProbe().getValue(EnvironmentAttributes.SKY_COLOR, p_460769_);
        k = applyWeatherDarken(k, p_461053_.getRainLevel(p_460769_), p_461053_.getThunderLevel(p_460769_));
        float f4 = Math.min(p_461207_.attributeProbe().getValue(EnvironmentAttributes.SKY_FOG_END_DISTANCE, p_460769_) / 16.0F, (float)p_460725_);
        float f5 = Mth.clampedLerp(f4 / 32.0F, 0.25F, 1.0F);
        f5 = 1.0F - (float)Math.pow(f5, 0.25);
        return ARGB.srgbLerp(f5, i, k);
    }

    private static int applyWeatherDarken(int skyColor, float rainLevel, float thunderLevel) {
        if (rainLevel > 0.0F) {
            float f = 1.0F - rainLevel * 0.5F;
            float f1 = 1.0F - rainLevel * 0.4F;
            skyColor = ARGB.scaleRGB(skyColor, f, f, f1);
        }

        if (thunderLevel > 0.0F) {
            skyColor = ARGB.scaleRGB(skyColor, 1.0F - thunderLevel * 0.5F);
        }

        return skyColor;
    }

    @Override
    public void setupFog(FogData p_423515_, Camera p_455012_, ClientLevel p_423511_, float p_423456_, DeltaTracker p_423432_) {
        this.updateRainFogState(p_455012_, p_423511_, p_423432_);
        float f = p_423432_.getGameTimeDeltaPartialTick(false);
        p_423515_.environmentalStart = p_455012_.attributeProbe().getValue(EnvironmentAttributes.FOG_START_DISTANCE, f);
        p_423515_.environmentalEnd = p_455012_.attributeProbe().getValue(EnvironmentAttributes.FOG_END_DISTANCE, f);
        p_423515_.environmentalStart = p_423515_.environmentalStart + -160.0F * this.rainFogMultiplier;
        float f1 = Math.min(96.0F, p_423515_.environmentalEnd);
        p_423515_.environmentalEnd = Math.max(f1, p_423515_.environmentalEnd + -256.0F * this.rainFogMultiplier);
        p_423515_.skyEnd = Math.min(p_423456_, p_455012_.attributeProbe().getValue(EnvironmentAttributes.SKY_FOG_END_DISTANCE, f));
        p_423515_.cloudEnd = Math.min(
            (float)(Minecraft.getInstance().options.cloudRange().get() * 16),
            p_455012_.attributeProbe().getValue(EnvironmentAttributes.CLOUD_FOG_END_DISTANCE, f)
        );
        if (Minecraft.getInstance().gui.getBossOverlay().shouldCreateWorldFog()) {
            p_423515_.environmentalStart = Math.min(p_423515_.environmentalStart, 10.0F);
            p_423515_.environmentalEnd = Math.min(p_423515_.environmentalEnd, 96.0F);
            p_423515_.skyEnd = p_423515_.environmentalEnd;
            p_423515_.cloudEnd = p_423515_.environmentalEnd;
        }
    }

    private void updateRainFogState(Camera camera, ClientLevel level, DeltaTracker deltaTracker) {
        BlockPos blockpos = camera.blockPosition();
        Biome biome = level.getBiome(blockpos).value();
        float f = deltaTracker.getGameTimeDeltaTicks();
        float f1 = deltaTracker.getGameTimeDeltaPartialTick(false);
        boolean flag = biome.hasPrecipitation();
        float f2 = Mth.clamp((level.getLightEngine().getLayerListener(LightLayer.SKY).getLightValue(blockpos) - 8.0F) / 7.0F, 0.0F, 1.0F);
        float f3 = level.getRainLevel(f1) * f2 * (flag ? 1.0F : 0.5F);
        this.rainFogMultiplier = this.rainFogMultiplier + (f3 - this.rainFogMultiplier) * f * 0.2F;
    }

    @Override
    public boolean isApplicable(@Nullable FogType p_423642_, Entity p_423662_) {
        return p_423642_ == FogType.ATMOSPHERIC;
    }
}
