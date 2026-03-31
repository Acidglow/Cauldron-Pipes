package net.minecraft.server.packs.resources;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class FallbackResourceManager implements ResourceManager {
    static final Logger LOGGER = LogUtils.getLogger();
    public final List<FallbackResourceManager.PackEntry> fallbacks = Lists.newArrayList();
    private final PackType type;
    private final String namespace;

    public FallbackResourceManager(PackType type, String namespace) {
        this.type = type;
        this.namespace = namespace;
    }

    public void push(PackResources resources) {
        this.pushInternal(resources.packId(), resources, null);
    }

    public void push(PackResources resources, Predicate<Identifier> filter) {
        this.pushInternal(resources.packId(), resources, filter);
    }

    public void pushFilterOnly(String name, Predicate<Identifier> filter) {
        this.pushInternal(name, null, filter);
    }

    private void pushInternal(String name, @Nullable PackResources resources, @Nullable Predicate<Identifier> filter) {
        this.fallbacks.add(new FallbackResourceManager.PackEntry(name, resources, filter));
    }

    @Override
    public Set<String> getNamespaces() {
        return ImmutableSet.of(this.namespace);
    }

    @Override
    public Optional<Resource> getResource(Identifier p_468360_) {
        for (int i = this.fallbacks.size() - 1; i >= 0; i--) {
            FallbackResourceManager.PackEntry fallbackresourcemanager$packentry = this.fallbacks.get(i);
            PackResources packresources = fallbackresourcemanager$packentry.resources;
            if (packresources != null) {
                IoSupplier<InputStream> iosupplier = packresources.getResource(this.type, p_468360_);
                if (iosupplier != null) {
                    IoSupplier<ResourceMetadata> iosupplier1 = this.createStackMetadataFinder(p_468360_, i);
                    return Optional.of(createResource(packresources, p_468360_, iosupplier, iosupplier1));
                }
            }

            if (fallbackresourcemanager$packentry.isFiltered(p_468360_)) {
                LOGGER.warn("Resource {} not found, but was filtered by pack {}", p_468360_, fallbackresourcemanager$packentry.name);
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    private static Resource createResource(
        PackResources source, Identifier location, IoSupplier<InputStream> streamSupplier, IoSupplier<ResourceMetadata> metadataSupplier
    ) {
        return new Resource(source, wrapForDebug(location, source, streamSupplier), metadataSupplier);
    }

    private static IoSupplier<InputStream> wrapForDebug(Identifier location, PackResources packResources, IoSupplier<InputStream> stream) {
        return LOGGER.isDebugEnabled()
            ? () -> new FallbackResourceManager.LeakedResourceWarningInputStream(stream.get(), location, packResources.packId())
            : stream;
    }

    @Override
    public List<Resource> getResourceStack(Identifier p_469116_) {
        Identifier identifier = getMetadataLocation(p_469116_);
        List<Resource> list = new ArrayList<>();
        boolean flag = false;
        String s = null;

        for (int i = this.fallbacks.size() - 1; i >= 0; i--) {
            FallbackResourceManager.PackEntry fallbackresourcemanager$packentry = this.fallbacks.get(i);
            PackResources packresources = fallbackresourcemanager$packentry.resources;
            if (packresources != null) {
                IoSupplier<InputStream> iosupplier = packresources.getResource(this.type, p_469116_);
                if (iosupplier != null) {
                    IoSupplier<ResourceMetadata> iosupplier1;
                    if (flag) {
                        iosupplier1 = ResourceMetadata.EMPTY_SUPPLIER;
                    } else {
                        iosupplier1 = () -> {
                            IoSupplier<InputStream> iosupplier2 = packresources.getResource(this.type, identifier);
                            return iosupplier2 != null ? parseMetadata(iosupplier2) : ResourceMetadata.EMPTY;
                        };
                    }

                    list.add(new Resource(packresources, iosupplier, iosupplier1));
                }
            }

            if (fallbackresourcemanager$packentry.isFiltered(p_469116_)) {
                s = fallbackresourcemanager$packentry.name;
                break;
            }

            if (fallbackresourcemanager$packentry.isFiltered(identifier)) {
                flag = true;
            }
        }

        if (list.isEmpty() && s != null) {
            LOGGER.warn("Resource {} not found, but was filtered by pack {}", p_469116_, s);
        }

        return Lists.reverse(list);
    }

    private static boolean isMetadata(Identifier location) {
        return location.getPath().endsWith(".mcmeta");
    }

    private static Identifier getIdentifierFromMetadata(Identifier location) {
        String s = location.getPath().substring(0, location.getPath().length() - ".mcmeta".length());
        return location.withPath(s);
    }

    static Identifier getMetadataLocation(Identifier location) {
        return location.withPath(location.getPath() + ".mcmeta");
    }

    @Override
    public Map<Identifier, Resource> listResources(String p_215413_, Predicate<Identifier> p_215414_) {
        record ResourceWithSourceAndIndex(PackResources packResources, IoSupplier<InputStream> resource, int packIndex) {
        }

        Map<Identifier, ResourceWithSourceAndIndex> map = new HashMap<>();
        Map<Identifier, ResourceWithSourceAndIndex> map1 = new HashMap<>();
        int i = this.fallbacks.size();

        for (int j = 0; j < i; j++) {
            FallbackResourceManager.PackEntry fallbackresourcemanager$packentry = this.fallbacks.get(j);
            fallbackresourcemanager$packentry.filterAll(map.keySet());
            fallbackresourcemanager$packentry.filterAll(map1.keySet());
            PackResources packresources = fallbackresourcemanager$packentry.resources;
            if (packresources != null) {
                int k = j;
                packresources.listResources(this.type, this.namespace, p_215413_, (p_466366_, p_466367_) -> {
                    if (isMetadata(p_466366_)) {
                        if (p_215414_.test(getIdentifierFromMetadata(p_466366_))) {
                            map1.put(p_466366_, new ResourceWithSourceAndIndex(packresources, p_466367_, k));
                        }
                    } else if (p_215414_.test(p_466366_)) {
                        map.put(p_466366_, new ResourceWithSourceAndIndex(packresources, p_466367_, k));
                    }
                });
            }
        }

        Map<Identifier, Resource> map2 = Maps.newTreeMap();
        map.forEach(
            (p_466372_, p_466373_) -> {
                Identifier identifier = getMetadataLocation(p_466372_);
                ResourceWithSourceAndIndex fallbackresourcemanager$1resourcewithsourceandindex = map1.get(identifier);
                IoSupplier<ResourceMetadata> iosupplier;
                if (fallbackresourcemanager$1resourcewithsourceandindex != null
                    && fallbackresourcemanager$1resourcewithsourceandindex.packIndex >= p_466373_.packIndex) {
                    iosupplier = convertToMetadata(fallbackresourcemanager$1resourcewithsourceandindex.resource);
                } else {
                    iosupplier = ResourceMetadata.EMPTY_SUPPLIER;
                }

                map2.put(p_466372_, createResource(p_466373_.packResources, p_466372_, p_466373_.resource, iosupplier));
            }
        );
        return map2;
    }

    private IoSupplier<ResourceMetadata> createStackMetadataFinder(Identifier location, int fallbackIndex) {
        return () -> {
            Identifier identifier = getMetadataLocation(location);

            for (int i = this.fallbacks.size() - 1; i >= fallbackIndex; i--) {
                FallbackResourceManager.PackEntry fallbackresourcemanager$packentry = this.fallbacks.get(i);
                PackResources packresources = fallbackresourcemanager$packentry.resources;
                if (packresources != null) {
                    IoSupplier<InputStream> iosupplier = packresources.getResource(this.type, identifier);
                    if (iosupplier != null) {
                        return parseMetadata(iosupplier);
                    }
                }

                if (fallbackresourcemanager$packentry.isFiltered(identifier)) {
                    break;
                }
            }

            return ResourceMetadata.EMPTY;
        };
    }

    private static IoSupplier<ResourceMetadata> convertToMetadata(IoSupplier<InputStream> streamSupplier) {
        return () -> parseMetadata(streamSupplier);
    }

    private static ResourceMetadata parseMetadata(IoSupplier<InputStream> streamSupplier) throws IOException {
        ResourceMetadata resourcemetadata;
        try (InputStream inputstream = streamSupplier.get()) {
            resourcemetadata = ResourceMetadata.fromJsonStream(inputstream);
        }

        return resourcemetadata;
    }

    private static void applyPackFiltersToExistingResources(
        FallbackResourceManager.PackEntry packEntry, Map<Identifier, FallbackResourceManager.EntryStack> resources
    ) {
        for (FallbackResourceManager.EntryStack fallbackresourcemanager$entrystack : resources.values()) {
            if (packEntry.isFiltered(fallbackresourcemanager$entrystack.fileLocation)) {
                fallbackresourcemanager$entrystack.fileSources.clear();
            } else if (packEntry.isFiltered(fallbackresourcemanager$entrystack.metadataLocation())) {
                fallbackresourcemanager$entrystack.metaSources.clear();
            }
        }
    }

    private void listPackResources(
        FallbackResourceManager.PackEntry entry,
        String path,
        Predicate<Identifier> filter,
        Map<Identifier, FallbackResourceManager.EntryStack> output
    ) {
        PackResources packresources = entry.resources;
        if (packresources != null) {
            packresources.listResources(
                this.type,
                this.namespace,
                path,
                (p_466377_, p_466378_) -> {
                    if (isMetadata(p_466377_)) {
                        Identifier identifier = getIdentifierFromMetadata(p_466377_);
                        if (!filter.test(identifier)) {
                            return;
                        }

                        output.computeIfAbsent(identifier, FallbackResourceManager.EntryStack::new).metaSources.put(packresources, p_466378_);
                    } else {
                        if (!filter.test(p_466377_)) {
                            return;
                        }

                        output.computeIfAbsent(p_466377_, FallbackResourceManager.EntryStack::new)
                            .fileSources
                            .add(new FallbackResourceManager.ResourceWithSource(packresources, p_466378_));
                    }
                }
            );
        }
    }

    @Override
    public Map<Identifier, List<Resource>> listResourceStacks(String p_215416_, Predicate<Identifier> p_215417_) {
        Map<Identifier, FallbackResourceManager.EntryStack> map = Maps.newHashMap();

        for (FallbackResourceManager.PackEntry fallbackresourcemanager$packentry : this.fallbacks) {
            applyPackFiltersToExistingResources(fallbackresourcemanager$packentry, map);
            this.listPackResources(fallbackresourcemanager$packentry, p_215416_, p_215417_, map);
        }

        TreeMap<Identifier, List<Resource>> treemap = Maps.newTreeMap();

        for (FallbackResourceManager.EntryStack fallbackresourcemanager$entrystack : map.values()) {
            if (!fallbackresourcemanager$entrystack.fileSources.isEmpty()) {
                List<Resource> list = new ArrayList<>();

                for (FallbackResourceManager.ResourceWithSource fallbackresourcemanager$resourcewithsource : fallbackresourcemanager$entrystack.fileSources) {
                    PackResources packresources = fallbackresourcemanager$resourcewithsource.source;
                    IoSupplier<InputStream> iosupplier = fallbackresourcemanager$entrystack.metaSources.get(packresources);
                    IoSupplier<ResourceMetadata> iosupplier1 = iosupplier != null ? convertToMetadata(iosupplier) : ResourceMetadata.EMPTY_SUPPLIER;
                    list.add(
                        createResource(
                            packresources, fallbackresourcemanager$entrystack.fileLocation, fallbackresourcemanager$resourcewithsource.resource, iosupplier1
                        )
                    );
                }

                treemap.put(fallbackresourcemanager$entrystack.fileLocation, list);
            }
        }

        return treemap;
    }

    @Override
    public Stream<PackResources> listPacks() {
        return this.fallbacks.stream().map(p_215386_ -> p_215386_.resources).filter(Objects::nonNull);
    }

    record EntryStack(
        Identifier fileLocation,
        Identifier metadataLocation,
        List<FallbackResourceManager.ResourceWithSource> fileSources,
        Map<PackResources, IoSupplier<InputStream>> metaSources
    ) {
        EntryStack(Identifier p_467793_) {
            this(p_467793_, FallbackResourceManager.getMetadataLocation(p_467793_), new ArrayList<>(), new Object2ObjectArrayMap<>());
        }
    }

    static class LeakedResourceWarningInputStream extends FilterInputStream {
        private final Supplier<String> message;
        private boolean closed;

        public LeakedResourceWarningInputStream(InputStream inputStream, Identifier resourceLocation, String packName) {
            super(inputStream);
            Exception exception = new Exception("Stacktrace");
            this.message = () -> {
                StringWriter stringwriter = new StringWriter();
                exception.printStackTrace(new PrintWriter(stringwriter));
                return "Leaked resource: '" + resourceLocation + "' loaded from pack: '" + packName + "'\n" + stringwriter;
            };
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.closed = true;
        }

        @Override
        protected void finalize() throws Throwable {
            if (!this.closed) {
                FallbackResourceManager.LOGGER.warn("{}", this.message.get());
            }

            super.finalize();
        }
    }

    record PackEntry(String name, @Nullable PackResources resources, @Nullable Predicate<Identifier> filter) {
        public void filterAll(Collection<Identifier> locations) {
            if (this.filter != null) {
                locations.removeIf(this.filter);
            }
        }

        public boolean isFiltered(Identifier location) {
            return this.filter != null && this.filter.test(location);
        }
    }

    record ResourceWithSource(PackResources source, IoSupplier<InputStream> resource) {
    }
}
