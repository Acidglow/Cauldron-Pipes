package net.minecraft.client.model.monster.piglin;

import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombifiedPiglinModel extends AbstractPiglinModel<ZombifiedPiglinRenderState> {
    public ZombifiedPiglinModel(ModelPart p_478371_) {
        super(p_478371_);
    }

    public void setupAnim(ZombifiedPiglinRenderState p_479839_) {
        super.setupAnim(p_479839_);
        AnimationUtils.animateZombieArms(this.leftArm, this.rightArm, p_479839_.isAggressive, p_479839_);
    }

    @Override
    public void setAllVisible(boolean p_478910_) {
        super.setAllVisible(p_478910_);
        this.leftSleeve.visible = p_478910_;
        this.rightSleeve.visible = p_478910_;
        this.leftPants.visible = p_478910_;
        this.rightPants.visible = p_478910_;
        this.jacket.visible = p_478910_;
    }
}
