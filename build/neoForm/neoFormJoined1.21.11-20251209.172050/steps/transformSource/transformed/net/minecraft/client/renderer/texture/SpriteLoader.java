package net.minecraft.client.renderer.texture;

import com.mojang.logging.LogUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceList;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SpriteLoader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Identifier location;
    private final int maxSupportedTextureSize;

    public SpriteLoader(Identifier location, int maxSupportedTextureSize) {
        this.location = location;
        this.maxSupportedTextureSize = maxSupportedTextureSize;
    }

    public static SpriteLoader create(TextureAtlas atlas) {
        return new SpriteLoader(atlas.location(), atlas.maxSupportedTextureSize());
    }

    private SpriteLoader.Preparations stitch(List<SpriteContents> contents, int mipLevel, Executor executor) {
        SpriteLoader.Preparations spriteloader$preparations;
        try (Zone zone = Profiler.get().zone(() -> "stitch " + this.location)) {
            int i = this.maxSupportedTextureSize;
            int j = Integer.MAX_VALUE;
            int k = 1 << mipLevel;

            for (SpriteContents spritecontents : contents) {
                j = Math.min(j, Math.min(spritecontents.width(), spritecontents.height()));
                int l = Math.min(Integer.lowestOneBit(spritecontents.width()), Integer.lowestOneBit(spritecontents.height()));
                if (l < k) {
                    LOGGER.warn(
                        "Texture {} with size {}x{} limits mip level from {} to {}",
                        spritecontents.name(),
                        spritecontents.width(),
                        spritecontents.height(),
                        Mth.log2(k),
                        Mth.log2(l)
                    );
                    k = l;
                }
            }

            int j1 = Math.min(j, k);
            int k1 = Mth.log2(j1);
            int l1;
            if (false) { // Forge: Do not lower the mipmap level
                LOGGER.warn("{}: dropping miplevel from {} to {}, because of minimum power of two: {}", this.location, mipLevel, k1, j1);
                l1 = k1;
            } else {
                l1 = mipLevel;
            }

            Options options = Minecraft.getInstance().options;
            int i1 = l1 != 0 && options.textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC ? options.maxAnisotropyBit().get() : 0;
            Stitcher<SpriteContents> stitcher = new Stitcher<>(i, i, l1, i1);

            for (SpriteContents spritecontents1 : contents) {
                stitcher.registerSprite(spritecontents1);
            }

            try {
                stitcher.stitch();
            } catch (StitcherException stitcherexception) {
                CrashReport crashreport = CrashReport.forThrowable(stitcherexception, "Stitching");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Stitcher");
                crashreportcategory.setDetail(
                    "Sprites",
                    stitcherexception.getAllSprites()
                        .stream()
                        .map(p_465723_ -> String.format(Locale.ROOT, "%s[%dx%d]", p_465723_.name(), p_465723_.width(), p_465723_.height()))
                        .collect(Collectors.joining(","))
                );
                crashreportcategory.setDetail("Max Texture Size", i);
                throw new ReportedException(crashreport);
            }

            int i2 = stitcher.getWidth();
            int j2 = stitcher.getHeight();
            Map<Identifier, TextureAtlasSprite> map = this.getStitchedSprites(stitcher, i2, j2);
            TextureAtlasSprite textureatlassprite = map.get(MissingTextureAtlasSprite.getLocation());
            CompletableFuture<Void> completablefuture = CompletableFuture.runAsync(
                () -> map.values().forEach(p_251202_ -> p_251202_.contents().increaseMipLevel(l1)), executor
            );
            spriteloader$preparations = new SpriteLoader.Preparations(i2, j2, l1, textureatlassprite, map, completablefuture);
        }

        return spriteloader$preparations;
    }

    private static CompletableFuture<List<SpriteContents>> runSpriteSuppliers(
        SpriteResourceLoader spriteResourceLoader, List<SpriteSource.Loader> factories, Executor executor
    ) {
        List<CompletableFuture<SpriteContents>> list = factories.stream()
            .map(p_461190_ -> CompletableFuture.supplyAsync(() -> p_461190_.get(spriteResourceLoader), executor))
            .toList();
        return Util.sequence(list).thenApply(p_252234_ -> p_252234_.stream().filter(Objects::nonNull).toList());
    }

    public CompletableFuture<SpriteLoader.Preparations> loadAndStitch(
        ResourceManager resourceManager, Identifier location, int mipLevel, Executor executor, Set<MetadataSectionType<?>> sectionTypes
    ) {
        SpriteResourceLoader spriteresourceloader = SpriteResourceLoader.create(sectionTypes);
        return CompletableFuture.<List<SpriteSource.Loader>>supplyAsync(() -> SpriteSourceList.load(resourceManager, location).list(resourceManager, sectionTypes), executor)
            .thenCompose(p_293671_ -> runSpriteSuppliers(spriteresourceloader, (List<SpriteSource.Loader>)p_293671_, executor))
            .thenApply(p_261393_ -> this.stitch((List<SpriteContents>)p_261393_, mipLevel, executor));
    }

    private Map<Identifier, TextureAtlasSprite> getStitchedSprites(Stitcher<SpriteContents> stitcher, int x, int y) {
        Map<Identifier, TextureAtlasSprite> map = new HashMap<>();
        stitcher.gatherSprites(
            (p_465717_, p_465718_, p_465719_, p_465720_) -> map.put(
                p_465717_.name(), new TextureAtlasSprite(this.location, p_465717_, x, y, p_465718_, p_465719_, p_465720_)
            )
        );
        return map;
    }

    @OnlyIn(Dist.CLIENT)
    public record Preparations(
        int width, int height, int mipLevel, TextureAtlasSprite missing, Map<Identifier, TextureAtlasSprite> regions, CompletableFuture<Void> readyForUpload
    ) {
        public @Nullable TextureAtlasSprite getSprite(Identifier name) {
            return this.regions.get(name);
        }
    }
}
