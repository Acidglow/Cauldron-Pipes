package net.minecraft.world.attribute;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvent;

public record BackgroundMusic(Optional<Music> defaultMusic, Optional<Music> creativeMusic, Optional<Music> underwaterMusic) {
    public static final BackgroundMusic EMPTY = new BackgroundMusic(Optional.empty(), Optional.empty(), Optional.empty());
    public static final BackgroundMusic OVERWORLD = new BackgroundMusic(Optional.of(Musics.GAME), Optional.of(Musics.CREATIVE), Optional.empty());
    public static final Codec<BackgroundMusic> CODEC = RecordCodecBuilder.create(
        p_458308_ -> p_458308_.group(
                Music.CODEC.optionalFieldOf("default").forGetter(BackgroundMusic::defaultMusic),
                Music.CODEC.optionalFieldOf("creative").forGetter(BackgroundMusic::creativeMusic),
                Music.CODEC.optionalFieldOf("underwater").forGetter(BackgroundMusic::underwaterMusic)
            )
            .apply(p_458308_, BackgroundMusic::new)
    );

    public BackgroundMusic(Music p_458216_) {
        this(Optional.of(p_458216_), Optional.empty(), Optional.empty());
    }

    public BackgroundMusic(Holder<SoundEvent> p_457727_) {
        this(Musics.createGameMusic(p_457727_));
    }

    public BackgroundMusic withUnderwater(Music underwaterMusic) {
        return new BackgroundMusic(this.defaultMusic, this.creativeMusic, Optional.of(underwaterMusic));
    }

    public Optional<Music> select(boolean creative, boolean isUnderwater) {
        if (isUnderwater && this.underwaterMusic.isPresent()) {
            return this.underwaterMusic;
        } else {
            return creative && this.creativeMusic.isPresent() ? this.creativeMusic : this.defaultMusic;
        }
    }
}
