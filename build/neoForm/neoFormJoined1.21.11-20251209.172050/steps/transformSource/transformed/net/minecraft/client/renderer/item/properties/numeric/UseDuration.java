package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record UseDuration(boolean remaining) implements RangeSelectItemModelProperty {
    public static final MapCodec<UseDuration> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_387574_ -> p_387574_.group(Codec.BOOL.optionalFieldOf("remaining", false).forGetter(UseDuration::remaining)).apply(p_387574_, UseDuration::new)
    );

    @Override
    public float get(ItemStack p_388572_, @Nullable ClientLevel p_387106_, @Nullable ItemOwner p_435045_, int p_386612_) {
        LivingEntity livingentity = p_435045_ == null ? null : p_435045_.asLivingEntity();
        if (livingentity != null && livingentity.getUseItem() == p_388572_) {
            return this.remaining ? livingentity.getUseItemRemainingTicks() : useDuration(p_388572_, livingentity);
        } else {
            return 0.0F;
        }
    }

    @Override
    public MapCodec<UseDuration> type() {
        return MAP_CODEC;
    }

    public static int useDuration(ItemStack stack, LivingEntity entity) {
        return stack.getUseDuration(entity) - entity.getUseItemRemainingTicks();
    }
}
