package net.minecraft.tags;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.DependencySorter;
import net.minecraft.util.StrictJsonParser;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class TagLoader<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    final TagLoader.ElementLookup<T> elementLookup;
    private final String directory;

    public TagLoader(TagLoader.ElementLookup<T> elementLookup, String directory) {
        this.elementLookup = elementLookup;
        this.directory = directory;
    }

    public Map<Identifier, List<TagLoader.EntryWithSource>> load(ResourceManager resourceManager) {
        Map<Identifier, List<TagLoader.EntryWithSource>> map = new HashMap<>();
        FileToIdConverter filetoidconverter = FileToIdConverter.json(this.directory);

        for (Entry<Identifier, List<Resource>> entry : filetoidconverter.listMatchingResourceStacks(resourceManager).entrySet()) {
            Identifier identifier = entry.getKey();
            Identifier identifier1 = filetoidconverter.fileToId(identifier);

            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonElement jsonelement = StrictJsonParser.parse(reader);
                    List<TagLoader.EntryWithSource> list = map.computeIfAbsent(identifier1, p_467206_ -> new ArrayList<>());
                    TagFile tagfile = TagFile.CODEC.parse(new Dynamic<>(JsonOps.INSTANCE, jsonelement)).getOrThrow();
                    if (tagfile.replace()) {
                        list.clear();
                    }

                    String s = resource.sourcePackId();
                    tagfile.entries().forEach(p_215997_ -> list.add(new TagLoader.EntryWithSource(p_215997_, s)));
                    // Make all removal entries optional at runtime to avoid them creating intrusive holders - see NeoForge#2319
                    tagfile.remove().forEach(e -> list.add(new TagLoader.EntryWithSource(e.withRequired(false), s, true)));
                } catch (Exception exception) {
                    LOGGER.error("Couldn't read tag list {} from {} in data pack {}", identifier1, identifier, resource.sourcePackId(), exception);
                }
            }
        }

        return map;
    }

    private Either<List<TagLoader.EntryWithSource>, List<T>> tryBuildTag(TagEntry.Lookup<T> lookup, List<TagLoader.EntryWithSource> entries) {
        SequencedSet<T> sequencedset = new LinkedHashSet<>();
        List<TagLoader.EntryWithSource> list = new ArrayList<>();

        for (TagLoader.EntryWithSource tagloader$entrywithsource : entries) {
            if (!tagloader$entrywithsource.entry().build(lookup, tagloader$entrywithsource.remove() ? sequencedset::remove : sequencedset::add)) {
                list.add(tagloader$entrywithsource);
            }
        }

        return list.isEmpty() ? Either.right(List.copyOf(sequencedset)) : Either.left(list);
    }

    public Map<Identifier, List<T>> build(Map<Identifier, List<TagLoader.EntryWithSource>> builders) {
        final Map<Identifier, List<T>> map = new HashMap<>();
        TagEntry.Lookup<T> lookup = new TagEntry.Lookup<T>() {
            @Override
            public @Nullable T element(Identifier p_468161_, boolean p_380359_) {
                return (T)TagLoader.this.elementLookup.get(p_468161_, p_380359_).orElse(null);
            }

            @Override
            public @Nullable Collection<T> tag(Identifier p_466850_) {
                return map.get(p_466850_);
            }
        };
        DependencySorter<Identifier, TagLoader.SortingEntry> dependencysorter = new DependencySorter<>();
        builders.forEach(
            (p_466937_, p_284686_) -> dependencysorter.addEntry(p_466937_, new TagLoader.SortingEntry((List<TagLoader.EntryWithSource>)p_284686_))
        );
        dependencysorter.orderByDependencies(
            (p_468743_, p_359646_) -> this.tryBuildTag(lookup, p_359646_.entries)
                .ifLeft(
                    p_359633_ -> LOGGER.error(
                        "Couldn't load tag {} as it is missing following references: {}",
                        p_468743_,
                        p_359633_.stream().map(Objects::toString).collect(Collectors.joining("\n\t", "\n\t", ""))
                    )
                )
                .ifRight(p_364232_ -> map.put(p_468743_, (List<T>)p_364232_))
        );
        return map;
    }

    public static <T> void loadTagsFromNetwork(TagNetworkSerialization.NetworkPayload payload, WritableRegistry<T> registry) {
        payload.resolve(registry).tags.forEach(registry::bindTag);
    }

    public static List<Registry.PendingTags<?>> loadTagsForExistingRegistries(ResourceManager resourceManager, RegistryAccess registryAccess) {
        return registryAccess.registries()
            .map(p_359642_ -> loadPendingTags(resourceManager, p_359642_.value()))
            .flatMap(Optional::stream)
            .collect(Collectors.toUnmodifiableList());
    }

    public static <T> void loadTagsForRegistry(ResourceManager resourceManager, WritableRegistry<T> registry) {
        ResourceKey<? extends Registry<T>> resourcekey = registry.key();
        TagLoader<Holder<T>> tagloader = new TagLoader<>(TagLoader.ElementLookup.fromWritableRegistry(registry), Registries.tagsDirPath(resourcekey));
        tagloader.build(tagloader.load(resourceManager))
            .forEach((p_466412_, p_466413_) -> registry.bindTag(TagKey.create(resourcekey, p_466412_), (List<Holder<T>>)p_466413_));
    }

    private static <T> Map<TagKey<T>, List<Holder<T>>> wrapTags(ResourceKey<? extends Registry<T>> registryKey, Map<Identifier, List<Holder<T>>> tags) {
        return tags.entrySet().stream().collect(Collectors.toUnmodifiableMap(p_466409_ -> TagKey.create(registryKey, p_466409_.getKey()), Entry::getValue));
    }

    private static <T> Optional<Registry.PendingTags<T>> loadPendingTags(ResourceManager resourceManager, Registry<T> registry) {
        ResourceKey<? extends Registry<T>> resourcekey = registry.key();
        TagLoader<Holder<T>> tagloader = new TagLoader<>(
            (TagLoader.ElementLookup<Holder<T>>)TagLoader.ElementLookup.fromFrozenRegistry(registry), Registries.tagsDirPath(resourcekey)
        );
        TagLoader.LoadResult<T> loadresult = new TagLoader.LoadResult<>(resourcekey, wrapTags(registry.key(), tagloader.build(tagloader.load(resourceManager))));
        return loadresult.tags().isEmpty() ? Optional.empty() : Optional.of(registry.prepareTagReload(loadresult));
    }

    public static List<HolderLookup.RegistryLookup<?>> buildUpdatedLookups(RegistryAccess.Frozen registry, List<Registry.PendingTags<?>> tags) {
        List<HolderLookup.RegistryLookup<?>> list = new ArrayList<>();
        registry.registries().forEach(p_367916_ -> {
            Registry.PendingTags<?> pendingtags = findTagsForRegistry(tags, p_367916_.key());
            list.add((HolderLookup.RegistryLookup<?>)(pendingtags != null ? pendingtags.lookup() : p_367916_.value()));
        });
        return list;
    }

    private static Registry.@Nullable PendingTags<?> findTagsForRegistry(List<Registry.PendingTags<?>> tags, ResourceKey<? extends Registry<?>> registryKey) {
        for (Registry.PendingTags<?> pendingtags : tags) {
            if (pendingtags.key() == registryKey) {
                return pendingtags;
            }
        }

        return null;
    }

    public interface ElementLookup<T> {
        Optional<? extends T> get(Identifier id, boolean required);

        static <T> TagLoader.ElementLookup<? extends Holder<T>> fromFrozenRegistry(Registry<T> registry) {
            return (p_466419_, p_466420_) -> registry.get(p_466419_);
        }

        static <T> TagLoader.ElementLookup<Holder<T>> fromWritableRegistry(WritableRegistry<T> registry) {
            HolderGetter<T> holdergetter = registry.createRegistrationLookup();
            return (p_466416_, p_466417_) -> ((HolderGetter<T>)(p_466417_ ? holdergetter : registry)).get(ResourceKey.create(registry.key(), p_466416_));
        }
    }

    public static record EntryWithSource(TagEntry entry, String source, boolean remove) {
        public EntryWithSource(TagEntry entry, String source) {
            this(entry, source, false);
        }

        @Override
        public String toString() {
            return this.entry + " (from " + this.source + ")";
        }
    }

    public record LoadResult<T>(ResourceKey<? extends Registry<T>> key, Map<TagKey<T>, List<Holder<T>>> tags) {
    }

    record SortingEntry(List<TagLoader.EntryWithSource> entries) implements DependencySorter.Entry<Identifier> {
        @Override
        public void visitRequiredDependencies(Consumer<Identifier> p_285529_) {
            this.entries.forEach(p_285236_ -> p_285236_.entry.visitRequiredDependencies(p_285529_));
        }

        @Override
        public void visitOptionalDependencies(Consumer<Identifier> p_285469_) {
            this.entries.forEach(p_284943_ -> p_284943_.entry.visitOptionalDependencies(p_285469_));
        }
    }
}
