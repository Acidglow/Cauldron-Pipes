package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface NoDataSpecialModelRenderer extends SpecialModelRenderer<Void> {
    default @Nullable Void extractArgument(ItemStack p_386451_) {
        return null;
    }

    default void submit(
        @Nullable Void p_451685_,
        ItemDisplayContext p_439179_,
        PoseStack p_439381_,
        SubmitNodeCollector p_439114_,
        int p_440203_,
        int p_440276_,
        boolean p_440607_,
        int p_451707_
    ) {
        this.submit(p_439179_, p_439381_, p_439114_, p_440203_, p_440276_, p_440607_, p_451707_);
    }

    void submit(
        ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, int packedOverlay, boolean hasFoil, int outlineColor
    );
}
