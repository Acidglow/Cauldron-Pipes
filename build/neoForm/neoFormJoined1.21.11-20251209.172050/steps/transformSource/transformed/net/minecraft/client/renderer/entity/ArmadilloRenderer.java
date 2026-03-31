package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.armadillo.ArmadilloModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.ArmadilloRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ArmadilloRenderer extends AgeableMobRenderer<Armadillo, ArmadilloRenderState, ArmadilloModel> {
    private static final Identifier ARMADILLO_LOCATION = Identifier.withDefaultNamespace("textures/entity/armadillo.png");

    public ArmadilloRenderer(EntityRendererProvider.Context p_316729_) {
        super(
            p_316729_,
            new ArmadilloModel(p_316729_.bakeLayer(ModelLayers.ARMADILLO)),
            new ArmadilloModel(p_316729_.bakeLayer(ModelLayers.ARMADILLO_BABY)),
            0.4F
        );
    }

    public Identifier getTextureLocation(ArmadilloRenderState p_469892_) {
        return ARMADILLO_LOCATION;
    }

    public ArmadilloRenderState createRenderState() {
        return new ArmadilloRenderState();
    }

    public void extractRenderState(Armadillo p_361126_, ArmadilloRenderState p_360513_, float p_364607_) {
        super.extractRenderState(p_361126_, p_360513_, p_364607_);
        p_360513_.isHidingInShell = p_361126_.shouldHideInShell();
        p_360513_.peekAnimationState.copyFrom(p_361126_.peekAnimationState);
        p_360513_.rollOutAnimationState.copyFrom(p_361126_.rollOutAnimationState);
        p_360513_.rollUpAnimationState.copyFrom(p_361126_.rollUpAnimationState);
    }
}
