package net.minecraft.server.packs.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.StrictJsonParser;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public abstract class SimpleJsonResourceReloadListener<T> extends SimplePreparableReloadListener<Map<Identifier, T>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final DynamicOps<JsonElement> ops;
    private final Codec<T> codec;
    private final FileToIdConverter lister;

    protected SimpleJsonResourceReloadListener(HolderLookup.Provider provider, Codec<T> codec, ResourceKey<? extends Registry<T>> registryKey) {
        this(provider.createSerializationContext(JsonOps.INSTANCE), codec, FileToIdConverter.registry(registryKey));
    }

    protected SimpleJsonResourceReloadListener(Codec<T> codec, FileToIdConverter lister) {
        this(JsonOps.INSTANCE, codec, lister);
    }

    private SimpleJsonResourceReloadListener(DynamicOps<JsonElement> ops, Codec<T> codec, FileToIdConverter lister) {
        this.ops = ops;
        this.codec = codec;
        this.lister = lister;
    }

    /**
     * Performs any reloading that can be done off-thread, such as file IO
     */
    protected Map<Identifier, T> prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, T> map = new HashMap<>();
        // Neo: add condition context
        scanDirectory(resourceManager, this.lister, this.makeConditionalOps(this.ops), this.codec, map);
        return map;
    }

    public static <T> void scanDirectoryWithOptionalValues(
            ResourceManager p_386974_,
            ResourceKey<? extends Registry<T>> p_388878_,
            DynamicOps<JsonElement> p_388402_,
            Codec<java.util.Optional<T>> p_387608_,
            Map<Identifier, java.util.Optional<T>> p_386495_
    ) {
        scanDirectory(p_386974_, FileToIdConverter.registry(p_388878_), p_388402_, p_387608_, p_386495_);
    }

    public static <T> void scanDirectory(
        ResourceManager resourceManager,
        ResourceKey<? extends Registry<T>> registryKey,
        DynamicOps<JsonElement> ops,
        Codec<T> codec,
        Map<Identifier, T> output
    ) {
        scanDirectory(resourceManager, FileToIdConverter.registry(registryKey), ops, codec, output);
    }

    public static <T> void scanDirectory(
        ResourceManager resourceManager, FileToIdConverter lister, DynamicOps<JsonElement> ops, Codec<T> codec, Map<Identifier, T> output
    ) {
        var conditionalCodec = net.neoforged.neoforge.common.conditions.ConditionalOps.createConditionalCodec(codec);
        for (Entry<Identifier, Resource> entry : lister.listMatchingResources(resourceManager).entrySet()) {
            Identifier identifier = entry.getKey();
            Identifier identifier1 = lister.fileToId(identifier);

            try (Reader reader = entry.getValue().openAsReader()) {
                conditionalCodec.parse(ops, com.google.gson.JsonParser.parseReader(reader)).ifSuccess(p_371454_ -> {
                    if (p_371454_.isEmpty()) {
                        LOGGER.debug("Skipping loading data file '{}' from '{}' as its conditions were not met", identifier1, identifier);
                    } else if (output.putIfAbsent(identifier1, p_371454_.get()) != null) {
                        throw new IllegalStateException("Duplicate data file ignored with ID " + identifier1);
                    }
                }).ifError(p_371566_ -> LOGGER.error("Couldn't parse data file '{}' from '{}': {}", identifier1, identifier, p_371566_));
            } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
                LOGGER.error("Couldn't parse data file '{}' from '{}'", identifier1, identifier, jsonparseexception);
            }
        }
    }

    protected Identifier getPreparedPath(Identifier rl) {
        return this.lister.idToFile(rl);
    }
}
