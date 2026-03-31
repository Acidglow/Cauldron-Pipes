package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.state.pip.GuiSignRenderState;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiSignRenderer extends PictureInPictureRenderer<GuiSignRenderState> {
    private final MaterialSet materials;

    public GuiSignRenderer(MultiBufferSource.BufferSource bufferSource, MaterialSet materials) {
        super(bufferSource);
        this.materials = materials;
    }

    @Override
    public Class<GuiSignRenderState> getRenderStateClass() {
        return GuiSignRenderState.class;
    }

    protected void renderToTexture(GuiSignRenderState p_415609_, PoseStack p_416717_) {
        Minecraft.getInstance().gameRenderer.getLighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        p_416717_.translate(0.0F, -0.75F, 0.0F);
        Material material = Sheets.getSignMaterial(p_415609_.woodType());
        Model.Simple model$simple = p_415609_.signModel();
        VertexConsumer vertexconsumer = material.buffer(this.materials, this.bufferSource, model$simple::renderType);
        model$simple.renderToBuffer(p_416717_, vertexconsumer, 15728880, OverlayTexture.NO_OVERLAY);
    }

    @Override
    protected String getTextureLabel() {
        return "sign";
    }
}
