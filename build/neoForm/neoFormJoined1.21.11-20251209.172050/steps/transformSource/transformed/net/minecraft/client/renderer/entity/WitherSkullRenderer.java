package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.object.skull.SkullModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.WitherSkullRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitherSkullRenderer extends EntityRenderer<WitherSkull, WitherSkullRenderState> {
    private static final Identifier WITHER_INVULNERABLE_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither_invulnerable.png");
    private static final Identifier WITHER_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither.png");
    private final SkullModel model;

    public WitherSkullRenderer(EntityRendererProvider.Context p_174449_) {
        super(p_174449_);
        this.model = new SkullModel(p_174449_.bakeLayer(ModelLayers.WITHER_SKULL));
    }

    public static LayerDefinition createSkullLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 35).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    protected int getBlockLightLevel(WitherSkull p_479695_, BlockPos p_116492_) {
        return 15;
    }

    public void submit(WitherSkullRenderState p_451165_, PoseStack p_434266_, SubmitNodeCollector p_433466_, CameraRenderState p_450994_) {
        p_434266_.pushPose();
        p_434266_.scale(-1.0F, -1.0F, 1.0F);
        p_433466_.submitModel(
            this.model,
            p_451165_.modelState,
            p_434266_,
            this.model.renderType(this.getTextureLocation(p_451165_)),
            p_451165_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            p_451165_.outlineColor,
            null
        );
        p_434266_.popPose();
        super.submit(p_451165_, p_434266_, p_433466_, p_450994_);
    }

    private Identifier getTextureLocation(WitherSkullRenderState renderState) {
        return renderState.isDangerous ? WITHER_INVULNERABLE_LOCATION : WITHER_LOCATION;
    }

    public WitherSkullRenderState createRenderState() {
        return new WitherSkullRenderState();
    }

    public void extractRenderState(WitherSkull p_482079_, WitherSkullRenderState p_364978_, float p_361764_) {
        super.extractRenderState(p_482079_, p_364978_, p_361764_);
        p_364978_.isDangerous = p_482079_.isDangerous();
        p_364978_.modelState.animationPos = 0.0F;
        p_364978_.modelState.yRot = p_482079_.getYRot(p_361764_);
        p_364978_.modelState.xRot = p_482079_.getXRot(p_361764_);
    }
}
