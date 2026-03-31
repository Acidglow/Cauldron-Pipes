package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DragonFireballRenderer extends EntityRenderer<DragonFireball, EntityRenderState> {
    private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon_fireball.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutNoCull(TEXTURE_LOCATION);

    public DragonFireballRenderer(EntityRendererProvider.Context p_173962_) {
        super(p_173962_);
    }

    protected int getBlockLightLevel(DragonFireball p_481307_, BlockPos p_114088_) {
        return 15;
    }

    @Override
    public void submit(EntityRenderState p_433003_, PoseStack p_435136_, SubmitNodeCollector p_435206_, CameraRenderState p_451058_) {
        p_435136_.pushPose();
        p_435136_.scale(2.0F, 2.0F, 2.0F);
        p_435136_.mulPose(p_451058_.orientation);
        p_435206_.submitCustomGeometry(p_435136_, RENDER_TYPE, (p_433881_, p_433014_) -> {
            vertex(p_433014_, p_433881_, p_433003_.lightCoords, 0.0F, 0, 0, 1);
            vertex(p_433014_, p_433881_, p_433003_.lightCoords, 1.0F, 0, 1, 1);
            vertex(p_433014_, p_433881_, p_433003_.lightCoords, 1.0F, 1, 1, 0);
            vertex(p_433014_, p_433881_, p_433003_.lightCoords, 0.0F, 1, 0, 0);
        });
        p_435136_.popPose();
        super.submit(p_433003_, p_435136_, p_435206_, p_451058_);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, int packedLight, float x, int y, int u, int v) {
        consumer.addVertex(pose, x - 0.5F, y - 0.25F, 0.0F)
            .setColor(-1)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(packedLight)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public EntityRenderState createRenderState() {
        return new EntityRenderState();
    }
}
