package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.ThrownItemRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ThrownItemRenderer<T extends Entity & ItemSupplier> extends EntityRenderer<T, ThrownItemRenderState> {
    private final ItemModelResolver itemModelResolver;
    private final float scale;
    private final boolean fullBright;

    public ThrownItemRenderer(EntityRendererProvider.Context context, float scale, boolean fullBright) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
        this.scale = scale;
        this.fullBright = fullBright;
    }

    public ThrownItemRenderer(EntityRendererProvider.Context p_174414_) {
        this(p_174414_, 1.0F, false);
    }

    @Override
    protected int getBlockLightLevel(T entity, BlockPos pos) {
        return this.fullBright ? 15 : super.getBlockLightLevel(entity, pos);
    }

    public void submit(ThrownItemRenderState p_434591_, PoseStack p_432777_, SubmitNodeCollector p_435755_, CameraRenderState p_451467_) {
        p_432777_.pushPose();
        p_432777_.scale(this.scale, this.scale, this.scale);
        p_432777_.mulPose(p_451467_.orientation);
        p_434591_.item.submit(p_432777_, p_435755_, p_434591_.lightCoords, OverlayTexture.NO_OVERLAY, p_434591_.outlineColor);
        p_432777_.popPose();
        super.submit(p_434591_, p_432777_, p_435755_, p_451467_);
    }

    public ThrownItemRenderState createRenderState() {
        return new ThrownItemRenderState();
    }

    public void extractRenderState(T p_364505_, ThrownItemRenderState p_363251_, float p_362608_) {
        super.extractRenderState(p_364505_, p_363251_, p_362608_);
        this.itemModelResolver.updateForNonLiving(p_363251_.item, p_364505_.getItem(), ItemDisplayContext.GROUND, p_364505_);
    }
}
