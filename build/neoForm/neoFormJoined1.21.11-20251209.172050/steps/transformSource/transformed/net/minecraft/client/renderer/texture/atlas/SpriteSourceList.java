package net.minecraft.client.renderer.texture.atlas;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.StrictJsonParser;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SpriteSourceList {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final FileToIdConverter ATLAS_INFO_CONVERTER = new FileToIdConverter("atlases", ".json");
    private final List<SpriteSource> sources;

    private SpriteSourceList(List<SpriteSource> sources) {
        this.sources = sources;
    }

    /**
     * @deprecated Neo: use {@link #list(ResourceManager, java.util.Set)} instead
     */
    @Deprecated
    public List<SpriteSource.Loader> list(ResourceManager resourceManager) {
        return this.list(resourceManager, java.util.Set.of());
    }

    public List<SpriteSource.Loader> list(ResourceManager resourceManager, java.util.Set<net.minecraft.server.packs.metadata.MetadataSectionType<?>> additionalMetadata) {
        final Map<Identifier, SpriteSource.DiscardableLoader> map = new HashMap<>();
        SpriteSource.Output spritesource$output = new SpriteSource.Output() {
            @Override
            public void add(Identifier p_469612_, SpriteSource.DiscardableLoader p_461059_) {
                SpriteSource.DiscardableLoader spritesource$discardableloader = map.put(p_469612_, p_461059_);
                if (spritesource$discardableloader != null) {
                    spritesource$discardableloader.discard();
                }
            }

            @Override
            public void removeAll(Predicate<Identifier> p_296294_) {
                Iterator<Entry<Identifier, SpriteSource.DiscardableLoader>> iterator = map.entrySet().iterator();

                while (iterator.hasNext()) {
                    Entry<Identifier, SpriteSource.DiscardableLoader> entry = iterator.next();
                    if (p_296294_.test(entry.getKey())) {
                        entry.getValue().discard();
                        iterator.remove();
                    }
                }
            }
        };
        this.sources.forEach(p_295860_ -> p_295860_.run(resourceManager, spritesource$output, additionalMetadata));
        Builder<SpriteSource.Loader> builder = ImmutableList.builder();
        builder.add(p_295583_ -> MissingTextureAtlasSprite.create());
        builder.addAll(map.values());
        return builder.build();
    }

    public static SpriteSourceList load(ResourceManager resourceManager, Identifier sprite) {
        Identifier identifier = ATLAS_INFO_CONVERTER.idToFile(sprite);
        List<SpriteSource> list = new ArrayList<>();

        for (Resource resource : resourceManager.getResourceStack(identifier)) {
            try (BufferedReader bufferedreader = resource.openAsReader()) {
                Dynamic<JsonElement> dynamic = new Dynamic<>(JsonOps.INSTANCE, StrictJsonParser.parse(bufferedreader));
                list.addAll(SpriteSources.FILE_CODEC.parse(dynamic).getOrThrow());
            } catch (Exception exception) {
                LOGGER.error("Failed to parse atlas definition {} in pack {}", identifier, resource.sourcePackId(), exception);
            }
        }

        return new SpriteSourceList(list);
    }
}
