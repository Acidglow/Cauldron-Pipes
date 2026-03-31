package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.phys.Vec3;

public record PlaySoundEffect(List<Holder<SoundEvent>> soundEvents, FloatProvider volume, FloatProvider pitch) implements EnchantmentEntityEffect {
    public static final MapCodec<PlaySoundEffect> CODEC = RecordCodecBuilder.mapCodec(
        p_454624_ -> p_454624_.group(
                ExtraCodecs.compactListCodec(SoundEvent.CODEC, SoundEvent.CODEC.sizeLimitedListOf(255))
                    .fieldOf("sound")
                    .forGetter(PlaySoundEffect::soundEvents),
                FloatProvider.codec(1.0E-5F, 10.0F).fieldOf("volume").forGetter(PlaySoundEffect::volume),
                FloatProvider.codec(1.0E-5F, 2.0F).fieldOf("pitch").forGetter(PlaySoundEffect::pitch)
            )
            .apply(p_454624_, PlaySoundEffect::new)
    );

    @Override
    public void apply(ServerLevel p_344971_, int p_344872_, EnchantedItemInUse p_345016_, Entity p_346106_, Vec3 p_345017_) {
        if (!p_346106_.isSilent()) {
            RandomSource randomsource = p_346106_.getRandom();
            int i = Mth.clamp(p_344872_ - 1, 0, this.soundEvents.size() - 1);
            p_344971_.playSound(
                null,
                p_345017_.x(),
                p_345017_.y(),
                p_345017_.z(),
                this.soundEvents.get(i),
                p_346106_.getSoundSource(),
                this.volume.sample(randomsource),
                this.pitch.sample(randomsource)
            );
        }
    }

    @Override
    public MapCodec<PlaySoundEffect> codec() {
        return CODEC;
    }
}
