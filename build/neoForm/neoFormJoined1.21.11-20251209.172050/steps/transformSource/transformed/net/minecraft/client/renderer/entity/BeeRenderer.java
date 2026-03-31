package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.bee.BeeModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.BeeRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.bee.Bee;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BeeRenderer extends AgeableMobRenderer<Bee, BeeRenderState, BeeModel> {
    private static final Identifier ANGRY_BEE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee_angry.png");
    private static final Identifier ANGRY_NECTAR_BEE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee_angry_nectar.png");
    private static final Identifier BEE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee.png");
    private static final Identifier NECTAR_BEE_TEXTURE = Identifier.withDefaultNamespace("textures/entity/bee/bee_nectar.png");

    public BeeRenderer(EntityRendererProvider.Context p_173931_) {
        super(p_173931_, new BeeModel(p_173931_.bakeLayer(ModelLayers.BEE)), new BeeModel(p_173931_.bakeLayer(ModelLayers.BEE_BABY)), 0.4F);
    }

    public Identifier getTextureLocation(BeeRenderState p_468372_) {
        if (p_468372_.isAngry) {
            return p_468372_.hasNectar ? ANGRY_NECTAR_BEE_TEXTURE : ANGRY_BEE_TEXTURE;
        } else {
            return p_468372_.hasNectar ? NECTAR_BEE_TEXTURE : BEE_TEXTURE;
        }
    }

    public BeeRenderState createRenderState() {
        return new BeeRenderState();
    }

    public void extractRenderState(Bee p_481867_, BeeRenderState p_360596_, float p_365357_) {
        super.extractRenderState(p_481867_, p_360596_, p_365357_);
        p_360596_.rollAmount = p_481867_.getRollAmount(p_365357_);
        p_360596_.hasStinger = !p_481867_.hasStung();
        p_360596_.isOnGround = p_481867_.onGround() && p_481867_.getDeltaMovement().lengthSqr() < 1.0E-7;
        p_360596_.isAngry = p_481867_.isAngry();
        p_360596_.hasNectar = p_481867_.hasNectar();
    }
}
