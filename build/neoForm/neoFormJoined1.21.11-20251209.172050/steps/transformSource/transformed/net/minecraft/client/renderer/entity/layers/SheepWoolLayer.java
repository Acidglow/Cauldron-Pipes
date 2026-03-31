package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SheepWoolLayer extends RenderLayer<SheepRenderState, SheepModel> {
    private static final Identifier SHEEP_WOOL_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_wool.png");
    private final EntityModel<SheepRenderState> adultModel;
    private final EntityModel<SheepRenderState> babyModel;

    public SheepWoolLayer(RenderLayerParent<SheepRenderState, SheepModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.adultModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_WOOL));
        this.babyModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_BABY_WOOL));
    }

    public void submit(PoseStack p_434319_, SubmitNodeCollector p_434903_, int p_434733_, SheepRenderState p_432970_, float p_432913_, float p_435103_) {
        if (!p_432970_.isSheared) {
            EntityModel<SheepRenderState> entitymodel = p_432970_.isBaby ? this.babyModel : this.adultModel;
            if (p_432970_.isInvisible) {
                if (p_432970_.appearsGlowing()) {
                    p_434903_.submitModel(
                        entitymodel,
                        p_432970_,
                        p_434319_,
                        RenderTypes.outline(SHEEP_WOOL_LOCATION),
                        p_434733_,
                        LivingEntityRenderer.getOverlayCoords(p_432970_, 0.0F),
                        -16777216,
                        null,
                        p_432970_.outlineColor,
                        null
                    );
                }
            } else {
                coloredCutoutModelCopyLayerRender(entitymodel, SHEEP_WOOL_LOCATION, p_434319_, p_434903_, p_434733_, p_432970_, p_432970_.getWoolColor(), 0);
            }
        }
    }
}
