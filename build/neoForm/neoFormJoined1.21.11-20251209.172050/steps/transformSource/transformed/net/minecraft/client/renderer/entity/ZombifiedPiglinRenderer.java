package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.piglin.ZombifiedPiglinModel;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.ZombifiedPiglinRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ZombifiedPiglinRenderer extends HumanoidMobRenderer<ZombifiedPiglin, ZombifiedPiglinRenderState, ZombifiedPiglinModel> {
    private static final Identifier ZOMBIFIED_PIGLIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/piglin/zombified_piglin.png");

    public ZombifiedPiglinRenderer(
        EntityRendererProvider.Context context,
        ModelLayerLocation modelLayer,
        ModelLayerLocation babyModelLayer,
        ArmorModelSet<ModelLayerLocation> armorModelSet,
        ArmorModelSet<ModelLayerLocation> babyArmorModelSet
    ) {
        super(
            context,
            new ZombifiedPiglinModel(context.bakeLayer(modelLayer)),
            new ZombifiedPiglinModel(context.bakeLayer(babyModelLayer)),
            0.5F,
            PiglinRenderer.PIGLIN_CUSTOM_HEAD_TRANSFORMS
        );
        this.addLayer(
            new HumanoidArmorLayer<>(
                this,
                ArmorModelSet.bake(armorModelSet, context.getModelSet(), ZombifiedPiglinModel::new),
                ArmorModelSet.bake(babyArmorModelSet, context.getModelSet(), ZombifiedPiglinModel::new),
                context.getEquipmentRenderer()
            )
        );
    }

    public Identifier getTextureLocation(ZombifiedPiglinRenderState p_362156_) {
        return ZOMBIFIED_PIGLIN_LOCATION;
    }

    public ZombifiedPiglinRenderState createRenderState() {
        return new ZombifiedPiglinRenderState();
    }

    public void extractRenderState(ZombifiedPiglin p_481234_, ZombifiedPiglinRenderState p_361548_, float p_361739_) {
        super.extractRenderState(p_481234_, p_361548_, p_361739_);
        p_361548_.isAggressive = p_481234_.isAggressive();
    }
}
