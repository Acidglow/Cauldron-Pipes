package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.enderman.EndermanModel;
import net.minecraft.client.renderer.entity.layers.CarriedBlockLayer;
import net.minecraft.client.renderer.entity.layers.EnderEyesLayer;
import net.minecraft.client.renderer.entity.state.EndermanRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EndermanRenderer extends MobRenderer<EnderMan, EndermanRenderState, EndermanModel<EndermanRenderState>> {
    private static final Identifier ENDERMAN_LOCATION = Identifier.withDefaultNamespace("textures/entity/enderman/enderman.png");
    private final RandomSource random = RandomSource.create();

    public EndermanRenderer(EntityRendererProvider.Context p_173992_) {
        super(p_173992_, new EndermanModel<>(p_173992_.bakeLayer(ModelLayers.ENDERMAN)), 0.5F);
        this.addLayer(new EnderEyesLayer(this));
        this.addLayer(new CarriedBlockLayer(this));
    }

    public Vec3 getRenderOffset(EndermanRenderState p_362373_) {
        Vec3 vec3 = super.getRenderOffset(p_362373_);
        if (p_362373_.isCreepy) {
            double d0 = 0.02 * p_362373_.scale;
            return vec3.add(this.random.nextGaussian() * d0, 0.0, this.random.nextGaussian() * d0);
        } else {
            return vec3;
        }
    }

    public Identifier getTextureLocation(EndermanRenderState p_467692_) {
        return ENDERMAN_LOCATION;
    }

    public EndermanRenderState createRenderState() {
        return new EndermanRenderState();
    }

    public void extractRenderState(EnderMan p_363104_, EndermanRenderState p_361917_, float p_364048_) {
        super.extractRenderState(p_363104_, p_361917_, p_364048_);
        HumanoidMobRenderer.extractHumanoidRenderState(p_363104_, p_361917_, p_364048_, this.itemModelResolver);
        p_361917_.isCreepy = p_363104_.isCreepy();
        p_361917_.carriedBlock = p_363104_.getCarriedBlock();
    }
}
