package net.minecraft.client.renderer.fog.environment;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class WaterFogEnvironment extends FogEnvironment {
    @Override
    public void setupFog(FogData p_423613_, Camera p_454826_, ClientLevel p_423530_, float p_423585_, DeltaTracker p_423472_) {
        float f = p_423472_.getGameTimeDeltaPartialTick(false);
        p_423613_.environmentalStart = p_454826_.attributeProbe().getValue(EnvironmentAttributes.WATER_FOG_START_DISTANCE, f);
        p_423613_.environmentalEnd = p_454826_.attributeProbe().getValue(EnvironmentAttributes.WATER_FOG_END_DISTANCE, f);
        if (p_454826_.entity() instanceof LocalPlayer localplayer) {
            p_423613_.environmentalEnd = p_423613_.environmentalEnd * Math.max(0.25F, localplayer.getWaterVision());
        }

        p_423613_.skyEnd = p_423613_.environmentalEnd;
        p_423613_.cloudEnd = p_423613_.environmentalEnd;
    }

    @Override
    public boolean isApplicable(@Nullable FogType p_423569_, Entity p_423444_) {
        return p_423569_ == FogType.WATER;
    }

    @Override
    public int getBaseColor(ClientLevel p_423612_, Camera p_423473_, int p_423430_, float p_423622_) {
        return p_423473_.attributeProbe().getValue(EnvironmentAttributes.WATER_FOG_COLOR, p_423622_);
    }
}
