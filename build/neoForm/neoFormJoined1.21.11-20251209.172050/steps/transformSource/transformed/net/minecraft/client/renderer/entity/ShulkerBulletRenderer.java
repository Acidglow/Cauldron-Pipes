package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.ShulkerBulletModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ShulkerBulletRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShulkerBulletRenderer extends EntityRenderer<ShulkerBullet, ShulkerBulletRenderState> {
    private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/shulker/spark.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityTranslucent(TEXTURE_LOCATION);
    private final ShulkerBulletModel model;

    public ShulkerBulletRenderer(EntityRendererProvider.Context p_174368_) {
        super(p_174368_);
        this.model = new ShulkerBulletModel(p_174368_.bakeLayer(ModelLayers.SHULKER_BULLET));
    }

    protected int getBlockLightLevel(ShulkerBullet entity, BlockPos pos) {
        return 15;
    }

    public void submit(ShulkerBulletRenderState p_451398_, PoseStack p_433786_, SubmitNodeCollector p_433909_, CameraRenderState p_450947_) {
        p_433786_.pushPose();
        float f = p_451398_.ageInTicks;
        p_433786_.translate(0.0F, 0.15F, 0.0F);
        p_433786_.mulPose(Axis.YP.rotationDegrees(Mth.sin(f * 0.1F) * 180.0F));
        p_433786_.mulPose(Axis.XP.rotationDegrees(Mth.cos(f * 0.1F) * 180.0F));
        p_433786_.mulPose(Axis.ZP.rotationDegrees(Mth.sin(f * 0.15F) * 360.0F));
        p_433786_.scale(-0.5F, -0.5F, 0.5F);
        p_433909_.submitModel(
            this.model,
            p_451398_,
            p_433786_,
            this.model.renderType(TEXTURE_LOCATION),
            p_451398_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            p_451398_.outlineColor,
            null
        );
        p_433786_.scale(1.5F, 1.5F, 1.5F);
        p_433909_.order(1)
            .submitModel(
                this.model, p_451398_, p_433786_, RENDER_TYPE, p_451398_.lightCoords, OverlayTexture.NO_OVERLAY, 654311423, null, p_451398_.outlineColor, null
            );
        p_433786_.popPose();
        super.submit(p_451398_, p_433786_, p_433909_, p_450947_);
    }

    public ShulkerBulletRenderState createRenderState() {
        return new ShulkerBulletRenderState();
    }

    public void extractRenderState(ShulkerBullet p_364481_, ShulkerBulletRenderState p_363271_, float p_360710_) {
        super.extractRenderState(p_364481_, p_363271_, p_360710_);
        p_363271_.yRot = p_364481_.getYRot(p_360710_);
        p_363271_.xRot = p_364481_.getXRot(p_360710_);
    }
}
