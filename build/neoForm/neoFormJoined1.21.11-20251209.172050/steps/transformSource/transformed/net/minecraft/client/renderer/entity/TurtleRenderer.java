package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.turtle.TurtleModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.TurtleRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.animal.turtle.Turtle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TurtleRenderer extends AgeableMobRenderer<Turtle, TurtleRenderState, TurtleModel> {
    private static final Identifier TURTLE_LOCATION = Identifier.withDefaultNamespace("textures/entity/turtle/big_sea_turtle.png");

    public TurtleRenderer(EntityRendererProvider.Context p_174430_) {
        super(p_174430_, new TurtleModel(p_174430_.bakeLayer(ModelLayers.TURTLE)), new TurtleModel(p_174430_.bakeLayer(ModelLayers.TURTLE_BABY)), 0.7F);
    }

    protected float getShadowRadius(TurtleRenderState p_364807_) {
        float f = super.getShadowRadius(p_364807_);
        return p_364807_.isBaby ? f * 0.83F : f;
    }

    public TurtleRenderState createRenderState() {
        return new TurtleRenderState();
    }

    public void extractRenderState(Turtle p_481358_, TurtleRenderState p_362479_, float p_360282_) {
        super.extractRenderState(p_481358_, p_362479_, p_360282_);
        p_362479_.isOnLand = !p_481358_.isInWater() && p_481358_.onGround();
        p_362479_.isLayingEgg = p_481358_.isLayingEgg();
        p_362479_.hasEgg = !p_481358_.isBaby() && p_481358_.hasEgg();
    }

    public Identifier getTextureLocation(TurtleRenderState p_362874_) {
        return TURTLE_LOCATION;
    }
}
