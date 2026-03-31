package net.minecraft.client.model.effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.KineticWeapon;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpearAnimations {
    static float progress(float porgress, float min, float max) {
        return Mth.clamp(Mth.inverseLerp(porgress, min, max), 0.0F, 1.0F);
    }

    public static <T extends HumanoidRenderState> void thirdPersonHandUse(
        ModelPart arm, ModelPart head, boolean isLeftArm, ItemStack stack, T renderState
    ) {
        int i = isLeftArm ? 1 : -1;
        arm.yRot = -0.1F * i + head.yRot;
        arm.xRot = (float) (-Math.PI / 2) + head.xRot + 0.8F;
        if (renderState.isFallFlying || renderState.swimAmount > 0.0F) {
            arm.xRot -= 0.9599311F;
        }

        arm.yRot = (float) (Math.PI / 180.0) * Math.clamp((180.0F / (float)Math.PI) * arm.yRot, -60.0F, 60.0F);
        arm.xRot = (float) (Math.PI / 180.0) * Math.clamp((180.0F / (float)Math.PI) * arm.xRot, -120.0F, 30.0F);
        if (!(renderState.ticksUsingItem <= 0.0F)
            && (!renderState.isUsingItem || renderState.useItemHand == (isLeftArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND))) {
            KineticWeapon kineticweapon = stack.get(DataComponents.KINETIC_WEAPON);
            if (kineticweapon != null) {
                SpearAnimations.UseParams spearanimations$useparams = SpearAnimations.UseParams.fromKineticWeapon(kineticweapon, renderState.ticksUsingItem);
                arm.yRot = arm.yRot
                    + -i * spearanimations$useparams.swayScaleFast() * (float) (Math.PI / 180.0) * spearanimations$useparams.swayIntensity() * 1.0F;
                arm.zRot = arm.zRot
                    + -i * spearanimations$useparams.swayScaleSlow() * (float) (Math.PI / 180.0) * spearanimations$useparams.swayIntensity() * 0.5F;
                arm.xRot = arm.xRot
                    + (float) (Math.PI / 180.0)
                        * (
                            -40.0F * spearanimations$useparams.raiseProgressStart()
                                + 30.0F * spearanimations$useparams.raiseProgressMiddle()
                                + -20.0F * spearanimations$useparams.raiseProgressEnd()
                                + 20.0F * spearanimations$useparams.lowerProgress()
                                + 10.0F * spearanimations$useparams.raiseBackProgress()
                                + 0.6F * spearanimations$useparams.swayScaleSlow() * spearanimations$useparams.swayIntensity()
                        );
            }
        }
    }

    public static <S extends ArmedEntityRenderState> void thirdPersonUseItem(
        S renderState, PoseStack poseStack, float useTime, HumanoidArm arm, ItemStack item
    ) {
        KineticWeapon kineticweapon = item.get(DataComponents.KINETIC_WEAPON);
        if (kineticweapon != null && useTime != 0.0F) {
            float f = Ease.inQuad(progress(renderState.attackTime, 0.05F, 0.2F));
            float f1 = Ease.inOutExpo(progress(renderState.attackTime, 0.4F, 1.0F));
            SpearAnimations.UseParams spearanimations$useparams = SpearAnimations.UseParams.fromKineticWeapon(kineticweapon, useTime);
            int i = arm == HumanoidArm.RIGHT ? 1 : -1;
            float f2 = 1.0F - Ease.outBack(1.0F - spearanimations$useparams.raiseProgress());
            float f3 = 0.125F;
            float f4 = hitFeedbackAmount(renderState.ticksSinceKineticHitFeedback);
            poseStack.translate(0.0, -f4 * 0.4, (double)(-kineticweapon.forwardMovement() * (f2 - spearanimations$useparams.raiseBackProgress()) + f4));
            poseStack.rotateAround(
                Axis.XN.rotationDegrees(70.0F * (spearanimations$useparams.raiseProgress() - spearanimations$useparams.raiseBackProgress()) - 40.0F * (f - f1)),
                0.0F,
                -0.03125F,
                0.125F
            );
            poseStack.rotateAround(
                Axis.YP.rotationDegrees(i * 90 * (spearanimations$useparams.raiseProgress() - spearanimations$useparams.swayProgress() + 3.0F * f1 + f)),
                0.0F,
                0.0F,
                0.125F
            );
        }
    }

    public static <T extends HumanoidRenderState> void thirdPersonAttackHand(HumanoidModel<T> model, T renderState) {
        float f = renderState.attackTime;
        HumanoidArm humanoidarm = renderState.attackArm;
        model.rightArm.yRot = model.rightArm.yRot - model.body.yRot;
        model.leftArm.yRot = model.leftArm.yRot - model.body.yRot;
        model.leftArm.xRot = model.leftArm.xRot - model.body.yRot;
        float f1 = Ease.inOutSine(progress(f, 0.0F, 0.05F));
        float f2 = Ease.inQuad(progress(f, 0.05F, 0.2F));
        float f3 = Ease.inOutExpo(progress(f, 0.4F, 1.0F));
        model.getArm(humanoidarm).xRot += (90.0F * f1 - 120.0F * f2 + 30.0F * f3) * (float) (Math.PI / 180.0);
    }

    public static <S extends ArmedEntityRenderState> void thirdPersonAttackItem(S renderState, PoseStack poseStack) {
        if (!(renderState.attackTime <= 0.0F)) {
            KineticWeapon kineticweapon = renderState.getMainHandItemStack().get(DataComponents.KINETIC_WEAPON);
            float f = kineticweapon != null ? kineticweapon.forwardMovement() : 0.0F;
            float f1 = 0.125F;
            float f2 = renderState.attackTime;
            float f3 = Ease.inQuad(progress(f2, 0.05F, 0.2F));
            float f4 = Ease.inOutExpo(progress(f2, 0.4F, 1.0F));
            poseStack.rotateAround(Axis.XN.rotationDegrees(70.0F * (f3 - f4)), 0.0F, -0.125F, 0.125F);
            poseStack.translate(0.0F, f * (f3 - f4), 0.0F);
        }
    }

    private static float hitFeedbackAmount(float ticksSinceEnemyHit) {
        return 0.4F * (Ease.outQuart(progress(ticksSinceEnemyHit, 1.0F, 3.0F)) - Ease.inOutSine(progress(ticksSinceEnemyHit, 3.0F, 10.0F)));
    }

    public static void firstPersonUse(float ticksSinceEnemyHit, PoseStack poseStack, float useTime, HumanoidArm arm, ItemStack item) {
        KineticWeapon kineticweapon = item.get(DataComponents.KINETIC_WEAPON);
        if (kineticweapon != null) {
            SpearAnimations.UseParams spearanimations$useparams = SpearAnimations.UseParams.fromKineticWeapon(kineticweapon, useTime);
            int i = arm == HumanoidArm.RIGHT ? 1 : -1;
            poseStack.translate(
                (double)(
                    i
                        * (
                            spearanimations$useparams.raiseProgress() * 0.15F
                                + spearanimations$useparams.raiseProgressEnd() * -0.05F
                                + spearanimations$useparams.swayProgress() * -0.1F
                                + spearanimations$useparams.swayScaleSlow() * 0.005F
                        )
                ),
                (double)(
                    spearanimations$useparams.raiseProgress() * -0.075F
                        + spearanimations$useparams.raiseProgressMiddle() * 0.075F
                        + spearanimations$useparams.swayScaleFast() * 0.01F
                ),
                spearanimations$useparams.raiseProgressStart() * 0.05
                    + spearanimations$useparams.raiseProgressEnd() * -0.05
                    + spearanimations$useparams.swayScaleSlow() * 0.005F
            );
            poseStack.rotateAround(
                Axis.XP
                    .rotationDegrees(
                        -65.0F * Ease.inOutBack(spearanimations$useparams.raiseProgress())
                            - 35.0F * spearanimations$useparams.lowerProgress()
                            + 100.0F * spearanimations$useparams.raiseBackProgress()
                            + -0.5F * spearanimations$useparams.swayScaleFast()
                    ),
                0.0F,
                0.1F,
                0.0F
            );
            poseStack.rotateAround(
                Axis.YN
                    .rotationDegrees(
                        i
                            * (
                                -90.0F * progress(spearanimations$useparams.raiseProgress(), 0.5F, 0.55F)
                                    + 90.0F * spearanimations$useparams.swayProgress()
                                    + 2.0F * spearanimations$useparams.swayScaleSlow()
                            )
                    ),
                i * 0.15F,
                0.0F,
                0.0F
            );
            poseStack.translate(0.0F, -hitFeedbackAmount(ticksSinceEnemyHit), 0.0F);
        }
    }

    public static void firstPersonAttack(float swingProgress, PoseStack poseStack, int offset, HumanoidArm arm) {
        float f = Ease.inOutSine(progress(swingProgress, 0.0F, 0.05F));
        float f1 = Ease.outBack(progress(swingProgress, 0.05F, 0.2F));
        float f2 = Ease.inOutExpo(progress(swingProgress, 0.4F, 1.0F));
        poseStack.translate(offset * 0.1F * (f - f1), -0.075F * (f - f2), 0.65F * (f - f1));
        poseStack.mulPose(Axis.XP.rotationDegrees(-70.0F * (f - f2)));
        poseStack.translate(0.0, 0.0, -0.25 * (f2 - f1));
    }

    @OnlyIn(Dist.CLIENT)
    record UseParams(
        float raiseProgress,
        float raiseProgressStart,
        float raiseProgressMiddle,
        float raiseProgressEnd,
        float swayProgress,
        float lowerProgress,
        float raiseBackProgress,
        float swayIntensity,
        float swayScaleSlow,
        float swayScaleFast
    ) {
        public static SpearAnimations.UseParams fromKineticWeapon(KineticWeapon kineticWeapon, float useTime) {
            int i = kineticWeapon.delayTicks();
            int j = kineticWeapon.dismountConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + i;
            int k = j - 20;
            int l = kineticWeapon.knockbackConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + i;
            int i1 = l - 40;
            int j1 = kineticWeapon.damageConditions().map(KineticWeapon.Condition::maxDurationTicks).orElse(0) + i;
            float f = SpearAnimations.progress(useTime, 0.0F, i);
            float f1 = SpearAnimations.progress(f, 0.0F, 0.5F);
            float f2 = SpearAnimations.progress(f, 0.5F, 0.8F);
            float f3 = SpearAnimations.progress(f, 0.8F, 1.0F);
            float f4 = SpearAnimations.progress(useTime, k, i1);
            float f5 = Ease.outCubic(Ease.inOutElastic(SpearAnimations.progress(useTime - 20.0F, i1, l)));
            float f6 = SpearAnimations.progress(useTime, j1 - 5, j1);
            float f7 = 2.0F * Ease.outCirc(f4) - 2.0F * Ease.inCirc(f6);
            float f8 = Mth.sin(useTime * 19.0F * (float) (Math.PI / 180.0)) * f7;
            float f9 = Mth.sin(useTime * 30.0F * (float) (Math.PI / 180.0)) * f7;
            return new SpearAnimations.UseParams(f, f1, f2, f3, f4, f5, f6, f7, f8, f9);
        }
    }
}
