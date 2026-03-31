package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.rabbit.RabbitModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.RabbitRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RabbitRenderer extends AgeableMobRenderer<Rabbit, RabbitRenderState, RabbitModel> {
    private static final Identifier RABBIT_BROWN_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/brown.png");
    private static final Identifier RABBIT_WHITE_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/white.png");
    private static final Identifier RABBIT_BLACK_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/black.png");
    private static final Identifier RABBIT_GOLD_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/gold.png");
    private static final Identifier RABBIT_SALT_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/salt.png");
    private static final Identifier RABBIT_WHITE_SPLOTCHED_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/white_splotched.png");
    private static final Identifier RABBIT_TOAST_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/toast.png");
    private static final Identifier RABBIT_EVIL_LOCATION = Identifier.withDefaultNamespace("textures/entity/rabbit/caerbannog.png");

    public RabbitRenderer(EntityRendererProvider.Context p_174360_) {
        super(p_174360_, new RabbitModel(p_174360_.bakeLayer(ModelLayers.RABBIT)), new RabbitModel(p_174360_.bakeLayer(ModelLayers.RABBIT_BABY)), 0.3F);
    }

    public Identifier getTextureLocation(RabbitRenderState p_365061_) {
        if (p_365061_.isToast) {
            return RABBIT_TOAST_LOCATION;
        } else {
            return switch (p_365061_.variant) {
                case BROWN -> RABBIT_BROWN_LOCATION;
                case WHITE -> RABBIT_WHITE_LOCATION;
                case BLACK -> RABBIT_BLACK_LOCATION;
                case GOLD -> RABBIT_GOLD_LOCATION;
                case SALT -> RABBIT_SALT_LOCATION;
                case WHITE_SPLOTCHED -> RABBIT_WHITE_SPLOTCHED_LOCATION;
                case EVIL -> RABBIT_EVIL_LOCATION;
            };
        }
    }

    public RabbitRenderState createRenderState() {
        return new RabbitRenderState();
    }

    public void extractRenderState(Rabbit p_481188_, RabbitRenderState p_362192_, float p_365470_) {
        super.extractRenderState(p_481188_, p_362192_, p_365470_);
        p_362192_.jumpCompletion = p_481188_.getJumpCompletion(p_365470_);
        p_362192_.isToast = checkMagicName(p_481188_, "Toast");
        p_362192_.variant = p_481188_.getVariant();
    }
}
