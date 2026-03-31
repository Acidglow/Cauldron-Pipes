package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.WindChargeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WindChargeRenderer extends EntityRenderer<AbstractWindCharge, EntityRenderState> {
    private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/projectiles/wind_charge.png");
    private final WindChargeModel model;

    public WindChargeRenderer(EntityRendererProvider.Context p_312557_) {
        super(p_312557_);
        this.model = new WindChargeModel(p_312557_.bakeLayer(ModelLayers.WIND_CHARGE));
    }

    @Override
    public void submit(EntityRenderState p_434443_, PoseStack p_435406_, SubmitNodeCollector p_433935_, CameraRenderState p_451325_) {
        p_433935_.submitModel(
            this.model,
            p_434443_,
            p_435406_,
            RenderTypes.breezeWind(TEXTURE_LOCATION, this.xOffset(p_434443_.ageInTicks) % 1.0F, 0.0F),
            p_434443_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            p_434443_.outlineColor,
            null
        );
        super.submit(p_434443_, p_435406_, p_433935_, p_451325_);
    }

    protected float xOffset(float tickCount) {
        return tickCount * 0.03F;
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
