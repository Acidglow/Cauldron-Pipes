package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.wither.WitherBossModel;
import net.minecraft.client.renderer.entity.layers.WitherArmorLayer;
import net.minecraft.client.renderer.entity.state.WitherRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WitherBossRenderer extends MobRenderer<WitherBoss, WitherRenderState, WitherBossModel> {
    private static final Identifier WITHER_INVULNERABLE_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither_invulnerable.png");
    private static final Identifier WITHER_LOCATION = Identifier.withDefaultNamespace("textures/entity/wither/wither.png");

    public WitherBossRenderer(EntityRendererProvider.Context p_174445_) {
        super(p_174445_, new WitherBossModel(p_174445_.bakeLayer(ModelLayers.WITHER)), 1.0F);
        this.addLayer(new WitherArmorLayer(this, p_174445_.getModelSet()));
    }

    protected int getBlockLightLevel(WitherBoss entity, BlockPos pos) {
        return 15;
    }

    public Identifier getTextureLocation(WitherRenderState p_468527_) {
        int i = Mth.floor(p_468527_.invulnerableTicks);
        return i > 0 && (i > 80 || i / 5 % 2 != 1) ? WITHER_INVULNERABLE_LOCATION : WITHER_LOCATION;
    }

    public WitherRenderState createRenderState() {
        return new WitherRenderState();
    }

    protected void scale(WitherRenderState p_360406_, PoseStack p_116440_) {
        float f = 2.0F;
        if (p_360406_.invulnerableTicks > 0.0F) {
            f -= p_360406_.invulnerableTicks / 220.0F * 0.5F;
        }

        p_116440_.scale(f, f, f);
    }

    public void extractRenderState(WitherBoss p_361169_, WitherRenderState p_362092_, float p_363950_) {
        super.extractRenderState(p_361169_, p_362092_, p_363950_);
        int i = p_361169_.getInvulnerableTicks();
        p_362092_.invulnerableTicks = i > 0 ? i - p_363950_ : 0.0F;
        System.arraycopy(p_361169_.getHeadXRots(), 0, p_362092_.xHeadRots, 0, p_362092_.xHeadRots.length);
        System.arraycopy(p_361169_.getHeadYRots(), 0, p_362092_.yHeadRots, 0, p_362092_.yHeadRots.length);
        p_362092_.isPowered = p_361169_.isPowered();
    }
}
