package net.minecraft.resources;

import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public class FileToIdConverter {
    private final String prefix;
    private final String extension;

    public FileToIdConverter(String prefix, String extension) {
        this.prefix = prefix;
        this.extension = extension;
    }

    public static FileToIdConverter json(String name) {
        return new FileToIdConverter(name, ".json");
    }

    public static FileToIdConverter registry(ResourceKey<? extends Registry<?>> registryKey) {
        return json(Registries.elementsDirPath(registryKey));
    }

    public Identifier idToFile(Identifier id) {
        return id.withPath(this.prefix + "/" + id.getPath() + this.extension);
    }

    public Identifier fileToId(Identifier file) {
        String s = file.getPath();
        return file.withPath(s.substring(this.prefix.length() + 1, s.length() - this.extension.length()));
    }

    public Map<Identifier, Resource> listMatchingResources(ResourceManager resourceManager) {
        return resourceManager.listResources(this.prefix, p_466119_ -> p_466119_.getPath().endsWith(this.extension));
    }

    public Map<Identifier, List<Resource>> listMatchingResourceStacks(ResourceManager resourceManager) {
        return resourceManager.listResourceStacks(this.prefix, p_466118_ -> p_466118_.getPath().endsWith(this.extension));
    }

    /**
     * List all resources under the given namespace which match this converter
     *
     * @param manager   The resource manager to collect the resources from
     * @param namespace The namespace to search under
     * @return All resources from the given namespace which match this converter
     */
    public Map<Identifier, Resource> listMatchingResourcesFromNamespace(ResourceManager manager, String namespace) {
        return manager.listResources(this.prefix, path -> path.getNamespace().equals(namespace) && path.getPath().endsWith(this.extension));
    }

    /**
     * List all resource stacks under the given namespace which match this converter
     *
     * @param manager   The resource manager to collect the resources from
     * @param namespace The namespace to search under
     * @return All resource stacks from the given namespace which match this converter
     */
    public Map<Identifier, List<Resource>> listMatchingResourceStacksFromNamespace(ResourceManager manager, String namespace) {
        return manager.listResourceStacks(this.prefix, path -> path.getNamespace().equals(namespace) && path.getPath().endsWith(this.extension));
    }
}
