package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.CowRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.CowVariant;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CowRenderer extends MobRenderer<Cow, CowRenderState, CowModel> {
    private final Map<CowVariant.ModelType, AdultAndBabyModelPair<CowModel>> models;

    public CowRenderer(EntityRendererProvider.Context p_173956_) {
        super(p_173956_, new CowModel(p_173956_.bakeLayer(ModelLayers.COW)), 0.7F);
        this.models = bakeModels(p_173956_);
    }

    private static Map<CowVariant.ModelType, AdultAndBabyModelPair<CowModel>> bakeModels(EntityRendererProvider.Context context) {
        return Maps.newEnumMap(
            Map.of(
                CowVariant.ModelType.NORMAL,
                new AdultAndBabyModelPair<>(new CowModel(context.bakeLayer(ModelLayers.COW)), new CowModel(context.bakeLayer(ModelLayers.COW_BABY))),
                CowVariant.ModelType.WARM,
                new AdultAndBabyModelPair<>(
                    new CowModel(context.bakeLayer(ModelLayers.WARM_COW)), new CowModel(context.bakeLayer(ModelLayers.WARM_COW_BABY))
                ),
                CowVariant.ModelType.COLD,
                new AdultAndBabyModelPair<>(
                    new CowModel(context.bakeLayer(ModelLayers.COLD_COW)), new CowModel(context.bakeLayer(ModelLayers.COLD_COW_BABY))
                )
            )
        );
    }

    public Identifier getTextureLocation(CowRenderState p_401372_) {
        return p_401372_.variant == null ? MissingTextureAtlasSprite.getLocation() : p_401372_.variant.modelAndTexture().asset().texturePath();
    }

    public CowRenderState createRenderState() {
        return new CowRenderState();
    }

    public void extractRenderState(Cow p_481354_, CowRenderState p_401182_, float p_360614_) {
        super.extractRenderState(p_481354_, p_401182_, p_360614_);
        p_401182_.variant = p_481354_.getVariant().value();
    }

    public void submit(CowRenderState p_451530_, PoseStack p_434100_, SubmitNodeCollector p_435172_, CameraRenderState p_450952_) {
        if (p_451530_.variant != null) {
            this.model = this.models.get(p_451530_.variant.modelAndTexture().model()).getModel(p_451530_.isBaby);
            super.submit(p_451530_, p_434100_, p_435172_, p_450952_);
        }
    }
}
