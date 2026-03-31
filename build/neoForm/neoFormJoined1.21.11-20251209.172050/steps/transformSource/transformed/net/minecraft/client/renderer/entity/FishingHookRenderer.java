package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FishingHookRenderer extends EntityRenderer<FishingHook, FishingHookRenderState> {
    private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/fishing_hook.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutout(TEXTURE_LOCATION);
    private static final double VIEW_BOBBING_SCALE = 960.0;

    public FishingHookRenderer(EntityRendererProvider.Context p_174117_) {
        super(p_174117_);
    }

    public boolean shouldRender(FishingHook p_363069_, Frustum p_362635_, double p_361840_, double p_361502_, double p_360380_) {
        return super.shouldRender(p_363069_, p_362635_, p_361840_, p_361502_, p_360380_) && p_363069_.getPlayerOwner() != null;
    }

    public void submit(FishingHookRenderState p_451173_, PoseStack p_434862_, SubmitNodeCollector p_433298_, CameraRenderState p_451318_) {
        p_434862_.pushPose();
        p_434862_.pushPose();
        p_434862_.scale(0.5F, 0.5F, 0.5F);
        p_434862_.mulPose(p_451318_.orientation);
        p_433298_.submitCustomGeometry(p_434862_, RENDER_TYPE, (p_434943_, p_432878_) -> {
            vertex(p_432878_, p_434943_, p_451173_.lightCoords, 0.0F, 0, 0, 1);
            vertex(p_432878_, p_434943_, p_451173_.lightCoords, 1.0F, 0, 1, 1);
            vertex(p_432878_, p_434943_, p_451173_.lightCoords, 1.0F, 1, 1, 0);
            vertex(p_432878_, p_434943_, p_451173_.lightCoords, 0.0F, 1, 0, 0);
        });
        p_434862_.popPose();
        float f = (float)p_451173_.lineOriginOffset.x;
        float f1 = (float)p_451173_.lineOriginOffset.y;
        float f2 = (float)p_451173_.lineOriginOffset.z;
        float f3 = Minecraft.getInstance().getWindow().getAppropriateLineWidth();
        p_433298_.submitCustomGeometry(p_434862_, RenderTypes.lines(), (p_454362_, p_454363_) -> {
            int i = 16;

            for (int j = 0; j < 16; j++) {
                float f4 = fraction(j, 16);
                float f5 = fraction(j + 1, 16);
                stringVertex(f, f1, f2, p_454363_, p_454362_, f4, f5, f3);
                stringVertex(f, f1, f2, p_454363_, p_454362_, f5, f4, f3);
            }
        });
        p_434862_.popPose();
        super.submit(p_451173_, p_434862_, p_433298_, p_451318_);
    }

    public static HumanoidArm getHoldingArm(Player player) {
        return player.getMainHandItem().canPerformAction(net.neoforged.neoforge.common.ItemAbilities.FISHING_ROD_CAST) ? player.getMainArm() : player.getMainArm().getOpposite();
    }

    private Vec3 getPlayerHandPos(Player player, float handAngle, float partialTick) {
        int i = getHoldingArm(player) == HumanoidArm.RIGHT ? 1 : -1;
        if (this.entityRenderDispatcher.options.getCameraType().isFirstPerson() && player == Minecraft.getInstance().player) {
            double d4 = 960.0 / this.entityRenderDispatcher.options.fov().get().intValue();
            Vec3 vec3 = this.entityRenderDispatcher
                .camera
                .getNearPlane()
                .getPointOnPlane(i * 0.525F, -0.1F)
                .scale(d4)
                .yRot(handAngle * 0.5F)
                .xRot(-handAngle * 0.7F);
            return player.getEyePosition(partialTick).add(vec3);
        } else {
            float f = Mth.lerp(partialTick, player.yBodyRotO, player.yBodyRot) * (float) (Math.PI / 180.0);
            double d0 = Mth.sin(f);
            double d1 = Mth.cos(f);
            float f1 = player.getScale();
            double d2 = i * 0.35 * f1;
            double d3 = 0.8 * f1;
            float f2 = player.isCrouching() ? -0.1875F : 0.0F;
            return player.getEyePosition(partialTick).add(-d1 * d2 - d0 * d3, f2 - 0.45 * f1, -d0 * d2 + d1 * d3);
        }
    }

    private static float fraction(int numerator, int denominator) {
        return (float)numerator / denominator;
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight, float x, int y, int u, int v) {
        consumer.addVertex(pose, x - 0.5F, y - 0.5F, 0.0F)
            .setColor(-1)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void stringVertex(
        float x,
        float y,
        float z,
        VertexConsumer consumer,
        PoseStack.Pose pose,
        float stringFraction,
        float nextStringFraction,
        float index
    ) {
        float f = x * stringFraction;
        float f1 = y * (stringFraction * stringFraction + stringFraction) * 0.5F + 0.25F;
        float f2 = z * stringFraction;
        float f3 = x * nextStringFraction - f;
        float f4 = y * (nextStringFraction * nextStringFraction + nextStringFraction) * 0.5F + 0.25F - f1;
        float f5 = z * nextStringFraction - f2;
        float f6 = Mth.sqrt(f3 * f3 + f4 * f4 + f5 * f5);
        f3 /= f6;
        f4 /= f6;
        f5 /= f6;
        consumer.addVertex(pose, f, f1, f2).setColor(-16777216).setNormal(pose, f3, f4, f5).setLineWidth(index);
    }

    public FishingHookRenderState createRenderState() {
        return new FishingHookRenderState();
    }

    public void extractRenderState(FishingHook p_361584_, FishingHookRenderState p_364824_, float p_360891_) {
        super.extractRenderState(p_361584_, p_364824_, p_360891_);
        Player player = p_361584_.getPlayerOwner();
        if (player == null) {
            p_364824_.lineOriginOffset = Vec3.ZERO;
        } else {
            float f = player.getAttackAnim(p_360891_);
            float f1 = Mth.sin(Mth.sqrt(f) * (float) Math.PI);
            Vec3 vec3 = this.getPlayerHandPos(player, f1, p_360891_);
            Vec3 vec31 = p_361584_.getPosition(p_360891_).add(0.0, 0.25, 0.0);
            p_364824_.lineOriginOffset = vec3.subtract(vec31);
        }
    }

    protected boolean affectedByCulling(FishingHook p_365042_) {
        return false;
    }
}
