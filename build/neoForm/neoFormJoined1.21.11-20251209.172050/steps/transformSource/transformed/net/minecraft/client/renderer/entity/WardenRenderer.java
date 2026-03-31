package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.warden.WardenModel;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.state.WardenRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WardenRenderer extends MobRenderer<Warden, WardenRenderState, WardenModel> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/warden/warden.png");
    private static final Identifier BIOLUMINESCENT_LAYER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/warden/warden_bioluminescent_layer.png");
    private static final Identifier HEART_TEXTURE = Identifier.withDefaultNamespace("textures/entity/warden/warden_heart.png");
    private static final Identifier PULSATING_SPOTS_TEXTURE_1 = Identifier.withDefaultNamespace("textures/entity/warden/warden_pulsating_spots_1.png");
    private static final Identifier PULSATING_SPOTS_TEXTURE_2 = Identifier.withDefaultNamespace("textures/entity/warden/warden_pulsating_spots_2.png");

    public WardenRenderer(EntityRendererProvider.Context p_234787_) {
        super(p_234787_, new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN)), 0.9F);
        WardenModel wardenmodel = new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN_BIOLUMINESCENT));
        WardenModel wardenmodel1 = new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN_PULSATING_SPOTS));
        WardenModel wardenmodel2 = new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN_TENDRILS));
        WardenModel wardenmodel3 = new WardenModel(p_234787_.bakeLayer(ModelLayers.WARDEN_HEART));
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this, p_465649_ -> BIOLUMINESCENT_LAYER_TEXTURE, (p_360647_, p_234810_) -> 1.0F, wardenmodel, RenderTypes::entityTranslucentEmissive, false
            )
        );
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this,
                p_465644_ -> PULSATING_SPOTS_TEXTURE_1,
                (p_465647_, p_465648_) -> Math.max(0.0F, Mth.cos(p_465648_ * 0.045F) * 0.25F),
                wardenmodel1,
                RenderTypes::entityTranslucentEmissive,
                false
            )
        );
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this,
                p_465645_ -> PULSATING_SPOTS_TEXTURE_2,
                (p_465651_, p_465652_) -> Math.max(0.0F, Mth.cos(p_465652_ * 0.045F + (float) Math.PI) * 0.25F),
                wardenmodel1,
                RenderTypes::entityTranslucentEmissive,
                false
            )
        );
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this, p_465650_ -> TEXTURE, (p_359288_, p_359289_) -> p_359288_.tendrilAnimation, wardenmodel2, RenderTypes::entityTranslucentEmissive, false
            )
        );
        this.addLayer(
            new LivingEntityEmissiveLayer<>(
                this,
                p_465646_ -> HEART_TEXTURE,
                (p_359290_, p_359291_) -> p_359290_.heartAnimation,
                wardenmodel3,
                RenderTypes::entityTranslucentEmissive,
                false
            )
        );
    }

    public Identifier getTextureLocation(WardenRenderState p_362254_) {
        return TEXTURE;
    }

    public WardenRenderState createRenderState() {
        return new WardenRenderState();
    }

    public void extractRenderState(Warden p_362437_, WardenRenderState p_362253_, float p_360809_) {
        super.extractRenderState(p_362437_, p_362253_, p_360809_);
        p_362253_.tendrilAnimation = p_362437_.getTendrilAnimation(p_360809_);
        p_362253_.heartAnimation = p_362437_.getHeartAnimation(p_360809_);
        p_362253_.roarAnimationState.copyFrom(p_362437_.roarAnimationState);
        p_362253_.sniffAnimationState.copyFrom(p_362437_.sniffAnimationState);
        p_362253_.emergeAnimationState.copyFrom(p_362437_.emergeAnimationState);
        p_362253_.diggingAnimationState.copyFrom(p_362437_.diggingAnimationState);
        p_362253_.attackAnimationState.copyFrom(p_362437_.attackAnimationState);
        p_362253_.sonicBoomAnimationState.copyFrom(p_362437_.sonicBoomAnimationState);
    }
}
