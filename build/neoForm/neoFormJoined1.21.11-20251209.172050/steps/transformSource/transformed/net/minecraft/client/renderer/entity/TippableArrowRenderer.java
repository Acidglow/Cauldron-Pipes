package net.minecraft.client.renderer.entity;

import net.minecraft.client.renderer.entity.state.TippableArrowRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TippableArrowRenderer extends ArrowRenderer<Arrow, TippableArrowRenderState> {
    public static final Identifier NORMAL_ARROW_LOCATION = Identifier.withDefaultNamespace("textures/entity/projectiles/arrow.png");
    public static final Identifier TIPPED_ARROW_LOCATION = Identifier.withDefaultNamespace("textures/entity/projectiles/tipped_arrow.png");

    public TippableArrowRenderer(EntityRendererProvider.Context p_174422_) {
        super(p_174422_);
    }

    protected Identifier getTextureLocation(TippableArrowRenderState p_362029_) {
        return p_362029_.isTipped ? TIPPED_ARROW_LOCATION : NORMAL_ARROW_LOCATION;
    }

    public TippableArrowRenderState createRenderState() {
        return new TippableArrowRenderState();
    }

    public void extractRenderState(Arrow p_479038_, TippableArrowRenderState p_360886_, float p_363722_) {
        super.extractRenderState(p_479038_, p_360886_, p_363722_);
        p_360886_.isTipped = p_479038_.getColor() > 0;
    }
}
