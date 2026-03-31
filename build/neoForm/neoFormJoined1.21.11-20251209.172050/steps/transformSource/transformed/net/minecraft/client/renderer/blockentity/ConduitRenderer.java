package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MaterialMapper;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.CondiutRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ConduitRenderer implements BlockEntityRenderer<ConduitBlockEntity, CondiutRenderState> {
    public static final MaterialMapper MAPPER = new MaterialMapper(TextureAtlas.LOCATION_BLOCKS, "entity/conduit");
    public static final Material SHELL_TEXTURE = MAPPER.defaultNamespaceApply("base");
    public static final Material ACTIVE_SHELL_TEXTURE = MAPPER.defaultNamespaceApply("cage");
    public static final Material WIND_TEXTURE = MAPPER.defaultNamespaceApply("wind");
    public static final Material VERTICAL_WIND_TEXTURE = MAPPER.defaultNamespaceApply("wind_vertical");
    public static final Material OPEN_EYE_TEXTURE = MAPPER.defaultNamespaceApply("open_eye");
    public static final Material CLOSED_EYE_TEXTURE = MAPPER.defaultNamespaceApply("closed_eye");
    private final MaterialSet materials;
    private final ModelPart eye;
    private final ModelPart wind;
    private final ModelPart shell;
    private final ModelPart cage;

    public ConduitRenderer(BlockEntityRendererProvider.Context context) {
        this.materials = context.materials();
        this.eye = context.bakeLayer(ModelLayers.CONDUIT_EYE);
        this.wind = context.bakeLayer(ModelLayers.CONDUIT_WIND);
        this.shell = context.bakeLayer(ModelLayers.CONDUIT_SHELL);
        this.cage = context.bakeLayer(ModelLayers.CONDUIT_CAGE);
    }

    public static LayerDefinition createEyeLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild(
            "eye", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 0.0F, new CubeDeformation(0.01F)), PartPose.ZERO
        );
        return LayerDefinition.create(meshdefinition, 16, 16);
    }

    public static LayerDefinition createWindLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("wind", CubeListBuilder.create().texOffs(0, 0).addBox(-8.0F, -8.0F, -8.0F, 16.0F, 16.0F, 16.0F), PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 64, 32);
    }

    public static LayerDefinition createShellLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6.0F, 6.0F, 6.0F), PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 32, 16);
    }

    public static LayerDefinition createCageLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        return LayerDefinition.create(meshdefinition, 32, 16);
    }

    public CondiutRenderState createRenderState() {
        return new CondiutRenderState();
    }

    public void extractRenderState(
        ConduitBlockEntity p_446529_, CondiutRenderState p_446205_, float p_445996_, Vec3 p_446533_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_447228_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_446529_, p_446205_, p_445996_, p_446533_, p_447228_);
        p_446205_.isActive = p_446529_.isActive();
        p_446205_.activeRotation = p_446529_.getActiveRotation(p_446529_.isActive() ? p_445996_ : 0.0F);
        p_446205_.animTime = p_446529_.tickCount + p_445996_;
        p_446205_.animationPhase = p_446529_.tickCount / 66 % 3;
        p_446205_.isHunting = p_446529_.isHunting();
    }

    public void submit(CondiutRenderState p_445955_, PoseStack p_439855_, SubmitNodeCollector p_439334_, CameraRenderState p_451490_) {
        if (!p_445955_.isActive) {
            p_439855_.pushPose();
            p_439855_.translate(0.5F, 0.5F, 0.5F);
            p_439855_.mulPose(new Quaternionf().rotationY(p_445955_.activeRotation * (float) (Math.PI / 180.0)));
            p_439334_.submitModelPart(
                this.shell,
                p_439855_,
                SHELL_TEXTURE.renderType(RenderTypes::entitySolid),
                p_445955_.lightCoords,
                OverlayTexture.NO_OVERLAY,
                this.materials.get(SHELL_TEXTURE),
                -1,
                p_445955_.breakProgress
            );
            p_439855_.popPose();
        } else {
            float f = p_445955_.activeRotation * (180.0F / (float)Math.PI);
            float f1 = Mth.sin(p_445955_.animTime * 0.1F) / 2.0F + 0.5F;
            f1 = f1 * f1 + f1;
            p_439855_.pushPose();
            p_439855_.translate(0.5F, 0.3F + f1 * 0.2F, 0.5F);
            Vector3f vector3f = new Vector3f(0.5F, 1.0F, 0.5F).normalize();
            p_439855_.mulPose(new Quaternionf().rotationAxis(f * (float) (Math.PI / 180.0), vector3f));
            p_439334_.submitModelPart(
                this.cage,
                p_439855_,
                ACTIVE_SHELL_TEXTURE.renderType(RenderTypes::entityCutoutNoCull),
                p_445955_.lightCoords,
                OverlayTexture.NO_OVERLAY,
                this.materials.get(ACTIVE_SHELL_TEXTURE),
                -1,
                p_445955_.breakProgress
            );
            p_439855_.popPose();
            p_439855_.pushPose();
            p_439855_.translate(0.5F, 0.5F, 0.5F);
            if (p_445955_.animationPhase == 1) {
                p_439855_.mulPose(new Quaternionf().rotationX((float) (Math.PI / 2)));
            } else if (p_445955_.animationPhase == 2) {
                p_439855_.mulPose(new Quaternionf().rotationZ((float) (Math.PI / 2)));
            }

            Material material = p_445955_.animationPhase == 1 ? VERTICAL_WIND_TEXTURE : WIND_TEXTURE;
            RenderType rendertype = material.renderType(RenderTypes::entityCutoutNoCull);
            TextureAtlasSprite textureatlassprite = this.materials.get(material);
            p_439334_.submitModelPart(this.wind, p_439855_, rendertype, p_445955_.lightCoords, OverlayTexture.NO_OVERLAY, textureatlassprite);
            p_439855_.popPose();
            p_439855_.pushPose();
            p_439855_.translate(0.5F, 0.5F, 0.5F);
            p_439855_.scale(0.875F, 0.875F, 0.875F);
            p_439855_.mulPose(new Quaternionf().rotationXYZ((float) Math.PI, 0.0F, (float) Math.PI));
            p_439334_.submitModelPart(this.wind, p_439855_, rendertype, p_445955_.lightCoords, OverlayTexture.NO_OVERLAY, textureatlassprite);
            p_439855_.popPose();
            p_439855_.pushPose();
            p_439855_.translate(0.5F, 0.3F + f1 * 0.2F, 0.5F);
            p_439855_.scale(0.5F, 0.5F, 0.5F);
            p_439855_.mulPose(p_451490_.orientation);
            p_439855_.mulPose(new Quaternionf().rotationZ((float) Math.PI).rotateY((float) Math.PI));
            float f2 = 1.3333334F;
            p_439855_.scale(1.3333334F, 1.3333334F, 1.3333334F);
            Material material1 = p_445955_.isHunting ? OPEN_EYE_TEXTURE : CLOSED_EYE_TEXTURE;
            p_439334_.submitModelPart(
                this.eye,
                p_439855_,
                material1.renderType(RenderTypes::entityCutoutNoCull),
                p_445955_.lightCoords,
                OverlayTexture.NO_OVERLAY,
                this.materials.get(material1)
            );
            p_439855_.popPose();
        }
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(ConduitBlockEntity blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return new net.minecraft.world.phys.AABB(pos.getX(), pos.getY() - .25, pos.getZ(), pos.getX() + 1.0, pos.getY() + 1.25, pos.getZ() + 1.0);
    }
}
