package net.minecraft.server.packs;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult.Error;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.util.FileUtil;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class VanillaPackResources implements PackResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final PackLocationInfo location;
    private final BuiltInMetadata metadata;
    private final Set<String> namespaces;
    private final List<Path> rootPaths;
    private final Map<PackType, List<Path>> pathsForType;

    VanillaPackResources(
        PackLocationInfo location, BuiltInMetadata metadata, Set<String> namespaces, List<Path> rootPaths, Map<PackType, List<Path>> pathsForType
    ) {
        this.location = location;
        this.metadata = metadata;
        this.namespaces = namespaces;
        this.rootPaths = rootPaths;
        this.pathsForType = pathsForType;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... p_250530_) {
        FileUtil.validatePath(p_250530_);
        List<String> list = List.of(p_250530_);

        for (Path path : this.rootPaths) {
            Path path1 = FileUtil.resolvePath(path, list);
            if (Files.exists(path1) && PathPackResources.validatePath(path1)) {
                return IoSupplier.create(path1);
            }
        }

        return null;
    }

    public void listRawPaths(PackType packType, Identifier packLocation, Consumer<Path> output) {
        FileUtil.decomposePath(packLocation.getPath()).ifSuccess(p_466355_ -> {
            String s = packLocation.getNamespace();

            for (Path path : this.pathsForType.get(packType)) {
                Path path1 = path.resolve(s);
                output.accept(FileUtil.resolvePath(path1, (List<String>)p_466355_));
            }
        }).ifError(p_337562_ -> LOGGER.error("Invalid path {}: {}", packLocation, p_337562_.message()));
    }

    @Override
    public void listResources(PackType p_248974_, String p_248703_, String p_250848_, PackResources.ResourceOutput p_249668_) {
        FileUtil.decomposePath(p_250848_).ifSuccess(p_248228_ -> {
            List<Path> list = this.pathsForType.get(p_248974_);
            int i = list.size();
            if (i == 1) {
                getResources(p_249668_, p_248703_, list.get(0), (List<String>)p_248228_);
            } else if (i > 1) {
                Map<Identifier, IoSupplier<InputStream>> map = new HashMap<>();

                for (int j = 0; j < i - 1; j++) {
                    getResources(map::putIfAbsent, p_248703_, list.get(j), (List<String>)p_248228_);
                }

                Path path = list.get(i - 1);
                if (map.isEmpty()) {
                    getResources(p_249668_, p_248703_, path, (List<String>)p_248228_);
                } else {
                    getResources(map::putIfAbsent, p_248703_, path, (List<String>)p_248228_);
                    map.forEach(p_249668_);
                }
            }
        }).ifError(p_337564_ -> LOGGER.error("Invalid path {}: {}", p_250848_, p_337564_.message()));
    }

    private static void getResources(PackResources.ResourceOutput resourceOutput, String namespace, Path root, List<String> paths) {
        Path path = root.resolve(namespace);
        PathPackResources.listPath(namespace, path, paths, resourceOutput);
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType p_250512_, Identifier p_466991_) {
        return FileUtil.decomposePath(p_466991_.getPath()).mapOrElse(p_466350_ -> {
            String s = p_466991_.getNamespace();

            for (Path path : this.pathsForType.get(p_250512_)) {
                Path path1 = FileUtil.resolvePath(path.resolve(s), (List<String>)p_466350_);
                if (Files.exists(path1) && PathPackResources.validatePath(path1)) {
                    return IoSupplier.create(path1);
                }
            }

            return null;
        }, p_337566_ -> {
            LOGGER.error("Invalid path {}: {}", p_466991_, p_337566_.message());
            return null;
        });
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return this.namespaces;
    }

    @Override
    public <T> @Nullable T getMetadataSection(MetadataSectionType<T> p_389612_) {
        IoSupplier<InputStream> iosupplier = this.getRootResource("pack.mcmeta");
        if (iosupplier != null) {
            try (InputStream inputstream = iosupplier.get()) {
                T t = AbstractPackResources.getMetadataFromStream(p_389612_, inputstream, this.location);
                if (t != null) {
                    return t;
                }

                return this.metadata.get(p_389612_);
            } catch (IOException ioexception) {
            }
        }

        return this.metadata.get(p_389612_);
    }

    @Override
    public PackLocationInfo location() {
        return this.location;
    }

    @Override
    public void close() {
    }

    public ResourceProvider asProvider() {
        return p_466351_ -> Optional.ofNullable(this.getResource(PackType.CLIENT_RESOURCES, p_466351_))
            .map(p_248221_ -> new Resource(this, (IoSupplier<InputStream>)p_248221_));
    }
}
