package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record Dye(int defaultColor) implements ItemTintSource {
    public static final MapCodec<Dye> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_386972_ -> p_386972_.group(ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(Dye::defaultColor)).apply(p_386972_, Dye::new)
    );

    @Override
    public int calculate(ItemStack p_387455_, @Nullable ClientLevel p_390508_, @Nullable LivingEntity p_390428_) {
        return DyedItemColor.getOrDefault(p_387455_, this.defaultColor);
    }

    @Override
    public MapCodec<Dye> type() {
        return MAP_CODEC;
    }
}
