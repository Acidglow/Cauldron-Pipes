package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.sheep.SheepFurModel;
import net.minecraft.client.model.animal.sheep.SheepModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.SheepRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SheepWoolUndercoatLayer extends RenderLayer<SheepRenderState, SheepModel> {
    private static final Identifier SHEEP_WOOL_UNDERCOAT_LOCATION = Identifier.withDefaultNamespace("textures/entity/sheep/sheep_wool_undercoat.png");
    private final EntityModel<SheepRenderState> adultModel;
    private final EntityModel<SheepRenderState> babyModel;

    public SheepWoolUndercoatLayer(RenderLayerParent<SheepRenderState, SheepModel> renderer, EntityModelSet modelSet) {
        super(renderer);
        this.adultModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_WOOL_UNDERCOAT));
        this.babyModel = new SheepFurModel(modelSet.bakeLayer(ModelLayers.SHEEP_BABY_WOOL_UNDERCOAT));
    }

    public void submit(PoseStack p_435426_, SubmitNodeCollector p_434288_, int p_433434_, SheepRenderState p_433460_, float p_433323_, float p_435925_) {
        if (!p_433460_.isInvisible && (p_433460_.isJebSheep || p_433460_.woolColor != DyeColor.WHITE)) {
            EntityModel<SheepRenderState> entitymodel = p_433460_.isBaby ? this.babyModel : this.adultModel;
            coloredCutoutModelCopyLayerRender(
                entitymodel, SHEEP_WOOL_UNDERCOAT_LOCATION, p_435426_, p_434288_, p_433434_, p_433460_, p_433460_.getWoolColor(), 1
            );
        }
    }
}
