package net.minecraft.client.renderer.texture.atlas.sources;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record PalettedPermutations(List<Identifier> textures, Identifier paletteKey, Map<String, Identifier> permutations, String separator)
    implements SpriteSource {
    static final Logger LOGGER = LogUtils.getLogger();
    public static final String DEFAULT_SEPARATOR = "_";
    public static final MapCodec<PalettedPermutations> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_465751_ -> p_465751_.group(
                Codec.list(Identifier.CODEC).fieldOf("textures").forGetter(PalettedPermutations::textures),
                Identifier.CODEC.fieldOf("palette_key").forGetter(PalettedPermutations::paletteKey),
                Codec.unboundedMap(Codec.STRING, Identifier.CODEC).fieldOf("permutations").forGetter(PalettedPermutations::permutations),
                Codec.STRING.optionalFieldOf("separator", "_").forGetter(PalettedPermutations::separator)
            )
            .apply(p_465751_, PalettedPermutations::new)
    );

    public PalettedPermutations(List<Identifier> p_400242_, Identifier p_469230_, Map<String, Identifier> p_400160_) {
        this(p_400242_, p_469230_, p_400160_, "_");
    }

    @Override
    public void run(ResourceManager p_267219_, SpriteSource.Output p_267250_) {
        Supplier<int[]> supplier = Suppliers.memoize(() -> loadPaletteEntryFromImage(p_267219_, this.paletteKey));
        Map<String, Supplier<IntUnaryOperator>> map = new HashMap<>();
        this.permutations
            .forEach(
                (p_267108_, p_468708_) -> map.put(
                    p_267108_, Suppliers.memoize(() -> createPaletteMapping(supplier.get(), loadPaletteEntryFromImage(p_267219_, p_468708_)))
                )
            );

        for (Identifier identifier : this.textures) {
            Identifier identifier1 = TEXTURE_ID_CONVERTER.idToFile(identifier);
            Optional<Resource> optional = p_267219_.getResource(identifier1);
            if (optional.isEmpty()) {
                LOGGER.warn("Unable to find texture {}", identifier1);
            } else {
                LazyLoadedImage lazyloadedimage = new LazyLoadedImage(identifier1, optional.get(), map.size());

                for (Entry<String, Supplier<IntUnaryOperator>> entry : map.entrySet()) {
                    Identifier identifier2 = identifier.withSuffix(this.separator + entry.getKey());
                    p_267250_.add(identifier2, new PalettedPermutations.PalettedSpriteSupplier(lazyloadedimage, entry.getValue(), identifier2));
                }
            }
        }
    }

    private static IntUnaryOperator createPaletteMapping(int[] keys, int[] values) {
        if (values.length != keys.length) {
            LOGGER.warn("Palette mapping has different sizes: {} and {}", keys.length, values.length);
            throw new IllegalArgumentException();
        } else {
            Int2IntMap int2intmap = new Int2IntOpenHashMap(values.length);

            for (int i = 0; i < keys.length; i++) {
                int j = keys[i];
                if (ARGB.alpha(j) != 0) {
                    int2intmap.put(ARGB.transparent(j), values[i]);
                }
            }

            return p_359295_ -> {
                int k = ARGB.alpha(p_359295_);
                if (k == 0) {
                    return p_359295_;
                } else {
                    int l = ARGB.transparent(p_359295_);
                    int i1 = int2intmap.getOrDefault(l, ARGB.opaque(l));
                    int j1 = ARGB.alpha(i1);
                    return ARGB.color(k * j1 / 255, i1);
                }
            };
        }
    }

    private static int[] loadPaletteEntryFromImage(ResourceManager resourceManager, Identifier palette) {
        Optional<Resource> optional = resourceManager.getResource(TEXTURE_ID_CONVERTER.idToFile(palette));
        if (optional.isEmpty()) {
            LOGGER.error("Failed to load palette image {}", palette);
            throw new IllegalArgumentException();
        } else {
            try {
                int[] aint;
                try (
                    InputStream inputstream = optional.get().open();
                    NativeImage nativeimage = NativeImage.read(inputstream);
                ) {
                    aint = nativeimage.getPixels();
                }

                return aint;
            } catch (Exception exception) {
                LOGGER.error("Couldn't load texture {}", palette, exception);
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public MapCodec<PalettedPermutations> codec() {
        return MAP_CODEC;
    }

    @OnlyIn(Dist.CLIENT)
    public record PalettedSpriteSupplier(LazyLoadedImage baseImage, Supplier<IntUnaryOperator> palette, Identifier permutationLocation)
        implements SpriteSource.DiscardableLoader {
        @Override
        public @Nullable SpriteContents get(SpriteResourceLoader p_295023_) {
            Object object;
            try {
                NativeImage nativeimage = this.baseImage.get().mappedCopy(this.palette.get());
                return new SpriteContents(this.permutationLocation, new FrameSize(nativeimage.getWidth(), nativeimage.getHeight()), nativeimage);
            } catch (IllegalArgumentException | IOException ioexception) {
                PalettedPermutations.LOGGER.error("unable to apply palette to {}", this.permutationLocation, ioexception);
                object = null;
            } finally {
                this.baseImage.release();
            }

            return (SpriteContents)object;
        }

        @Override
        public void discard() {
            this.baseImage.release();
        }
    }
}
