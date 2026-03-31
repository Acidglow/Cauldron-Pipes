package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.animal.pig.ColdPigModel;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.SimpleEquipmentLayer;
import net.minecraft.client.renderer.entity.state.PigRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.pig.PigVariant;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PigRenderer extends MobRenderer<Pig, PigRenderState, PigModel> {
    private final Map<PigVariant.ModelType, AdultAndBabyModelPair<PigModel>> models;

    public PigRenderer(EntityRendererProvider.Context p_174340_) {
        super(p_174340_, new PigModel(p_174340_.bakeLayer(ModelLayers.PIG)), 0.7F);
        this.models = bakeModels(p_174340_);
        this.addLayer(
            new SimpleEquipmentLayer<>(
                this,
                p_174340_.getEquipmentRenderer(),
                EquipmentClientInfo.LayerType.PIG_SADDLE,
                p_397421_ -> p_397421_.saddle,
                new PigModel(p_174340_.bakeLayer(ModelLayers.PIG_SADDLE)),
                new PigModel(p_174340_.bakeLayer(ModelLayers.PIG_BABY_SADDLE))
            )
        );
    }

    private static Map<PigVariant.ModelType, AdultAndBabyModelPair<PigModel>> bakeModels(EntityRendererProvider.Context context) {
        return Maps.newEnumMap(
            Map.of(
                PigVariant.ModelType.NORMAL,
                new AdultAndBabyModelPair<>(new PigModel(context.bakeLayer(ModelLayers.PIG)), new PigModel(context.bakeLayer(ModelLayers.PIG_BABY))),
                PigVariant.ModelType.COLD,
                new AdultAndBabyModelPair<>(
                    new ColdPigModel(context.bakeLayer(ModelLayers.COLD_PIG)), new ColdPigModel(context.bakeLayer(ModelLayers.COLD_PIG_BABY))
                )
            )
        );
    }

    public void submit(PigRenderState p_450990_, PoseStack p_434907_, SubmitNodeCollector p_435624_, CameraRenderState p_451556_) {
        if (p_450990_.variant != null) {
            this.model = this.models.get(p_450990_.variant.modelAndTexture().model()).getModel(p_450990_.isBaby);
            super.submit(p_450990_, p_434907_, p_435624_, p_451556_);
        }
    }

    public Identifier getTextureLocation(PigRenderState p_468391_) {
        return p_468391_.variant == null ? MissingTextureAtlasSprite.getLocation() : p_468391_.variant.modelAndTexture().asset().texturePath();
    }

    public PigRenderState createRenderState() {
        return new PigRenderState();
    }

    public void extractRenderState(Pig p_479718_, PigRenderState p_364366_, float p_361960_) {
        super.extractRenderState(p_479718_, p_364366_, p_361960_);
        p_364366_.saddle = p_479718_.getItemBySlot(EquipmentSlot.SADDLE).copy();
        p_364366_.variant = p_479718_.getVariant().value();
    }
}
