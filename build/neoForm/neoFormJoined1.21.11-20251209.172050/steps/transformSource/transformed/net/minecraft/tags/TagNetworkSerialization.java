package net.minecraft.tags;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistrySynchronization;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RegistryLayer;

public class TagNetworkSerialization {
    public static Map<ResourceKey<? extends Registry<?>>, TagNetworkSerialization.NetworkPayload> serializeTagsToNetwork(
        LayeredRegistryAccess<RegistryLayer> registryAccess
    ) {
        return RegistrySynchronization.networkSafeRegistries(registryAccess)
            .map(p_203949_ -> Pair.of(p_203949_.key(), serializeToNetwork(p_203949_.value())))
            .filter(p_359657_ -> !p_359657_.getSecond().isEmpty())
            .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond));
    }

    private static <T> TagNetworkSerialization.NetworkPayload serializeToNetwork(Registry<T> registry) {
        Map<Identifier, IntList> map = new HashMap<>();
        registry.getTags().forEach(p_466428_ -> {
            IntList intlist = new IntArrayList(p_466428_.size());

            for (Holder<T> holder : p_466428_) {
                if (holder.kind() != Holder.Kind.REFERENCE) {
                    throw new IllegalStateException("Can't serialize unregistered value " + holder);
                }

                intlist.add(registry.getId(holder.value()));
            }

            map.put(p_466428_.key().location(), intlist);
        });
        return new TagNetworkSerialization.NetworkPayload(map);
    }

    static <T> TagLoader.LoadResult<T> deserializeTagsFromNetwork(Registry<T> registry, TagNetworkSerialization.NetworkPayload payload) {
        ResourceKey<? extends Registry<T>> resourcekey = registry.key();
        Map<TagKey<T>, List<Holder<T>>> map = new HashMap<>();
        payload.tags.forEach((p_466424_, p_466425_) -> {
            TagKey<T> tagkey = TagKey.create(resourcekey, p_466424_);
            List<Holder<T>> list = p_466425_.intStream().mapToObj(registry::get).flatMap(Optional::stream).collect(Collectors.toUnmodifiableList());
            map.put(tagkey, list);
        });
        return new TagLoader.LoadResult<>(resourcekey, map);
    }

    public static final class NetworkPayload {
        public static final TagNetworkSerialization.NetworkPayload EMPTY = new TagNetworkSerialization.NetworkPayload(Map.of());
        final Map<Identifier, IntList> tags;

        NetworkPayload(Map<Identifier, IntList> tags) {
            this.tags = tags;
        }

        public void write(FriendlyByteBuf buffer) {
            buffer.writeMap(this.tags, FriendlyByteBuf::writeIdentifier, FriendlyByteBuf::writeIntIdList);
        }

        public static TagNetworkSerialization.NetworkPayload read(FriendlyByteBuf buffer) {
            return new TagNetworkSerialization.NetworkPayload(buffer.readMap(FriendlyByteBuf::readIdentifier, FriendlyByteBuf::readIntIdList));
        }

        public boolean isEmpty() {
            return this.tags.isEmpty();
        }

        public int size() {
            return this.tags.size();
        }

        public <T> TagLoader.LoadResult<T> resolve(Registry<T> registry) {
            return TagNetworkSerialization.deserializeTagsFromNetwork(registry, this);
        }
    }
}
