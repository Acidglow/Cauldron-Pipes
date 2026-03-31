package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.animal.feline.CatModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.CatRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CatCollarLayer extends RenderLayer<CatRenderState, CatModel> {
    private static final Identifier CAT_COLLAR_LOCATION = Identifier.withDefaultNamespace("textures/entity/cat/cat_collar.png");
    private final CatModel adultModel;
    private final CatModel babyModel;

    public CatCollarLayer(RenderLayerParent<CatRenderState, CatModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.adultModel = new CatModel(modelSet.bakeLayer(ModelLayers.CAT_COLLAR));
        this.babyModel = new CatModel(modelSet.bakeLayer(ModelLayers.CAT_BABY_COLLAR));
    }

    public void submit(PoseStack p_434729_, SubmitNodeCollector p_435679_, int p_435603_, CatRenderState p_435848_, float p_435437_, float p_432910_) {
        DyeColor dyecolor = p_435848_.collarColor;
        if (dyecolor != null) {
            int i = dyecolor.getTextureDiffuseColor();
            CatModel catmodel = p_435848_.isBaby ? this.babyModel : this.adultModel;
            coloredCutoutModelCopyLayerRender(catmodel, CAT_COLLAR_LOCATION, p_434729_, p_435679_, p_435603_, p_435848_, i, 1);
        }
    }
}
