package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BeaconRenderer<T extends BlockEntity & BeaconBeamOwner> implements BlockEntityRenderer<T, BeaconRenderState> {
    public static final Identifier BEAM_LOCATION = Identifier.withDefaultNamespace("textures/entity/beacon_beam.png");
    public static final int MAX_RENDER_Y = 2048;
    private static final float BEAM_SCALE_THRESHOLD = 96.0F;
    public static final float SOLID_BEAM_RADIUS = 0.2F;
    public static final float BEAM_GLOW_RADIUS = 0.25F;

    public BeaconRenderState createRenderState() {
        return new BeaconRenderState();
    }

    public void extractRenderState(
        T p_447130_, BeaconRenderState p_446076_, float p_445698_, Vec3 p_445714_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_447097_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_447130_, p_446076_, p_445698_, p_445714_, p_447097_);
        extract(p_447130_, p_446076_, p_445698_, p_445714_);
    }

    public static <T extends BlockEntity & BeaconBeamOwner> void extract(T sign, BeaconRenderState renderState, float partialTick, Vec3 cameraPosition) {
        renderState.animationTime = sign.getLevel() != null ? Math.floorMod(sign.getLevel().getGameTime(), 40) + partialTick : 0.0F;
        renderState.sections = sign.getBeamSections()
            .stream()
            .map(p_445220_ -> new BeaconRenderState.Section(p_445220_.getColor(), p_445220_.getHeight()))
            .toList();
        float f = (float)cameraPosition.subtract(renderState.blockPos.getCenter()).horizontalDistance();
        LocalPlayer localplayer = Minecraft.getInstance().player;
        renderState.beamRadiusScale = localplayer != null && localplayer.isScoping() ? 1.0F : Math.max(1.0F, f / 96.0F);
    }

    public void submit(BeaconRenderState p_447169_, PoseStack p_440263_, SubmitNodeCollector p_439020_, CameraRenderState p_450986_) {
        int i = 0;

        for (int j = 0; j < p_447169_.sections.size(); j++) {
            BeaconRenderState.Section beaconrenderstate$section = p_447169_.sections.get(j);
            submitBeaconBeam(
                p_440263_,
                p_439020_,
                p_447169_.beamRadiusScale,
                p_447169_.animationTime,
                i,
                j == p_447169_.sections.size() - 1 ? 2048 : beaconrenderstate$section.height(),
                beaconrenderstate$section.color()
            );
            i += beaconrenderstate$section.height();
        }
    }

    private static void submitBeaconBeam(
        PoseStack poseStack, SubmitNodeCollector nodeCollector, float radiusScale, float animationTime, int yOffset, int height, int color
    ) {
        submitBeaconBeam(poseStack, nodeCollector, BEAM_LOCATION, 1.0F, animationTime, yOffset, height, color, 0.2F * radiusScale, 0.25F * radiusScale);
    }

    public static void submitBeaconBeam(
        PoseStack poseStack,
        SubmitNodeCollector nodeCollector,
        Identifier location,
        float partialTick,
        float animationTime,
        int yOffset,
        int height,
        int color,
        float beamRadius,
        float glowRadius
    ) {
        int i = yOffset + height;
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        float f = height < 0 ? animationTime : -animationTime;
        float f1 = Mth.frac(f * 0.2F - Mth.floor(f * 0.1F));
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(animationTime * 2.25F - 45.0F));
        float f2 = 0.0F;
        float f4 = 0.0F;
        float f5 = -beamRadius;
        float f6 = 0.0F;
        float f7 = 0.0F;
        float f8 = -beamRadius;
        float f9 = 0.0F;
        float f10 = 1.0F;
        float f11 = -1.0F + f1;
        float f12 = height * partialTick * (0.5F / beamRadius) + f11;
        nodeCollector.submitCustomGeometry(
            poseStack,
            RenderTypes.beaconBeam(location, false),
            (p_440236_, p_440655_) -> renderPart(
                p_440236_, p_440655_, color, yOffset, i, 0.0F, beamRadius, beamRadius, 0.0F, f5, 0.0F, 0.0F, f8, 0.0F, 1.0F, f12, f11
            )
        );
        poseStack.popPose();
        float f2_f = -glowRadius;
        float f3 = -glowRadius;
        float f4_f = -glowRadius;
        float f5_f = -glowRadius;
        f9 = 0.0F;
        f10 = 1.0F;
        float f11_f = -1.0F + f1;
        float f12_f = height * partialTick + f11;
        nodeCollector.submitCustomGeometry(
            poseStack,
            RenderTypes.beaconBeam(location, true),
            (p_440366_, p_440684_) -> renderPart(
                p_440366_, p_440684_, ARGB.color(32, color), yOffset, i, f2_f, f3, glowRadius, f4_f, f5_f, glowRadius, glowRadius, glowRadius, 0.0F, 1.0F, f12_f, f11_f
            )
        );
        poseStack.popPose();
    }

    private static void renderPart(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        int color,
        int minY,
        int maxY,
        float x1,
        float z1,
        float x2,
        float z2,
        float x3,
        float z3,
        float x4,
        float z4,
        float minU,
        float maxU,
        float minV,
        float maxV
    ) {
        renderQuad(
            pose, consumer, color, minY, maxY, x1, z1, x2, z2, minU, maxU, minV, maxV
        );
        renderQuad(
            pose, consumer, color, minY, maxY, x4, z4, x3, z3, minU, maxU, minV, maxV
        );
        renderQuad(
            pose, consumer, color, minY, maxY, x2, z2, x4, z4, minU, maxU, minV, maxV
        );
        renderQuad(
            pose, consumer, color, minY, maxY, x3, z3, x1, z1, minU, maxU, minV, maxV
        );
    }

    private static void renderQuad(
        PoseStack.Pose pose,
        VertexConsumer consumer,
        int color,
        int minY,
        int maxY,
        float minX,
        float minZ,
        float maxX,
        float maxZ,
        float minU,
        float maxU,
        float minV,
        float maxV
    ) {
        addVertex(pose, consumer, color, maxY, minX, minZ, maxU, minV);
        addVertex(pose, consumer, color, minY, minX, minZ, maxU, maxV);
        addVertex(pose, consumer, color, minY, maxX, maxZ, minU, maxV);
        addVertex(pose, consumer, color, maxY, maxX, maxZ, minU, minV);
    }

    private static void addVertex(
        PoseStack.Pose pose, VertexConsumer consumer, int color, int y, float x, float z, float u, float v
    ) {
        consumer.addVertex(pose, x, (float)y, z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
    }

    @Override
    public boolean shouldRender(T p_173534_, Vec3 p_173535_) {
        return Vec3.atCenterOf(p_173534_.getBlockPos()).multiply(1.0, 0.0, 1.0).closerThan(p_173535_.multiply(1.0, 0.0, 1.0), this.getViewDistance());
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(T blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1.0, MAX_RENDER_Y, pos.getZ() + 1.0);
    }
}
