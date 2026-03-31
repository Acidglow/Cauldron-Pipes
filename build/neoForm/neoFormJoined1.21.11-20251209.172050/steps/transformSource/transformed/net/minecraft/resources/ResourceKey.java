package net.minecraft.resources;

import com.google.common.collect.MapMaker;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;

/**
 * An immutable key for a resource, in terms of the name of its parent registry and its location in that registry.
 * <p>
 * {@link net.minecraft.core.Registry} uses this to return resource keys for registry objects via {@link net.minecraft.core.Registry#getResourceKey(Object)}. It also uses this class to store its name, with the parent registry name set to {@code minecraft:root}. When used in this way it is usually referred to as a "registry key".</p>
 * <p>
 * @param <T> The type of the resource represented by this {@code ResourceKey}, or the type of the registry if it is a registry key.
 * @see net.minecraft.resources.ResourceLocation
 */
public class ResourceKey<T> implements java.lang.Comparable<ResourceKey<?>> {
    private static final ConcurrentMap<ResourceKey.InternKey, ResourceKey<?>> VALUES = new MapMaker().weakValues().makeMap();
    /**
     * The name of the parent registry of the resource.
     */
    private final Identifier registryName;
    private final Identifier identifier;

    public static <T> Codec<ResourceKey<T>> codec(ResourceKey<? extends Registry<T>> registryKey) {
        return Identifier.CODEC.xmap(p_466139_ -> create(registryKey, p_466139_), ResourceKey::identifier);
    }

    public static <T> StreamCodec<ByteBuf, ResourceKey<T>> streamCodec(ResourceKey<? extends Registry<T>> registryKey) {
        return Identifier.STREAM_CODEC.map(p_466137_ -> create(registryKey, p_466137_), ResourceKey::identifier);
    }

    /**
     * Constructs a new {@code ResourceKey} for a resource with the specified {@code location} within the registry specified by the given {@code registryKey}.
     *
     * @return the created resource key. The registry name is set to the location of the specified {@code registryKey} and with the specified {@code location} as the location of the resource.
     */
    public static <T> ResourceKey<T> create(ResourceKey<? extends Registry<T>> registryKey, Identifier location) {
        return create(registryKey.identifier, location);
    }

    /**
     * @return the created registry key. The registry name is set to {@code minecraft:root} and the location the specified {@code registryName}.
     */
    public static <T> ResourceKey<Registry<T>> createRegistryKey(Identifier location) {
        return create(Registries.ROOT_REGISTRY_NAME, location);
    }

    private static <T> ResourceKey<T> create(Identifier registryName, Identifier location) {
        return (ResourceKey<T>)VALUES.computeIfAbsent(
            new ResourceKey.InternKey(registryName, location), p_466135_ -> new ResourceKey(p_466135_.registry, p_466135_.identifier)
        );
    }

    private ResourceKey(Identifier registryName, Identifier location) {
        this.registryName = registryName;
        this.identifier = location;
    }

    @Override
    public String toString() {
        return "ResourceKey[" + this.registryName + " / " + this.identifier + "]";
    }

    /**
     * @return {@code true} if this resource key is a direct child of the specified {@code registryKey}.
     */
    public boolean isFor(ResourceKey<? extends Registry<?>> registryKey) {
        return this.registryName.equals(registryKey.identifier());
    }

    public <E> Optional<ResourceKey<E>> cast(ResourceKey<? extends Registry<E>> registryKey) {
        return this.isFor(registryKey) ? Optional.of((ResourceKey<E>)this) : Optional.empty();
    }

    public Identifier identifier() {
        return this.identifier;
    }

    public Identifier registry() {
        return this.registryName;
    }

    public ResourceKey<Registry<T>> registryKey() {
        return createRegistryKey(this.registryName);
    }

    @Override
    public int compareTo(ResourceKey<?> o) {
        int ret = this.registry().compareTo(o.registry());
        if (ret == 0) ret = this.identifier().compareTo(o.identifier());
        return ret;
    }

    record InternKey(Identifier registry, Identifier identifier) {
    }
}
