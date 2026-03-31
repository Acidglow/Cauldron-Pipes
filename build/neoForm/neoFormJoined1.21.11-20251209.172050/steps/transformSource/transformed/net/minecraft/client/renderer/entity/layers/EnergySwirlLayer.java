package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class EnergySwirlLayer<S extends EntityRenderState, M extends EntityModel<S>> extends RenderLayer<S, M> {
    public EnergySwirlLayer(RenderLayerParent<S, M> p_116967_) {
        super(p_116967_);
    }

    @Override
    public void submit(PoseStack p_433050_, SubmitNodeCollector p_433355_, int p_434382_, S p_433262_, float p_434698_, float p_433860_) {
        if (this.isPowered(p_433262_)) {
            float f = p_433262_.ageInTicks;
            M m = this.model();
            p_433355_.order(1)
                .submitModel(
                    m,
                    p_433262_,
                    p_433050_,
                    RenderTypes.energySwirl(this.getTextureLocation(), this.xOffset(f) % 1.0F, f * 0.01F % 1.0F),
                    p_434382_,
                    OverlayTexture.NO_OVERLAY,
                    -8355712,
                    null,
                    p_433262_.outlineColor,
                    null
                );
        }
    }

    protected abstract boolean isPowered(S renderState);

    protected abstract float xOffset(float tickCount);

    protected abstract Identifier getTextureLocation();

    protected abstract M model();
}
