package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.SlimeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.SlimeRenderer;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SlimeOuterLayer extends RenderLayer<SlimeRenderState, SlimeModel> {
    private final SlimeModel model;

    public SlimeOuterLayer(RenderLayerParent<SlimeRenderState, SlimeModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.model = new SlimeModel(modelSet.bakeLayer(ModelLayers.SLIME_OUTER));
    }

    public void submit(PoseStack p_433232_, SubmitNodeCollector p_435081_, int p_433844_, SlimeRenderState p_433271_, float p_435384_, float p_433573_) {
        boolean flag = p_433271_.appearsGlowing() && p_433271_.isInvisible;
        if (!p_433271_.isInvisible || flag) {
            int i = LivingEntityRenderer.getOverlayCoords(p_433271_, 0.0F);
            if (flag) {
                p_435081_.order(1)
                    .submitModel(
                        this.model,
                        p_433271_,
                        p_433232_,
                        RenderTypes.outline(SlimeRenderer.SLIME_LOCATION),
                        p_433844_,
                        i,
                        -1,
                        null,
                        p_433271_.outlineColor,
                        null
                    );
            } else {
                p_435081_.order(1)
                    .submitModel(
                        this.model,
                        p_433271_,
                        p_433232_,
                        RenderTypes.entityTranslucent(SlimeRenderer.SLIME_LOCATION),
                        p_433844_,
                        i,
                        -1,
                        null,
                        p_433271_.outlineColor,
                        null
                    );
            }
        }
    }
}
