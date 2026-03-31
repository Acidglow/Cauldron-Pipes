package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.monster.zombie.ZombieModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.SwingAnimationType;
import net.minecraft.world.item.component.SwingAnimation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractZombieRenderer<T extends Zombie, S extends ZombieRenderState, M extends ZombieModel<S>> extends HumanoidMobRenderer<T, S, M> {
    private static final Identifier ZOMBIE_LOCATION = Identifier.withDefaultNamespace("textures/entity/zombie/zombie.png");

    protected AbstractZombieRenderer(EntityRendererProvider.Context context, M adultModel, M babyModel, ArmorModelSet<M> adultArmorModels, ArmorModelSet<M> babyArmorModels) {
        super(context, adultModel, babyModel, 0.5F);
        this.addLayer(new HumanoidArmorLayer<>(this, adultArmorModels, babyArmorModels, context.getEquipmentRenderer()));
    }

    public Identifier getTextureLocation(S p_467832_) {
        return ZOMBIE_LOCATION;
    }

    public void extractRenderState(T p_478689_, S p_365238_, float p_361332_) {
        super.extractRenderState(p_478689_, p_365238_, p_361332_);
        p_365238_.isAggressive = p_478689_.isAggressive();
        p_365238_.isConverting = p_478689_.isUnderWaterConverting();
    }

    protected boolean isShaking(S p_363333_) {
        return super.isShaking(p_363333_) || p_363333_.isConverting;
    }

    protected HumanoidModel.ArmPose getArmPose(T p_478236_, HumanoidArm p_454981_) {
        SwingAnimation swinganimation = p_478236_.getItemHeldByArm(p_454981_.getOpposite()).get(DataComponents.SWING_ANIMATION);
        return swinganimation != null && swinganimation.type() == SwingAnimationType.STAB
            ? HumanoidModel.ArmPose.SPEAR
            : super.getArmPose(p_478236_, p_454981_);
    }
}
