package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.VaultRenderState;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultClientData;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class VaultRenderer implements BlockEntityRenderer<VaultBlockEntity, VaultRenderState> {
    private final ItemModelResolver itemModelResolver;
    private final RandomSource random = RandomSource.create();

    public VaultRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public VaultRenderState createRenderState() {
        return new VaultRenderState();
    }

    public void extractRenderState(
        VaultBlockEntity p_446594_, VaultRenderState p_446868_, float p_446263_, Vec3 p_446377_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_446440_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_446594_, p_446868_, p_446263_, p_446377_, p_446440_);
        ItemStack itemstack = p_446594_.getSharedData().getDisplayItem();
        if (VaultBlockEntity.Client.shouldDisplayActiveEffects(p_446594_.getSharedData()) && !itemstack.isEmpty() && p_446594_.getLevel() != null) {
            p_446868_.displayItem = new ItemClusterRenderState();
            this.itemModelResolver.updateForTopItem(p_446868_.displayItem.item, itemstack, ItemDisplayContext.GROUND, p_446594_.getLevel(), null, 0);
            p_446868_.displayItem.count = ItemClusterRenderState.getRenderedAmount(itemstack.getCount());
            p_446868_.displayItem.seed = ItemClusterRenderState.getSeedForItemStack(itemstack);
            VaultClientData vaultclientdata = p_446594_.getClientData();
            p_446868_.spin = Mth.rotLerp(p_446263_, vaultclientdata.previousSpin(), vaultclientdata.currentSpin());
        }
    }

    public void submit(VaultRenderState p_445948_, PoseStack p_440133_, SubmitNodeCollector p_439779_, CameraRenderState p_451277_) {
        if (p_445948_.displayItem != null) {
            p_440133_.pushPose();
            p_440133_.translate(0.5F, 0.4F, 0.5F);
            p_440133_.mulPose(Axis.YP.rotationDegrees(p_445948_.spin));
            ItemEntityRenderer.renderMultipleFromCount(p_440133_, p_439779_, p_445948_.lightCoords, p_445948_.displayItem, this.random);
            p_440133_.popPose();
        }
    }
}
