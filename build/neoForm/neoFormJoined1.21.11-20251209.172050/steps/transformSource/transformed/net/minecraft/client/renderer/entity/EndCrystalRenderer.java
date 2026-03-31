package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.crystal.EndCrystalModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.state.EndCrystalRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EndCrystalRenderer extends EntityRenderer<EndCrystal, EndCrystalRenderState> {
    private static final Identifier END_CRYSTAL_LOCATION = Identifier.withDefaultNamespace("textures/entity/end_crystal/end_crystal.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutNoCull(END_CRYSTAL_LOCATION);
    private final EndCrystalModel model;

    public EndCrystalRenderer(EntityRendererProvider.Context p_173970_) {
        super(p_173970_);
        this.shadowRadius = 0.5F;
        this.model = new EndCrystalModel(p_173970_.bakeLayer(ModelLayers.END_CRYSTAL));
    }

    public void submit(EndCrystalRenderState p_433044_, PoseStack p_435546_, SubmitNodeCollector p_434595_, CameraRenderState p_451464_) {
        p_435546_.pushPose();
        p_435546_.scale(2.0F, 2.0F, 2.0F);
        p_435546_.translate(0.0F, -0.5F, 0.0F);
        p_434595_.submitModel(this.model, p_433044_, p_435546_, RENDER_TYPE, p_433044_.lightCoords, OverlayTexture.NO_OVERLAY, p_433044_.outlineColor, null);
        p_435546_.popPose();
        Vec3 vec3 = p_433044_.beamOffset;
        if (vec3 != null) {
            float f = getY(p_433044_.ageInTicks);
            float f1 = (float)vec3.x;
            float f2 = (float)vec3.y;
            float f3 = (float)vec3.z;
            p_435546_.translate(vec3);
            EnderDragonRenderer.submitCrystalBeams(-f1, -f2 + f, -f3, p_433044_.ageInTicks, p_435546_, p_434595_, p_433044_.lightCoords);
        }

        super.submit(p_433044_, p_435546_, p_434595_, p_451464_);
    }

    public static float getY(float ageInTicks) {
        float f = Mth.sin(ageInTicks * 0.2F) / 2.0F + 0.5F;
        f = (f * f + f) * 0.4F;
        return f - 1.4F;
    }

    public EndCrystalRenderState createRenderState() {
        return new EndCrystalRenderState();
    }

    public void extractRenderState(EndCrystal p_362185_, EndCrystalRenderState p_364683_, float p_362440_) {
        super.extractRenderState(p_362185_, p_364683_, p_362440_);
        p_364683_.ageInTicks = p_362185_.time + p_362440_;
        p_364683_.showsBottom = p_362185_.showsBottom();
        BlockPos blockpos = p_362185_.getBeamTarget();
        if (blockpos != null) {
            p_364683_.beamOffset = Vec3.atCenterOf(blockpos).subtract(p_362185_.getPosition(p_362440_));
        } else {
            p_364683_.beamOffset = null;
        }
    }

    public boolean shouldRender(EndCrystal livingEntity, Frustum camera, double camX, double camY, double camZ) {
        return super.shouldRender(livingEntity, camera, camX, camY, camZ) || livingEntity.getBeamTarget() != null;
    }
}
