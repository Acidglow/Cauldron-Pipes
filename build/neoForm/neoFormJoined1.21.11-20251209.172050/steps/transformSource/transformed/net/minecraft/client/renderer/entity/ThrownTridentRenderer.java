package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.projectile.TridentModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ThrownTridentRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ThrownTridentRenderer extends EntityRenderer<ThrownTrident, ThrownTridentRenderState> {
    public static final Identifier TRIDENT_LOCATION = Identifier.withDefaultNamespace("textures/entity/trident.png");
    private final TridentModel model;

    public ThrownTridentRenderer(EntityRendererProvider.Context p_174420_) {
        super(p_174420_);
        this.model = new TridentModel(p_174420_.bakeLayer(ModelLayers.TRIDENT));
    }

    public void submit(ThrownTridentRenderState p_434752_, PoseStack p_435381_, SubmitNodeCollector p_435880_, CameraRenderState p_451376_) {
        p_435381_.pushPose();
        p_435381_.mulPose(Axis.YP.rotationDegrees(p_434752_.yRot - 90.0F));
        p_435381_.mulPose(Axis.ZP.rotationDegrees(p_434752_.xRot + 90.0F));
        List<RenderType> list = ItemRenderer.getFoilRenderTypes(this.model.renderType(TRIDENT_LOCATION), false, p_434752_.isFoil);

        for (int i = 0; i < list.size(); i++) {
            p_435880_.order(i)
                .submitModel(
                    this.model, Unit.INSTANCE, p_435381_, list.get(i), p_434752_.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, p_434752_.outlineColor, null
                );
        }

        p_435381_.popPose();
        super.submit(p_434752_, p_435381_, p_435880_, p_451376_);
    }

    public ThrownTridentRenderState createRenderState() {
        return new ThrownTridentRenderState();
    }

    public void extractRenderState(ThrownTrident p_481763_, ThrownTridentRenderState p_360843_, float p_361066_) {
        super.extractRenderState(p_481763_, p_360843_, p_361066_);
        p_360843_.yRot = p_481763_.getYRot(p_361066_);
        p_360843_.xRot = p_481763_.getXRot(p_361066_);
        p_360843_.isFoil = p_481763_.isFoil();
    }
}
