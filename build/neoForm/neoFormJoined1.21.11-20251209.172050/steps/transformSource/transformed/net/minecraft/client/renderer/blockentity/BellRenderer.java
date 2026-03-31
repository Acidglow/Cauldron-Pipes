package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.bell.BellModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BellRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BellRenderer implements BlockEntityRenderer<BellBlockEntity, BellRenderState> {
    public static final Material BELL_TEXTURE = Sheets.BLOCK_ENTITIES_MAPPER.defaultNamespaceApply("bell/bell_body");
    private final MaterialSet materials;
    private final BellModel model;

    public BellRenderer(BlockEntityRendererProvider.Context context) {
        this.materials = context.materials();
        this.model = new BellModel(context.bakeLayer(ModelLayers.BELL));
    }

    public BellRenderState createRenderState() {
        return new BellRenderState();
    }

    public void extractRenderState(
        BellBlockEntity p_446818_, BellRenderState p_446429_, float p_447331_, Vec3 p_445785_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_446767_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_446818_, p_446429_, p_447331_, p_445785_, p_446767_);
        p_446429_.ticks = p_446818_.ticks + p_447331_;
        p_446429_.shakeDirection = p_446818_.shaking ? p_446818_.clickDirection : null;
    }

    public void submit(BellRenderState p_447373_, PoseStack p_439508_, SubmitNodeCollector p_438917_, CameraRenderState p_451142_) {
        BellModel.State bellmodel$state = new BellModel.State(p_447373_.ticks, p_447373_.shakeDirection);
        this.model.setupAnim(bellmodel$state);
        RenderType rendertype = BELL_TEXTURE.renderType(RenderTypes::entitySolid);
        p_438917_.submitModel(
            this.model,
            bellmodel$state,
            p_439508_,
            rendertype,
            p_447373_.lightCoords,
            OverlayTexture.NO_OVERLAY,
            -1,
            this.materials.get(BELL_TEXTURE),
            0,
            p_447373_.breakProgress
        );
    }
}
