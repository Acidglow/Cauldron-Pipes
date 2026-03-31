package net.minecraft.client.color.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GrassColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record GrassColorSource(float temperature, float downfall) implements ItemTintSource {
    public static final MapCodec<GrassColorSource> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_388947_ -> p_388947_.group(
                ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("temperature").forGetter(GrassColorSource::temperature),
                ExtraCodecs.floatRange(0.0F, 1.0F).fieldOf("downfall").forGetter(GrassColorSource::downfall)
            )
            .apply(p_388947_, GrassColorSource::new)
    );

    public GrassColorSource() {
        this(0.5F, 1.0F);
    }

    @Override
    public int calculate(ItemStack p_388684_, @Nullable ClientLevel p_390380_, @Nullable LivingEntity p_390527_) {
        return GrassColor.get(this.temperature, this.downfall);
    }

    @Override
    public MapCodec<GrassColorSource> type() {
        return MAP_CODEC;
    }
}
