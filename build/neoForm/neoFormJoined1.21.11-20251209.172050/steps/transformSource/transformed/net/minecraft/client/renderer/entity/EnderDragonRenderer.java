package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.dragon.EnderDragonModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EnderDragonRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class EnderDragonRenderer extends EntityRenderer<EnderDragon, EnderDragonRenderState> {
    public static final Identifier CRYSTAL_BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal_beam.png");
    private static final Identifier DRAGON_EXPLODING_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_exploding.png");
    private static final Identifier DRAGON_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");
    private static final Identifier DRAGON_EYES_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_eyes.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutNoCull(DRAGON_LOCATION);
    private static final RenderType DECAL = RenderTypes.entityDecal(DRAGON_LOCATION);
    private static final RenderType EYES = RenderTypes.eyes(DRAGON_EYES_LOCATION);
    private static final RenderType BEAM = RenderTypes.entitySmoothCutout(CRYSTAL_BEAM_LOCATION);
    private static final float HALF_SQRT_3 = (float)(Math.sqrt(3.0) / 2.0);
    private final EnderDragonModel model;

    public EnderDragonRenderer(EntityRendererProvider.Context p_173973_) {
        super(p_173973_);
        this.shadowRadius = 0.5F;
        this.model = new EnderDragonModel(p_173973_.bakeLayer(ModelLayers.ENDER_DRAGON));
    }

    public void submit(EnderDragonRenderState p_451286_, PoseStack p_433253_, SubmitNodeCollector p_435169_, CameraRenderState p_451014_) {
        p_433253_.pushPose();
        float f = p_451286_.getHistoricalPos(7).yRot();
        float f1 = (float)(p_451286_.getHistoricalPos(5).y() - p_451286_.getHistoricalPos(10).y());
        p_433253_.mulPose(Axis.YP.rotationDegrees(-f));
        p_433253_.mulPose(Axis.XP.rotationDegrees(f1 * 10.0F));
        p_433253_.translate(0.0F, 0.0F, 1.0F);
        p_433253_.scale(-1.0F, -1.0F, 1.0F);
        p_433253_.translate(0.0F, -1.501F, 0.0F);
        int i = OverlayTexture.pack(0.0F, p_451286_.hasRedOverlay);
        if (p_451286_.deathTime > 0.0F) {
            int j = ARGB.white(p_451286_.deathTime / 200.0F);
            p_435169_.order(0)
                .submitModel(
                    this.model,
                    p_451286_,
                    p_433253_,
                    RenderTypes.dragonExplosionAlpha(DRAGON_EXPLODING_LOCATION),
                    p_451286_.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    j,
                    null,
                    p_451286_.outlineColor,
                    null
                );
            p_435169_.order(1).submitModel(this.model, p_451286_, p_433253_, DECAL, p_451286_.lightCoords, i, -1, null, p_451286_.outlineColor, null);
        } else {
            p_435169_.order(0).submitModel(this.model, p_451286_, p_433253_, RENDER_TYPE, p_451286_.lightCoords, i, -1, null, p_451286_.outlineColor, null);
        }

        p_435169_.submitModel(this.model, p_451286_, p_433253_, EYES, p_451286_.lightCoords, OverlayTexture.NO_OVERLAY, p_451286_.outlineColor, null);
        if (p_451286_.deathTime > 0.0F) {
            float f2 = p_451286_.deathTime / 200.0F;
            p_433253_.pushPose();
            p_433253_.translate(0.0F, -1.0F, -2.0F);
            submitRays(p_433253_, f2, p_435169_, RenderTypes.dragonRays());
            submitRays(p_433253_, f2, p_435169_, RenderTypes.dragonRaysDepth());
            p_433253_.popPose();
        }

        p_433253_.popPose();
        if (p_451286_.beamOffset != null) {
            submitCrystalBeams(
                (float)p_451286_.beamOffset.x,
                (float)p_451286_.beamOffset.y,
                (float)p_451286_.beamOffset.z,
                p_451286_.ageInTicks,
                p_433253_,
                p_435169_,
                p_451286_.lightCoords
            );
        }

        super.submit(p_451286_, p_433253_, p_435169_, p_451014_);
    }

    private static void submitRays(PoseStack poseStack, float deathProgress, SubmitNodeCollector nodeCollector, RenderType renderType) {
        nodeCollector.submitCustomGeometry(
            poseStack,
            renderType,
            (p_434141_, p_434217_) -> {
                float f = Math.min(deathProgress > 0.8F ? (deathProgress - 0.8F) / 0.2F : 0.0F, 1.0F);
                int i = ARGB.colorFromFloat(1.0F - f, 1.0F, 1.0F, 1.0F);
                int j = 16711935;
                RandomSource randomsource = RandomSource.create(432L);
                Vector3f vector3f = new Vector3f();
                Vector3f vector3f1 = new Vector3f();
                Vector3f vector3f2 = new Vector3f();
                Vector3f vector3f3 = new Vector3f();
                Quaternionf quaternionf = new Quaternionf();
                int k = Mth.floor((deathProgress + deathProgress * deathProgress) / 2.0F * 60.0F);

                for (int l = 0; l < k; l++) {
                    quaternionf.rotationXYZ(
                            randomsource.nextFloat() * (float) (Math.PI * 2),
                            randomsource.nextFloat() * (float) (Math.PI * 2),
                            randomsource.nextFloat() * (float) (Math.PI * 2)
                        )
                        .rotateXYZ(
                            randomsource.nextFloat() * (float) (Math.PI * 2),
                            randomsource.nextFloat() * (float) (Math.PI * 2),
                            randomsource.nextFloat() * (float) (Math.PI * 2) + deathProgress * (float) (Math.PI / 2)
                        );
                    p_434141_.rotate(quaternionf);
                    float f1 = randomsource.nextFloat() * 20.0F + 5.0F + f * 10.0F;
                    float f2 = randomsource.nextFloat() * 2.0F + 1.0F + f * 2.0F;
                    vector3f1.set(-HALF_SQRT_3 * f2, f1, -0.5F * f2);
                    vector3f2.set(HALF_SQRT_3 * f2, f1, -0.5F * f2);
                    vector3f3.set(0.0F, f1, f2);
                    p_434217_.addVertex(p_434141_, vector3f).setColor(i);
                    p_434217_.addVertex(p_434141_, vector3f1).setColor(16711935);
                    p_434217_.addVertex(p_434141_, vector3f2).setColor(16711935);
                    p_434217_.addVertex(p_434141_, vector3f).setColor(i);
                    p_434217_.addVertex(p_434141_, vector3f2).setColor(16711935);
                    p_434217_.addVertex(p_434141_, vector3f3).setColor(16711935);
                    p_434217_.addVertex(p_434141_, vector3f).setColor(i);
                    p_434217_.addVertex(p_434141_, vector3f3).setColor(16711935);
                    p_434217_.addVertex(p_434141_, vector3f1).setColor(16711935);
                }
            }
        );
    }

    public static void submitCrystalBeams(
        float offsetX, float offsetY, float offsetZ, float ageInTicks, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight
    ) {
        float f = Mth.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        float f1 = Mth.sqrt(offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ);
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.0F, 0.0F);
        poseStack.mulPose(Axis.YP.rotation((float)(-Math.atan2(offsetZ, offsetX)) - (float) (Math.PI / 2)));
        poseStack.mulPose(Axis.XP.rotation((float)(-Math.atan2(f, offsetY)) - (float) (Math.PI / 2)));
        float f2 = 0.0F - ageInTicks * 0.01F;
        float f3 = f1 / 32.0F - ageInTicks * 0.01F;
        nodeCollector.submitCustomGeometry(
            poseStack,
            BEAM,
            (p_465635_, p_465636_) -> {
                int i = 8;
                float f4 = 0.0F;
                float f5 = 0.75F;
                float f6 = 0.0F;

                for (int j = 1; j <= 8; j++) {
                    float f7 = Mth.sin(j * (float) (Math.PI * 2) / 8.0F) * 0.75F;
                    float f8 = Mth.cos(j * (float) (Math.PI * 2) / 8.0F) * 0.75F;
                    float f9 = j / 8.0F;
                    p_465636_.addVertex(p_465635_, f4 * 0.2F, f5 * 0.2F, 0.0F)
                        .setColor(-16777216)
                        .setUv(f6, f2)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(p_465635_, 0.0F, -1.0F, 0.0F);
                    p_465636_.addVertex(p_465635_, f4, f5, f1)
                        .setColor(-1)
                        .setUv(f6, f3)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(p_465635_, 0.0F, -1.0F, 0.0F);
                    p_465636_.addVertex(p_465635_, f7, f8, f1)
                        .setColor(-1)
                        .setUv(f9, f3)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(p_465635_, 0.0F, -1.0F, 0.0F);
                    p_465636_.addVertex(p_465635_, f7 * 0.2F, f8 * 0.2F, 0.0F)
                        .setColor(-16777216)
                        .setUv(f9, f2)
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(packedLight)
                        .setNormal(p_465635_, 0.0F, -1.0F, 0.0F);
                    f4 = f7;
                    f5 = f8;
                    f6 = f9;
                }
            }
        );
        poseStack.popPose();
    }

    public EnderDragonRenderState createRenderState() {
        return new EnderDragonRenderState();
    }

    public void extractRenderState(EnderDragon p_361171_, EnderDragonRenderState p_363002_, float p_363418_) {
        super.extractRenderState(p_361171_, p_363002_, p_363418_);
        p_363002_.flapTime = Mth.lerp(p_363418_, p_361171_.oFlapTime, p_361171_.flapTime);
        p_363002_.deathTime = p_361171_.dragonDeathTime > 0 ? p_361171_.dragonDeathTime + p_363418_ : 0.0F;
        p_363002_.hasRedOverlay = p_361171_.hurtTime > 0;
        EndCrystal endcrystal = p_361171_.nearestCrystal;
        if (endcrystal != null) {
            Vec3 vec3 = endcrystal.getPosition(p_363418_).add(0.0, EndCrystalRenderer.getY(endcrystal.time + p_363418_), 0.0);
            p_363002_.beamOffset = vec3.subtract(p_361171_.getPosition(p_363418_));
        } else {
            p_363002_.beamOffset = null;
        }

        DragonPhaseInstance dragonphaseinstance = p_361171_.getPhaseManager().getCurrentPhase();
        p_363002_.isLandingOrTakingOff = dragonphaseinstance == EnderDragonPhase.LANDING || dragonphaseinstance == EnderDragonPhase.TAKEOFF;
        p_363002_.isSitting = dragonphaseinstance.isSitting();
        BlockPos blockpos = p_361171_.level()
            .getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(p_361171_.getFightOrigin()));
        p_363002_.distanceToEgg = blockpos.distToCenterSqr(p_361171_.position());
        p_363002_.partialTicks = p_361171_.isDeadOrDying() ? 0.0F : p_363418_;
        p_363002_.flightHistory.copyFrom(p_361171_.flightHistory);
    }

    protected boolean affectedByCulling(EnderDragon p_361699_) {
        return false;
    }
}
