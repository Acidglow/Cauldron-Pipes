package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.piglin.PiglinModel;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.PiglinRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.item.CrossbowItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PiglinRenderer extends HumanoidMobRenderer<AbstractPiglin, PiglinRenderState, PiglinModel> {
    private static final Identifier PIGLIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/piglin/piglin.png");
    private static final Identifier PIGLIN_BRUTE_LOCATION = Identifier.withDefaultNamespace("textures/entity/piglin/piglin_brute.png");
    public static final CustomHeadLayer.Transforms PIGLIN_CUSTOM_HEAD_TRANSFORMS = new CustomHeadLayer.Transforms(0.0F, 0.0F, 1.0019531F);

    public PiglinRenderer(
        EntityRendererProvider.Context context,
        ModelLayerLocation adultModelLayer,
        ModelLayerLocation babyModelLayer,
        ArmorModelSet<ModelLayerLocation> armorModelSet,
        ArmorModelSet<ModelLayerLocation> babyArmorModelSet
    ) {
        super(context, new PiglinModel(context.bakeLayer(adultModelLayer)), new PiglinModel(context.bakeLayer(babyModelLayer)), 0.5F, PIGLIN_CUSTOM_HEAD_TRANSFORMS);
        this.addLayer(
            new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(armorModelSet, context.getModelSet(), PiglinModel::new),
                ArmorModelSet.bake(babyArmorModelSet, context.getModelSet(), PiglinModel::new),
                context.getEquipmentRenderer()
            )
        );
    }

    public Identifier getTextureLocation(PiglinRenderState p_467417_) {
        return p_467417_.isBrute ? PIGLIN_BRUTE_LOCATION : PIGLIN_LOCATION;
    }

    public PiglinRenderState createRenderState() {
        return new PiglinRenderState();
    }

    public void extractRenderState(AbstractPiglin p_361113_, PiglinRenderState p_364996_, float p_362352_) {
        super.extractRenderState(p_361113_, p_364996_, p_362352_);
        p_364996_.isBrute = p_361113_.getType() == EntityType.PIGLIN_BRUTE;
        p_364996_.armPose = p_361113_.getArmPose();
        p_364996_.maxCrossbowChageDuration = CrossbowItem.getChargeDuration(p_361113_.getUseItem(), p_361113_);
        p_364996_.isConverting = p_361113_.isConverting();
    }

    protected boolean isShaking(PiglinRenderState p_360965_) {
        return super.isShaking(p_360965_) || p_360965_.isConverting;
    }
}
