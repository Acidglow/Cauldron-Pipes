package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.ShelfRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ShelfBlock;
import net.minecraft.world.level.block.entity.ShelfBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ShelfRenderer implements BlockEntityRenderer<ShelfBlockEntity, ShelfRenderState> {
    private static final float ITEM_SIZE = 0.25F;
    private static final float ALIGN_ITEMS_TO_BOTTOM = -0.25F;
    private final ItemModelResolver itemModelResolver;

    public ShelfRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public ShelfRenderState createRenderState() {
        return new ShelfRenderState();
    }

    public void extractRenderState(
        ShelfBlockEntity p_445792_, ShelfRenderState p_445521_, float p_446232_, Vec3 p_446470_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_446704_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_445792_, p_445521_, p_446232_, p_446470_, p_446704_);
        p_445521_.alignToBottom = p_445792_.getAlignItemsToBottom();
        NonNullList<ItemStack> nonnulllist = p_445792_.getItems();
        int i = HashCommon.long2int(p_445792_.getBlockPos().asLong());

        for (int j = 0; j < nonnulllist.size(); j++) {
            ItemStack itemstack = nonnulllist.get(j);
            if (!itemstack.isEmpty()) {
                ItemStackRenderState itemstackrenderstate = new ItemStackRenderState();
                this.itemModelResolver.updateForTopItem(itemstackrenderstate, itemstack, ItemDisplayContext.ON_SHELF, p_445792_.level(), p_445792_, i + j);
                p_445521_.items[j] = itemstackrenderstate;
            }
        }
    }

    public void submit(ShelfRenderState p_446848_, PoseStack p_440250_, SubmitNodeCollector p_440104_, CameraRenderState p_451082_) {
        Direction direction = p_446848_.blockState.getValue(ShelfBlock.FACING);
        float f = direction.getAxis().isHorizontal() ? -direction.toYRot() : 180.0F;

        for (int i = 0; i < p_446848_.items.length; i++) {
            ItemStackRenderState itemstackrenderstate = p_446848_.items[i];
            if (itemstackrenderstate != null) {
                this.submitItem(p_446848_, itemstackrenderstate, p_440250_, p_440104_, i, f);
            }
        }
    }

    private void submitItem(
        ShelfRenderState shelfRenderState, ItemStackRenderState itemStackRenderState, PoseStack poseStack, SubmitNodeCollector nodeCollector, int index, float rotation
    ) {
        float f = (index - 1) * 0.3125F;
        Vec3 vec3 = new Vec3(f, shelfRenderState.alignToBottom ? -0.25 : 0.0, -0.25);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
        poseStack.translate(vec3);
        poseStack.scale(0.25F, 0.25F, 0.25F);
        AABB aabb = itemStackRenderState.getModelBoundingBox();
        double d0 = -aabb.minY;
        if (!shelfRenderState.alignToBottom) {
            d0 += -(aabb.maxY - aabb.minY) / 2.0;
        }

        poseStack.translate(0.0, d0, 0.0);
        itemStackRenderState.submit(poseStack, nodeCollector, shelfRenderState.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
