package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Ease;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class HumanoidModel<T extends HumanoidRenderState> extends EntityModel<T> implements ArmedModel<T>, HeadedModel {
    public static final MeshTransformer BABY_TRANSFORMER = new BabyModelTransform(true, 16.0F, 0.0F, 2.0F, 2.0F, 24.0F, Set.of("head"));
    public static final float OVERLAY_SCALE = 0.25F;
    public static final float HAT_OVERLAY_SCALE = 0.5F;
    public static final float LEGGINGS_OVERLAY_SCALE = -0.1F;
    private static final float DUCK_WALK_ROTATION = 0.005F;
    private static final float SPYGLASS_ARM_ROT_Y = (float) (Math.PI / 12);
    private static final float SPYGLASS_ARM_ROT_X = 1.9198622F;
    private static final float SPYGLASS_ARM_CROUCH_ROT_X = (float) (Math.PI / 12);
    private static final float HIGHEST_SHIELD_BLOCKING_ANGLE = (float) (-Math.PI * 4.0 / 9.0);
    private static final float LOWEST_SHIELD_BLOCKING_ANGLE = 0.43633232F;
    private static final float HORIZONTAL_SHIELD_MOVEMENT_LIMIT = (float) (Math.PI / 6);
    public static final float TOOT_HORN_XROT_BASE = 1.4835298F;
    public static final float TOOT_HORN_YROT_BASE = (float) (Math.PI / 6);
    public final ModelPart head;
    /**
     * The Biped's Headwear. Used for the outer layer of player skins.
     */
    public final ModelPart hat;
    public final ModelPart body;
    /**
     * The Biped's Right Arm
     */
    public final ModelPart rightArm;
    /**
     * The Biped's Left Arm
     */
    public final ModelPart leftArm;
    /**
     * The Biped's Right Leg
     */
    public final ModelPart rightLeg;
    /**
     * The Biped's Left Leg
     */
    public final ModelPart leftLeg;

    public HumanoidModel(ModelPart root) {
        this(root, RenderTypes::entityCutoutNoCull);
    }

    public HumanoidModel(ModelPart root, Function<Identifier, RenderType> renderType) {
        super(root, renderType);
        this.head = root.getChild("head");
        this.hat = this.head.getChild("hat");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
    }

    public static MeshDefinition createMesh(CubeDeformation cubeDeformation, float yOffset) {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition partdefinition1 = partdefinition.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, cubeDeformation),
            PartPose.offset(0.0F, 0.0F + yOffset, 0.0F)
        );
        partdefinition1.addOrReplaceChild(
            "hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, cubeDeformation.extend(0.5F)), PartPose.ZERO
        );
        partdefinition.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(16, 16).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, cubeDeformation),
            PartPose.offset(0.0F, 0.0F + yOffset, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "right_arm",
            CubeListBuilder.create().texOffs(40, 16).addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation),
            PartPose.offset(-5.0F, 2.0F + yOffset, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "left_arm",
            CubeListBuilder.create().texOffs(40, 16).mirror().addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation),
            PartPose.offset(5.0F, 2.0F + yOffset, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation),
            PartPose.offset(-1.9F, 12.0F + yOffset, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation),
            PartPose.offset(1.9F, 12.0F + yOffset, 0.0F)
        );
        return meshdefinition;
    }

    public static ArmorModelSet<MeshDefinition> createArmorMeshSet(CubeDeformation innerArmorCubeDeformation, CubeDeformation outerArmorCubeDeformation) {
        return createArmorMeshSet(HumanoidModel::createBaseArmorMesh, innerArmorCubeDeformation, outerArmorCubeDeformation);
    }

    protected static ArmorModelSet<MeshDefinition> createArmorMeshSet(
        Function<CubeDeformation, MeshDefinition> meshCreator, CubeDeformation innerCubeDeformation, CubeDeformation outerCubeDeformation
    ) {
        MeshDefinition meshdefinition = meshCreator.apply(outerCubeDeformation);
        meshdefinition.getRoot().retainPartsAndChildren(Set.of("head"));
        MeshDefinition meshdefinition1 = meshCreator.apply(outerCubeDeformation);
        meshdefinition1.getRoot().retainExactParts(Set.of("body", "left_arm", "right_arm"));
        MeshDefinition meshdefinition2 = meshCreator.apply(innerCubeDeformation);
        meshdefinition2.getRoot().retainExactParts(Set.of("left_leg", "right_leg", "body"));
        MeshDefinition meshdefinition3 = meshCreator.apply(outerCubeDeformation);
        meshdefinition3.getRoot().retainExactParts(Set.of("left_leg", "right_leg"));
        return new ArmorModelSet<>(meshdefinition, meshdefinition1, meshdefinition2, meshdefinition3);
    }

    private static MeshDefinition createBaseArmorMesh(CubeDeformation cubeDeformation) {
        MeshDefinition meshdefinition = createMesh(cubeDeformation, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create().texOffs(0, 16).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(-0.1F)),
            PartPose.offset(-1.9F, 12.0F, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create().texOffs(0, 16).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, cubeDeformation.extend(-0.1F)),
            PartPose.offset(1.9F, 12.0F, 0.0F)
        );
        return meshdefinition;
    }

    public void setupAnim(T p_361833_) {
        super.setupAnim(p_361833_);
        HumanoidModel.ArmPose humanoidmodel$armpose = p_361833_.leftArmPose;
        HumanoidModel.ArmPose humanoidmodel$armpose1 = p_361833_.rightArmPose;
        float f = p_361833_.swimAmount;
        boolean flag = p_361833_.isFallFlying;
        this.head.xRot = p_361833_.xRot * (float) (Math.PI / 180.0);
        this.head.yRot = p_361833_.yRot * (float) (Math.PI / 180.0);
        if (flag) {
            this.head.xRot = (float) (-Math.PI / 4);
        } else if (f > 0.0F) {
            this.head.xRot = Mth.rotLerpRad(f, this.head.xRot, (float) (-Math.PI / 4));
        }

        float f1 = p_361833_.walkAnimationPos;
        float f2 = p_361833_.walkAnimationSpeed;
        this.rightArm.xRot = Mth.cos(f1 * 0.6662F + (float) Math.PI) * 2.0F * f2 * 0.5F / p_361833_.speedValue;
        this.leftArm.xRot = Mth.cos(f1 * 0.6662F) * 2.0F * f2 * 0.5F / p_361833_.speedValue;
        this.rightLeg.xRot = Mth.cos(f1 * 0.6662F) * 1.4F * f2 / p_361833_.speedValue;
        this.leftLeg.xRot = Mth.cos(f1 * 0.6662F + (float) Math.PI) * 1.4F * f2 / p_361833_.speedValue;
        this.rightLeg.yRot = 0.005F;
        this.leftLeg.yRot = -0.005F;
        this.rightLeg.zRot = 0.005F;
        this.leftLeg.zRot = -0.005F;
        if (p_361833_.isPassenger) {
            this.rightArm.xRot += (float) (-Math.PI / 5);
            this.leftArm.xRot += (float) (-Math.PI / 5);
            this.rightLeg.xRot = -1.4137167F;
            this.rightLeg.yRot = (float) (Math.PI / 10);
            this.rightLeg.zRot = 0.07853982F;
            this.leftLeg.xRot = -1.4137167F;
            this.leftLeg.yRot = (float) (-Math.PI / 10);
            this.leftLeg.zRot = -0.07853982F;
        }

        boolean flag1 = p_361833_.mainArm == HumanoidArm.RIGHT;
        if (p_361833_.isUsingItem) {
            boolean flag2 = p_361833_.useItemHand == InteractionHand.MAIN_HAND;
            if (flag2 == flag1) {
                this.poseRightArm(p_361833_);
                if (!p_361833_.rightArmPose.affectsOffhandPose()) {
                    this.poseLeftArm(p_361833_);
                }
            } else {
                this.poseLeftArm(p_361833_);
                if (!p_361833_.leftArmPose.affectsOffhandPose()) {
                    this.poseRightArm(p_361833_);
                }
            }
        } else {
            boolean flag3 = flag1 ? humanoidmodel$armpose.isTwoHanded() : humanoidmodel$armpose1.isTwoHanded();
            if (flag1 != flag3) {
                this.poseLeftArm(p_361833_);
                if (!p_361833_.leftArmPose.affectsOffhandPose()) {
                    this.poseRightArm(p_361833_);
                }
            } else {
                this.poseRightArm(p_361833_);
                if (!p_361833_.rightArmPose.affectsOffhandPose()) {
                    this.poseLeftArm(p_361833_);
                }
            }
        }

        this.setupAttackAnimation(p_361833_);
        if (p_361833_.isCrouching) {
            this.body.xRot = 0.5F;
            this.rightArm.xRot += 0.4F;
            this.leftArm.xRot += 0.4F;
            this.rightLeg.z += 4.0F;
            this.leftLeg.z += 4.0F;
            this.head.y += 4.2F;
            this.body.y += 3.2F;
            this.leftArm.y += 3.2F;
            this.rightArm.y += 3.2F;
        }

        if (humanoidmodel$armpose1 != HumanoidModel.ArmPose.SPYGLASS) {
            AnimationUtils.bobModelPart(this.rightArm, p_361833_.ageInTicks, 1.0F);
        }

        if (humanoidmodel$armpose != HumanoidModel.ArmPose.SPYGLASS) {
            AnimationUtils.bobModelPart(this.leftArm, p_361833_.ageInTicks, -1.0F);
        }

        if (f > 0.0F) {
            float f7 = f1 % 26.0F;
            HumanoidArm humanoidarm = p_361833_.attackArm;
            float f3 = p_361833_.rightArmPose != HumanoidModel.ArmPose.SPEAR && (humanoidarm != HumanoidArm.RIGHT || !(p_361833_.attackTime > 0.0F)) ? f : 0.0F;
            float f4 = p_361833_.leftArmPose != HumanoidModel.ArmPose.SPEAR && (humanoidarm != HumanoidArm.LEFT || !(p_361833_.attackTime > 0.0F)) ? f : 0.0F;
            if (!p_361833_.isUsingItem) {
                if (f7 < 14.0F) {
                    this.leftArm.xRot = Mth.rotLerpRad(f4, this.leftArm.xRot, 0.0F);
                    this.rightArm.xRot = Mth.lerp(f3, this.rightArm.xRot, 0.0F);
                    this.leftArm.yRot = Mth.rotLerpRad(f4, this.leftArm.yRot, (float) Math.PI);
                    this.rightArm.yRot = Mth.lerp(f3, this.rightArm.yRot, (float) Math.PI);
                    this.leftArm.zRot = Mth.rotLerpRad(
                        f4, this.leftArm.zRot, (float) Math.PI + 1.8707964F * this.quadraticArmUpdate(f7) / this.quadraticArmUpdate(14.0F)
                    );
                    this.rightArm.zRot = Mth.lerp(
                        f3, this.rightArm.zRot, (float) Math.PI - 1.8707964F * this.quadraticArmUpdate(f7) / this.quadraticArmUpdate(14.0F)
                    );
                } else if (f7 >= 14.0F && f7 < 22.0F) {
                    float f8 = (f7 - 14.0F) / 8.0F;
                    this.leftArm.xRot = Mth.rotLerpRad(f4, this.leftArm.xRot, (float) (Math.PI / 2) * f8);
                    this.rightArm.xRot = Mth.lerp(f3, this.rightArm.xRot, (float) (Math.PI / 2) * f8);
                    this.leftArm.yRot = Mth.rotLerpRad(f4, this.leftArm.yRot, (float) Math.PI);
                    this.rightArm.yRot = Mth.lerp(f3, this.rightArm.yRot, (float) Math.PI);
                    this.leftArm.zRot = Mth.rotLerpRad(f4, this.leftArm.zRot, 5.012389F - 1.8707964F * f8);
                    this.rightArm.zRot = Mth.lerp(f3, this.rightArm.zRot, 1.2707963F + 1.8707964F * f8);
                } else if (f7 >= 22.0F && f7 < 26.0F) {
                    float f5 = (f7 - 22.0F) / 4.0F;
                    this.leftArm.xRot = Mth.rotLerpRad(f4, this.leftArm.xRot, (float) (Math.PI / 2) - (float) (Math.PI / 2) * f5);
                    this.rightArm.xRot = Mth.lerp(f3, this.rightArm.xRot, (float) (Math.PI / 2) - (float) (Math.PI / 2) * f5);
                    this.leftArm.yRot = Mth.rotLerpRad(f4, this.leftArm.yRot, (float) Math.PI);
                    this.rightArm.yRot = Mth.lerp(f3, this.rightArm.yRot, (float) Math.PI);
                    this.leftArm.zRot = Mth.rotLerpRad(f4, this.leftArm.zRot, (float) Math.PI);
                    this.rightArm.zRot = Mth.lerp(f3, this.rightArm.zRot, (float) Math.PI);
                }
            }

            float f9 = 0.3F;
            float f6 = 0.33333334F;
            this.leftLeg.xRot = Mth.lerp(f, this.leftLeg.xRot, 0.3F * Mth.cos(f1 * 0.33333334F + (float) Math.PI));
            this.rightLeg.xRot = Mth.lerp(f, this.rightLeg.xRot, 0.3F * Mth.cos(f1 * 0.33333334F));
        }
    }

    private void poseRightArm(T renderState) {
        switch (renderState.rightArmPose) {
            case EMPTY:
                this.rightArm.yRot = 0.0F;
                break;
            case ITEM:
                this.rightArm.xRot = this.rightArm.xRot * 0.5F - (float) (Math.PI / 10);
                this.rightArm.yRot = 0.0F;
                break;
            case BLOCK:
                this.poseBlockingArm(this.rightArm, true);
                break;
            case BOW_AND_ARROW:
                this.rightArm.yRot = -0.1F + this.head.yRot;
                this.leftArm.yRot = 0.1F + this.head.yRot + 0.4F;
                this.rightArm.xRot = (float) (-Math.PI / 2) + this.head.xRot;
                this.leftArm.xRot = (float) (-Math.PI / 2) + this.head.xRot;
                break;
            case THROW_TRIDENT:
                this.rightArm.xRot = this.rightArm.xRot * 0.5F - (float) Math.PI;
                this.rightArm.yRot = 0.0F;
                break;
            case CROSSBOW_CHARGE:
                AnimationUtils.animateCrossbowCharge(this.rightArm, this.leftArm, renderState.maxCrossbowChargeDuration, renderState.ticksUsingItem, true);
                break;
            case CROSSBOW_HOLD:
                AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, true);
                break;
            case SPYGLASS:
                this.rightArm.xRot = Mth.clamp(this.head.xRot - 1.9198622F - (renderState.isCrouching ? (float) (Math.PI / 12) : 0.0F), -2.4F, 3.3F);
                this.rightArm.yRot = this.head.yRot - (float) (Math.PI / 12);
                break;
            case TOOT_HORN:
                this.rightArm.xRot = Mth.clamp(this.head.xRot, -1.2F, 1.2F) - 1.4835298F;
                this.rightArm.yRot = this.head.yRot - (float) (Math.PI / 6);
                break;
            case BRUSH:
                this.rightArm.xRot = this.rightArm.xRot * 0.5F - (float) (Math.PI / 5);
                this.rightArm.yRot = 0.0F;
                break;
            case SPEAR:
                SpearAnimations.thirdPersonHandUse(this.rightArm, this.head, true, renderState.getUseItemStackForArm(HumanoidArm.RIGHT), renderState);
                break;
            default:
                renderState.rightArmPose.applyTransform(this, renderState, net.minecraft.world.entity.HumanoidArm.RIGHT);
        }
    }

    private void poseLeftArm(T renderState) {
        switch (renderState.leftArmPose) {
            case EMPTY:
                this.leftArm.yRot = 0.0F;
                break;
            case ITEM:
                this.leftArm.xRot = this.leftArm.xRot * 0.5F - (float) (Math.PI / 10);
                this.leftArm.yRot = 0.0F;
                break;
            case BLOCK:
                this.poseBlockingArm(this.leftArm, false);
                break;
            case BOW_AND_ARROW:
                this.rightArm.yRot = -0.1F + this.head.yRot - 0.4F;
                this.leftArm.yRot = 0.1F + this.head.yRot;
                this.rightArm.xRot = (float) (-Math.PI / 2) + this.head.xRot;
                this.leftArm.xRot = (float) (-Math.PI / 2) + this.head.xRot;
                break;
            case THROW_TRIDENT:
                this.leftArm.xRot = this.leftArm.xRot * 0.5F - (float) Math.PI;
                this.leftArm.yRot = 0.0F;
                break;
            case CROSSBOW_CHARGE:
                AnimationUtils.animateCrossbowCharge(this.rightArm, this.leftArm, renderState.maxCrossbowChargeDuration, renderState.ticksUsingItem, false);
                break;
            case CROSSBOW_HOLD:
                AnimationUtils.animateCrossbowHold(this.rightArm, this.leftArm, this.head, false);
                break;
            case SPYGLASS:
                this.leftArm.xRot = Mth.clamp(this.head.xRot - 1.9198622F - (renderState.isCrouching ? (float) (Math.PI / 12) : 0.0F), -2.4F, 3.3F);
                this.leftArm.yRot = this.head.yRot + (float) (Math.PI / 12);
                break;
            case TOOT_HORN:
                this.leftArm.xRot = Mth.clamp(this.head.xRot, -1.2F, 1.2F) - 1.4835298F;
                this.leftArm.yRot = this.head.yRot + (float) (Math.PI / 6);
                break;
            case BRUSH:
                this.leftArm.xRot = this.leftArm.xRot * 0.5F - (float) (Math.PI / 5);
                this.leftArm.yRot = 0.0F;
                break;
            case SPEAR:
                SpearAnimations.thirdPersonHandUse(this.leftArm, this.head, false, renderState.getUseItemStackForArm(HumanoidArm.LEFT), renderState);
                break;
            default:
                renderState.leftArmPose.applyTransform(this, renderState, net.minecraft.world.entity.HumanoidArm.LEFT);
        }
    }

    private void poseBlockingArm(ModelPart arm, boolean isRightArm) {
        arm.xRot = arm.xRot * 0.5F - 0.9424779F + Mth.clamp(this.head.xRot, (float) (-Math.PI * 4.0 / 9.0), 0.43633232F);
        arm.yRot = (isRightArm ? -30.0F : 30.0F) * (float) (Math.PI / 180.0) + Mth.clamp(this.head.yRot, (float) (-Math.PI / 6), (float) (Math.PI / 6));
    }

    protected void setupAttackAnimation(T renderState) {
        float f = renderState.attackTime;
        if (!(f <= 0.0F)) {
            this.body.yRot = Mth.sin(Mth.sqrt(f) * (float) (Math.PI * 2)) * 0.2F;
            if (renderState.attackArm == HumanoidArm.LEFT) {
                this.body.yRot *= -1.0F;
            }

            float f1 = renderState.ageScale;
            this.rightArm.z = Mth.sin(this.body.yRot) * 5.0F * f1;
            this.rightArm.x = -Mth.cos(this.body.yRot) * 5.0F * f1;
            this.leftArm.z = -Mth.sin(this.body.yRot) * 5.0F * f1;
            this.leftArm.x = Mth.cos(this.body.yRot) * 5.0F * f1;
            this.rightArm.yRot = this.rightArm.yRot + this.body.yRot;
            this.leftArm.yRot = this.leftArm.yRot + this.body.yRot;
            this.leftArm.xRot = this.leftArm.xRot + this.body.yRot;
            switch (renderState.swingAnimationType) {
                case WHACK:
                    float f2 = Ease.outQuart(f);
                    float f3 = Mth.sin(f2 * (float) Math.PI);
                    float f4 = Mth.sin(f * (float) Math.PI) * -(this.head.xRot - 0.7F) * 0.75F;
                    ModelPart modelpart = this.getArm(renderState.attackArm);
                    modelpart.xRot -= f3 * 1.2F + f4;
                    modelpart.yRot = modelpart.yRot + this.body.yRot * 2.0F;
                    modelpart.zRot = modelpart.zRot + Mth.sin(f * (float) Math.PI) * -0.4F;
                case NONE:
                default:
                    break;
                case STAB:
                    SpearAnimations.thirdPersonAttackHand(this, renderState);
            }
        }
    }

    private float quadraticArmUpdate(float limbSwing) {
        return -65.0F * limbSwing + limbSwing * limbSwing;
    }

    public void setAllVisible(boolean visible) {
        this.head.visible = visible;
        this.hat.visible = visible;
        this.body.visible = visible;
        this.rightArm.visible = visible;
        this.leftArm.visible = visible;
        this.rightLeg.visible = visible;
        this.leftLeg.visible = visible;
    }

    public void translateToHand(HumanoidRenderState p_434082_, HumanoidArm p_102854_, PoseStack p_102855_) {
        this.root.translateAndRotate(p_102855_);
        this.getArm(p_102854_).translateAndRotate(p_102855_);
    }

    public ModelPart getArm(HumanoidArm side) {
        return side == HumanoidArm.LEFT ? this.leftArm : this.rightArm;
    }

    @Override
    public ModelPart getHead() {
        return this.head;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum ArmPose implements net.neoforged.fml.common.asm.enumextension.IExtensibleEnum {
        EMPTY(false, false),
        ITEM(false, false),
        BLOCK(false, false),
        BOW_AND_ARROW(true, true),
        THROW_TRIDENT(false, true),
        CROSSBOW_CHARGE(true, true),
        CROSSBOW_HOLD(true, true),
        SPYGLASS(false, false),
        TOOT_HORN(false, false),
        BRUSH(false, false),
        SPEAR(false, true) {
            @Override
            public <S extends ArmedEntityRenderState> void animateUseItem(
                S p_458957_, PoseStack p_455157_, float p_455081_, HumanoidArm p_456163_, ItemStack p_455992_
            ) {
                SpearAnimations.thirdPersonUseItem(p_458957_, p_455157_, p_455081_, p_456163_, p_455992_);
            }
        };

        private final boolean twoHanded;
        private final boolean affectsOffhandPose;

        private final net.neoforged.neoforge.client.@org.jspecify.annotations.Nullable IArmPoseTransformer forgeArmPose;

        @net.neoforged.fml.common.asm.enumextension.ReservedConstructor
        ArmPose(boolean twoHanded, boolean affectsOffhandPose) {
            this.twoHanded = twoHanded;
            this.affectsOffhandPose = affectsOffhandPose;
            this.forgeArmPose = null;
        }

        private ArmPose(boolean p_twoHanded, boolean p_affectsOffhandPose, net.neoforged.neoforge.client.IArmPoseTransformer forgeArmPose) {
            this.twoHanded = p_twoHanded;
            this.affectsOffhandPose = p_affectsOffhandPose;
            com.google.common.base.Preconditions.checkNotNull(forgeArmPose, "Cannot create new ArmPose with null transformer!");
            this.forgeArmPose = forgeArmPose;
        }

        public boolean isTwoHanded() {
            return this.twoHanded;
        }

        public boolean affectsOffhandPose() {
            return this.affectsOffhandPose;
        }

        public <T extends HumanoidRenderState> void applyTransform(HumanoidModel<T> model, T entity, net.minecraft.world.entity.HumanoidArm arm) {
            if (this.forgeArmPose != null) this.forgeArmPose.applyTransform(model, entity, arm);
        }

        public static net.neoforged.fml.common.asm.enumextension.ExtensionInfo getExtensionInfo() {
            return net.neoforged.fml.common.asm.enumextension.ExtensionInfo.nonExtended(HumanoidModel.ArmPose.class);
        }

        public <S extends ArmedEntityRenderState> void animateUseItem(
            S renderState, PoseStack poseStack, float useTime, HumanoidArm arm, ItemStack item
        ) {
        }
    }
}
