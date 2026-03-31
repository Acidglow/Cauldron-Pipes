package net.minecraft.client.model.monster.zombie;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ZombieRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractZombieModel<S extends ZombieRenderState> extends HumanoidModel<S> {
    protected AbstractZombieModel(ModelPart p_478867_) {
        super(p_478867_);
    }

    public void setupAnim(S p_479495_) {
        super.setupAnim(p_479495_);
        AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, p_479495_.isAggressive, p_479495_);
    }
}
