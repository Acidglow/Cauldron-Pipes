package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.animal.parrot.ParrotModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParrotRenderer extends MobRenderer<Parrot, ParrotRenderState, ParrotModel> {
    private static final Identifier RED_BLUE = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_red_blue.png");
    private static final Identifier BLUE = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_blue.png");
    private static final Identifier GREEN = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_green.png");
    private static final Identifier YELLOW_BLUE = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_yellow_blue.png");
    private static final Identifier GREY = Identifier.withDefaultNamespace("textures/entity/parrot/parrot_grey.png");

    public ParrotRenderer(EntityRendererProvider.Context p_174336_) {
        super(p_174336_, new ParrotModel(p_174336_.bakeLayer(ModelLayers.PARROT)), 0.3F);
    }

    public Identifier getTextureLocation(ParrotRenderState p_363063_) {
        return getVariantTexture(p_363063_.variant);
    }

    public ParrotRenderState createRenderState() {
        return new ParrotRenderState();
    }

    public void extractRenderState(Parrot p_481942_, ParrotRenderState p_362444_, float p_361350_) {
        super.extractRenderState(p_481942_, p_362444_, p_361350_);
        p_362444_.variant = p_481942_.getVariant();
        float f = Mth.lerp(p_361350_, p_481942_.oFlap, p_481942_.flap);
        float f1 = Mth.lerp(p_361350_, p_481942_.oFlapSpeed, p_481942_.flapSpeed);
        p_362444_.flapAngle = (Mth.sin(f) + 1.0F) * f1;
        p_362444_.pose = ParrotModel.getPose(p_481942_);
    }

    public static Identifier getVariantTexture(Parrot.Variant variant) {
        return switch (variant) {
            case RED_BLUE -> RED_BLUE;
            case BLUE -> BLUE;
            case GREEN -> GREEN;
            case YELLOW_BLUE -> YELLOW_BLUE;
            case GRAY -> GREY;
        };
    }
}
