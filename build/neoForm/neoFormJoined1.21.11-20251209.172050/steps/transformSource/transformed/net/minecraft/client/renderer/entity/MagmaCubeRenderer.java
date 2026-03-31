package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.slime.MagmaCubeModel;
import net.minecraft.client.renderer.entity.state.SlimeRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.MagmaCube;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MagmaCubeRenderer extends MobRenderer<MagmaCube, SlimeRenderState, MagmaCubeModel> {
    private static final Identifier MAGMACUBE_LOCATION = Identifier.withDefaultNamespace("textures/entity/slime/magmacube.png");

    public MagmaCubeRenderer(EntityRendererProvider.Context p_174298_) {
        super(p_174298_, new MagmaCubeModel(p_174298_.bakeLayer(ModelLayers.MAGMA_CUBE)), 0.25F);
    }

    protected int getBlockLightLevel(MagmaCube entity, BlockPos pos) {
        return 15;
    }

    public Identifier getTextureLocation(SlimeRenderState p_361835_) {
        return MAGMACUBE_LOCATION;
    }

    public SlimeRenderState createRenderState() {
        return new SlimeRenderState();
    }

    public void extractRenderState(MagmaCube p_362519_, SlimeRenderState p_361851_, float p_361242_) {
        super.extractRenderState(p_362519_, p_361851_, p_361242_);
        p_361851_.squish = Mth.lerp(p_361242_, p_362519_.oSquish, p_362519_.squish);
        p_361851_.size = p_362519_.getSize();
    }

    protected float getShadowRadius(SlimeRenderState p_382806_) {
        return p_382806_.size * 0.25F;
    }

    protected void scale(SlimeRenderState p_362807_, PoseStack p_115390_) {
        int i = p_362807_.size;
        float f = p_362807_.squish / (i * 0.5F + 1.0F);
        float f1 = 1.0F / (f + 1.0F);
        p_115390_.scale(f1 * i, 1.0F / f1 * i, f1 * i);
    }
}
