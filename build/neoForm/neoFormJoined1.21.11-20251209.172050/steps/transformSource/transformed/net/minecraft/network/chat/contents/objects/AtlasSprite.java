package net.minecraft.network.chat.contents.objects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;

public record AtlasSprite(Identifier atlas, Identifier sprite) implements ObjectInfo {
    public static final Identifier DEFAULT_ATLAS = AtlasIds.BLOCKS;
    public static final MapCodec<AtlasSprite> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_466110_ -> p_466110_.group(
                Identifier.CODEC.optionalFieldOf("atlas", DEFAULT_ATLAS).forGetter(AtlasSprite::atlas),
                Identifier.CODEC.fieldOf("sprite").forGetter(AtlasSprite::sprite)
            )
            .apply(p_466110_, AtlasSprite::new)
    );

    @Override
    public MapCodec<AtlasSprite> codec() {
        return MAP_CODEC;
    }

    @Override
    public FontDescription fontDescription() {
        return new FontDescription.AtlasSprite(this.atlas, this.sprite);
    }

    private static String toShortName(Identifier location) {
        return location.getNamespace().equals("minecraft") ? location.getPath() : location.toString();
    }

    @Override
    public String description() {
        String s = toShortName(this.sprite);
        return this.atlas.equals(DEFAULT_ATLAS) ? "[" + s + "]" : "[" + s + "@" + toShortName(this.atlas) + "]";
    }
}
