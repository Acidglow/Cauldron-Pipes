package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.feline.OcelotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.FelineRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OcelotRenderer extends AgeableMobRenderer<Ocelot, FelineRenderState, OcelotModel> {
    private static final Identifier CAT_OCELOT_LOCATION = Identifier.withDefaultNamespace("textures/entity/cat/ocelot.png");

    public OcelotRenderer(EntityRendererProvider.Context p_174330_) {
        super(p_174330_, new OcelotModel(p_174330_.bakeLayer(ModelLayers.OCELOT)), new OcelotModel(p_174330_.bakeLayer(ModelLayers.OCELOT_BABY)), 0.4F);
    }

    public Identifier getTextureLocation(FelineRenderState p_467520_) {
        return CAT_OCELOT_LOCATION;
    }

    public FelineRenderState createRenderState() {
        return new FelineRenderState();
    }

    public void extractRenderState(Ocelot p_479269_, FelineRenderState p_364547_, float p_363203_) {
        super.extractRenderState(p_479269_, p_364547_, p_363203_);
        p_364547_.isCrouching = p_479269_.isCrouching();
        p_364547_.isSprinting = p_479269_.isSprinting();
    }
}
