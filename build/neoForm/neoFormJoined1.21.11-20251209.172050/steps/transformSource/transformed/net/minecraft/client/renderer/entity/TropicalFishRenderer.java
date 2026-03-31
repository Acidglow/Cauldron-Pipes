package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.animal.fish.TropicalFishLargeModel;
import net.minecraft.client.model.animal.fish.TropicalFishSmallModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.TropicalFishPatternLayer;
import net.minecraft.client.renderer.entity.state.TropicalFishRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TropicalFishRenderer extends MobRenderer<TropicalFish, TropicalFishRenderState, EntityModel<TropicalFishRenderState>> {
    private final EntityModel<TropicalFishRenderState> smallModel = this.getModel();
    private final EntityModel<TropicalFishRenderState> largeModel;
    private static final Identifier SMALL_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a.png");
    private static final Identifier LARGE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_b.png");

    public TropicalFishRenderer(EntityRendererProvider.Context p_174428_) {
        super(p_174428_, new TropicalFishSmallModel(p_174428_.bakeLayer(ModelLayers.TROPICAL_FISH_SMALL)), 0.15F);
        this.largeModel = new TropicalFishLargeModel(p_174428_.bakeLayer(ModelLayers.TROPICAL_FISH_LARGE));
        this.addLayer(new TropicalFishPatternLayer(this, p_174428_.getModelSet()));
    }

    public Identifier getTextureLocation(TropicalFishRenderState p_468510_) {
        return switch (p_468510_.pattern.base()) {
            case SMALL -> SMALL_TEXTURE;
            case LARGE -> LARGE_TEXTURE;
        };
    }

    public TropicalFishRenderState createRenderState() {
        return new TropicalFishRenderState();
    }

    public void extractRenderState(TropicalFish p_481952_, TropicalFishRenderState p_363671_, float p_361595_) {
        super.extractRenderState(p_481952_, p_363671_, p_361595_);
        p_363671_.pattern = p_481952_.getPattern();
        p_363671_.baseColor = p_481952_.getBaseColor().getTextureDiffuseColor();
        p_363671_.patternColor = p_481952_.getPatternColor().getTextureDiffuseColor();
    }

    public void submit(TropicalFishRenderState p_451418_, PoseStack p_433628_, SubmitNodeCollector p_433130_, CameraRenderState p_451132_) {
        this.model = switch (p_451418_.pattern.base()) {
            case SMALL -> this.smallModel;
            case LARGE -> this.largeModel;
        };
        super.submit(p_451418_, p_433628_, p_433130_, p_451132_);
    }

    protected int getModelTint(TropicalFishRenderState p_364711_) {
        return p_364711_.baseColor;
    }

    protected void setupRotations(TropicalFishRenderState p_365512_, PoseStack p_116227_, float p_116228_, float p_116229_) {
        super.setupRotations(p_365512_, p_116227_, p_116228_, p_116229_);
        float f = 4.3F * Mth.sin(0.6F * p_365512_.ageInTicks);
        p_116227_.mulPose(Axis.YP.rotationDegrees(f));
        if (!p_365512_.isInWater) {
            p_116227_.translate(0.2F, 0.1F, 0.0F);
            p_116227_.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }
}
