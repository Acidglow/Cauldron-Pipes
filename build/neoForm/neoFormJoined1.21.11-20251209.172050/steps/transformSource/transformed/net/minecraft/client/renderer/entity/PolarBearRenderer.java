package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.polarbear.PolarBearModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.PolarBearRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PolarBearRenderer extends AgeableMobRenderer<PolarBear, PolarBearRenderState, PolarBearModel> {
    private static final Identifier BEAR_LOCATION = Identifier.withDefaultNamespace("textures/entity/bear/polarbear.png");

    public PolarBearRenderer(EntityRendererProvider.Context p_174356_) {
        super(
            p_174356_,
            new PolarBearModel(p_174356_.bakeLayer(ModelLayers.POLAR_BEAR)),
            new PolarBearModel(p_174356_.bakeLayer(ModelLayers.POLAR_BEAR_BABY)),
            0.9F
        );
    }

    public Identifier getTextureLocation(PolarBearRenderState p_360804_) {
        return BEAR_LOCATION;
    }

    public PolarBearRenderState createRenderState() {
        return new PolarBearRenderState();
    }

    public void extractRenderState(PolarBear p_478630_, PolarBearRenderState p_361616_, float p_363931_) {
        super.extractRenderState(p_478630_, p_361616_, p_363931_);
        p_361616_.standScale = p_478630_.getStandingAnimationScale(p_363931_);
    }
}
