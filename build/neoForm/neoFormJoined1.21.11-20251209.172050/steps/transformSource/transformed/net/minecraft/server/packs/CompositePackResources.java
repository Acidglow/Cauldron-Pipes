package net.minecraft.server.packs;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jspecify.annotations.Nullable;

public class CompositePackResources implements PackResources {
    private final PackResources primaryPackResources;
    private final List<PackResources> packResourcesStack;

    public CompositePackResources(PackResources primaryPackResources, List<PackResources> packResourcesStack) {
        this.primaryPackResources = primaryPackResources;
        List<PackResources> list = new ArrayList<>(packResourcesStack.size() + 1);
        list.addAll(Lists.reverse(packResourcesStack));
        list.add(primaryPackResources);
        this.packResourcesStack = List.copyOf(list);
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... p_295316_) {
        return this.primaryPackResources.getRootResource(p_295316_);
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType p_295406_, Identifier p_466893_) {
        for (PackResources packresources : this.packResourcesStack) {
            IoSupplier<InputStream> iosupplier = packresources.getResource(p_295406_, p_466893_);
            if (iosupplier != null) {
                return iosupplier;
            }
        }

        return null;
    }

    @Override
    public void listResources(PackType p_295490_, String p_296164_, String p_294691_, PackResources.ResourceOutput p_295313_) {
        Map<Identifier, IoSupplier<InputStream>> map = new HashMap<>();

        for (PackResources packresources : this.packResourcesStack) {
            packresources.listResources(p_295490_, p_296164_, p_294691_, map::putIfAbsent);
        }

        map.forEach(p_295313_);
    }

    @Override
    public Set<String> getNamespaces(PackType p_294708_) {
        Set<String> set = new HashSet<>();

        for (PackResources packresources : this.packResourcesStack) {
            set.addAll(packresources.getNamespaces(p_294708_));
        }

        return set;
    }

    @Override
    public <T> @Nullable T getMetadataSection(MetadataSectionType<T> p_389616_) throws IOException {
        return this.primaryPackResources.getMetadataSection(p_389616_);
    }

    @Override
    public PackLocationInfo location() {
        return this.primaryPackResources.location();
    }

    @Override
    public void close() {
        this.packResourcesStack.forEach(PackResources::close);
    }
}
