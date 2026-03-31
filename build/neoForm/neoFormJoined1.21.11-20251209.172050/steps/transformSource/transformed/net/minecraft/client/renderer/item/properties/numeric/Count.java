package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record Count(boolean normalize) implements RangeSelectItemModelProperty {
    public static final MapCodec<Count> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_387175_ -> p_387175_.group(Codec.BOOL.optionalFieldOf("normalize", true).forGetter(Count::normalize)).apply(p_387175_, Count::new)
    );

    @Override
    public float get(ItemStack p_388019_, @Nullable ClientLevel p_386708_, @Nullable ItemOwner p_434102_, int p_388179_) {
        float f = p_388019_.getCount();
        float f1 = p_388019_.getMaxStackSize();
        return this.normalize ? Mth.clamp(f / f1, 0.0F, 1.0F) : Mth.clamp(f, 0.0F, f1);
    }

    @Override
    public MapCodec<Count> type() {
        return MAP_CODEC;
    }
}
