package net.minecraft.client.renderer.entity;

import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.AdultAndBabyModelPair;
import net.minecraft.client.model.animal.chicken.ChickenModel;
import net.minecraft.client.model.animal.chicken.ColdChickenModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.chicken.ChickenVariant;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ChickenRenderer extends MobRenderer<Chicken, ChickenRenderState, ChickenModel> {
    private final Map<ChickenVariant.ModelType, AdultAndBabyModelPair<ChickenModel>> models;

    public ChickenRenderer(EntityRendererProvider.Context p_173952_) {
        super(p_173952_, new ChickenModel(p_173952_.bakeLayer(ModelLayers.CHICKEN)), 0.3F);
        this.models = bakeModels(p_173952_);
    }

    private static Map<ChickenVariant.ModelType, AdultAndBabyModelPair<ChickenModel>> bakeModels(EntityRendererProvider.Context context) {
        return Maps.newEnumMap(
            Map.of(
                ChickenVariant.ModelType.NORMAL,
                new AdultAndBabyModelPair<>(
                    new ChickenModel(context.bakeLayer(ModelLayers.CHICKEN)), new ChickenModel(context.bakeLayer(ModelLayers.CHICKEN_BABY))
                ),
                ChickenVariant.ModelType.COLD,
                new AdultAndBabyModelPair<>(
                    new ColdChickenModel(context.bakeLayer(ModelLayers.COLD_CHICKEN)),
                    new ColdChickenModel(context.bakeLayer(ModelLayers.COLD_CHICKEN_BABY))
                )
            )
        );
    }

    public void submit(ChickenRenderState p_451454_, PoseStack p_433710_, SubmitNodeCollector p_434568_, CameraRenderState p_451557_) {
        if (p_451454_.variant != null) {
            this.model = this.models.get(p_451454_.variant.modelAndTexture().model()).getModel(p_451454_.isBaby);
            super.submit(p_451454_, p_433710_, p_434568_, p_451557_);
        }
    }

    public Identifier getTextureLocation(ChickenRenderState p_468177_) {
        return p_468177_.variant == null ? MissingTextureAtlasSprite.getLocation() : p_468177_.variant.modelAndTexture().asset().texturePath();
    }

    public ChickenRenderState createRenderState() {
        return new ChickenRenderState();
    }

    public void extractRenderState(Chicken p_481253_, ChickenRenderState p_365088_, float p_364120_) {
        super.extractRenderState(p_481253_, p_365088_, p_364120_);
        p_365088_.flap = Mth.lerp(p_364120_, p_481253_.oFlap, p_481253_.flap);
        p_365088_.flapSpeed = Mth.lerp(p_364120_, p_481253_.oFlapSpeed, p_481253_.flapSpeed);
        p_365088_.variant = p_481253_.getVariant().value();
    }
}
