package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.breeze.BreezeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.BreezeRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BreezeWindLayer extends RenderLayer<BreezeRenderState, BreezeModel> {
    private static final Identifier TEXTURE_LOCATION = Identifier.withDefaultNamespace("textures/entity/breeze/breeze_wind.png");
    private final BreezeModel model;

    public BreezeWindLayer(RenderLayerParent<BreezeRenderState, BreezeModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new BreezeModel(modelSet.bakeLayer(ModelLayers.BREEZE_WIND));
    }

    public void submit(PoseStack p_434287_, SubmitNodeCollector p_432948_, int p_435599_, BreezeRenderState p_434059_, float p_434631_, float p_435421_) {
        RenderType rendertype = RenderTypes.breezeWind(TEXTURE_LOCATION, this.xOffset(p_434059_.ageInTicks) % 1.0F, 0.0F);
        p_432948_.order(1)
            .submitModel(this.model, p_434059_, p_434287_, rendertype, p_435599_, OverlayTexture.NO_OVERLAY, -1, null, p_434059_.outlineColor, null);
    }

    private float xOffset(float tickCount) {
        return tickCount * 0.02F;
    }
}
