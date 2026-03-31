package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.dolphin.DolphinModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.layers.DolphinCarryingItemLayer;
import net.minecraft.client.renderer.entity.state.DolphinRenderState;
import net.minecraft.client.renderer.entity.state.HoldingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.dolphin.Dolphin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DolphinRenderer extends AgeableMobRenderer<Dolphin, DolphinRenderState, DolphinModel> {
    private static final Identifier DOLPHIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/dolphin.png");

    public DolphinRenderer(EntityRendererProvider.Context p_173960_) {
        super(p_173960_, new DolphinModel(p_173960_.bakeLayer(ModelLayers.DOLPHIN)), new DolphinModel(p_173960_.bakeLayer(ModelLayers.DOLPHIN_BABY)), 0.7F);
        this.addLayer(new DolphinCarryingItemLayer(this));
    }

    public Identifier getTextureLocation(DolphinRenderState p_469757_) {
        return DOLPHIN_LOCATION;
    }

    public DolphinRenderState createRenderState() {
        return new DolphinRenderState();
    }

    public void extractRenderState(Dolphin p_480257_, DolphinRenderState p_364903_, float p_361483_) {
        super.extractRenderState(p_480257_, p_364903_, p_361483_);
        HoldingEntityRenderState.extractHoldingEntityRenderState(p_480257_, p_364903_, this.itemModelResolver);
        p_364903_.isMoving = p_480257_.getDeltaMovement().horizontalDistanceSqr() > 1.0E-7;
    }
}
