package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.MinecartTntRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.vehicle.minecart.MinecartTNT;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TntMinecartRenderer extends AbstractMinecartRenderer<MinecartTNT, MinecartTntRenderState> {
    public TntMinecartRenderer(EntityRendererProvider.Context p_174424_) {
        super(p_174424_, ModelLayers.TNT_MINECART);
    }

    protected void submitMinecartContents(
        MinecartTntRenderState p_435255_, BlockState p_434730_, PoseStack p_435268_, SubmitNodeCollector p_435423_, int p_433197_
    ) {
        float f = p_435255_.fuseRemainingInTicks;
        if (f > -1.0F && f < 10.0F) {
            float f1 = 1.0F - f / 10.0F;
            f1 = Mth.clamp(f1, 0.0F, 1.0F);
            f1 *= f1;
            f1 *= f1;
            float f2 = 1.0F + f1 * 0.3F;
            p_435268_.scale(f2, f2, f2);
        }

        submitWhiteSolidBlock(p_434730_, p_435268_, p_435423_, p_433197_, f > -1.0F && (int)f / 5 % 2 == 0, p_435255_.outlineColor);
    }

    public static void submitWhiteSolidBlock(
        BlockState blockState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, boolean flash, int outlineColor
    ) {
        int i;
        if (flash) {
            i = OverlayTexture.pack(OverlayTexture.u(1.0F), 10);
        } else {
            i = OverlayTexture.NO_OVERLAY;
        }

        nodeCollector.submitBlock(poseStack, blockState, packedLight, i, outlineColor);
    }

    public MinecartTntRenderState createRenderState() {
        return new MinecartTntRenderState();
    }

    public void extractRenderState(MinecartTNT p_481076_, MinecartTntRenderState p_363447_, float p_364875_) {
        super.extractRenderState(p_481076_, p_363447_, p_364875_);
        p_363447_.fuseRemainingInTicks = p_481076_.getFuse() > -1 ? p_481076_.getFuse() - p_364875_ + 1.0F : -1.0F;
    }
}
