package net.minecraft.client.model.object.boat;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.BoatRenderState;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractBoatModel extends EntityModel<BoatRenderState> {
    private final ModelPart leftPaddle;
    private final ModelPart rightPaddle;

    public AbstractBoatModel(ModelPart p_479531_) {
        super(p_479531_);
        this.leftPaddle = p_479531_.getChild("left_paddle");
        this.rightPaddle = p_479531_.getChild("right_paddle");
    }

    public void setupAnim(BoatRenderState p_480952_) {
        super.setupAnim(p_480952_);
        animatePaddle(p_480952_.rowingTimeLeft, 0, this.leftPaddle);
        animatePaddle(p_480952_.rowingTimeRight, 1, this.rightPaddle);
    }

    private static void animatePaddle(float rowingTime, int side, ModelPart paddle) {
        paddle.xRot = Mth.clampedLerp((Mth.sin(-rowingTime) + 1.0F) / 2.0F, (float) (-Math.PI / 3), (float) (-Math.PI / 12));
        paddle.yRot = Mth.clampedLerp((Mth.sin(-rowingTime + 1.0F) + 1.0F) / 2.0F, (float) (-Math.PI / 4), (float) (Math.PI / 4));
        if (side == 1) {
            paddle.yRot = (float) Math.PI - paddle.yRot;
        }
    }
}
