package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.ghast.HappyGhastModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.HappyGhastRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RopesLayer<M extends HappyGhastModel> extends RenderLayer<HappyGhastRenderState, M> {
    private final RenderType ropes;
    private final HappyGhastModel adultModel;
    private final HappyGhastModel babyModel;

    public RopesLayer(RenderLayerParent<HappyGhastRenderState, M> renderer, EntityModelSet entityModels, Identifier texture) {
        super(renderer);
        this.ropes = RenderTypes.entityCutoutNoCull(texture);
        this.adultModel = new HappyGhastModel(entityModels.bakeLayer(ModelLayers.HAPPY_GHAST_ROPES));
        this.babyModel = new HappyGhastModel(entityModels.bakeLayer(ModelLayers.HAPPY_GHAST_BABY_ROPES));
    }

    public void submit(PoseStack p_433539_, SubmitNodeCollector p_434725_, int p_434922_, HappyGhastRenderState p_434097_, float p_434010_, float p_435950_) {
        if (p_434097_.isLeashHolder && p_434097_.bodyItem.is(ItemTags.HARNESSES)) {
            HappyGhastModel happyghastmodel = p_434097_.isBaby ? this.babyModel : this.adultModel;
            p_434725_.submitModel(happyghastmodel, p_434097_, p_433539_, this.ropes, p_434922_, OverlayTexture.NO_OVERLAY, p_434097_.outlineColor, null);
        }
    }
}
