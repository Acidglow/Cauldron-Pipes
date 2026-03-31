package net.minecraft.client.renderer.texture.atlas.sources;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.IdentifierPattern;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record SourceFilter(IdentifierPattern filter) implements SpriteSource {
    public static final MapCodec<SourceFilter> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_465753_ -> p_465753_.group(IdentifierPattern.CODEC.fieldOf("pattern").forGetter(SourceFilter::filter)).apply(p_465753_, SourceFilter::new)
    );

    @Override
    public void run(ResourceManager p_261888_, SpriteSource.Output p_261864_) {
        p_261864_.removeAll(this.filter.locationPredicate());
    }

    @Override
    public MapCodec<SourceFilter> codec() {
        return MAP_CODEC;
    }
}
