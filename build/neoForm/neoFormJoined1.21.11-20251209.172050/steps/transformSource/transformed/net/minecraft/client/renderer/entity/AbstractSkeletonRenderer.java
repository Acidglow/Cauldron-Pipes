package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.skeleton.SkeletonModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.SkeletonRenderState;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSkeletonRenderer<T extends AbstractSkeleton, S extends SkeletonRenderState> extends HumanoidMobRenderer<T, S, SkeletonModel<S>> {
    public AbstractSkeletonRenderer(EntityRendererProvider.Context context, ModelLayerLocation skeletonLayer, ArmorModelSet<ModelLayerLocation> armorModelSet) {
        this(context, armorModelSet, new SkeletonModel<>(context.bakeLayer(skeletonLayer)));
    }

    public AbstractSkeletonRenderer(EntityRendererProvider.Context context, ArmorModelSet<ModelLayerLocation> armorModelSet, SkeletonModel<S> model) {
        super(context, model, 0.5F);
        this.addLayer(
            new HumanoidArmorLayer<>(this, ArmorModelSet.bake(armorModelSet, context.getModelSet(), SkeletonModel::new), context.getEquipmentRenderer())
        );
    }

    public void extractRenderState(T p_478693_, S p_364836_, float p_362389_) {
        super.extractRenderState(p_478693_, p_364836_, p_362389_);
        p_364836_.isAggressive = p_478693_.isAggressive();
        p_364836_.isShaking = p_478693_.isShaking();
        p_364836_.isHoldingBow = p_478693_.getMainHandItem().is(Items.BOW);
    }

    protected boolean isShaking(S p_364410_) {
        return p_364410_.isShaking;
    }

    protected HumanoidModel.ArmPose getArmPose(T p_477991_, HumanoidArm p_389423_) {
        return p_477991_.getMainArm() == p_389423_ && p_477991_.isAggressive() && p_477991_.getMainHandItem().is(Items.BOW)
            ? HumanoidModel.ArmPose.BOW_AND_ARROW
            : super.getArmPose(p_477991_, p_389423_);
    }
}
