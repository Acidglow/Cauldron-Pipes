package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import org.jspecify.annotations.Nullable;

public class DefaultedMappedRegistry<T> extends MappedRegistry<T> implements DefaultedRegistry<T> {
    private final Identifier defaultKey;
    private Holder.Reference<T> defaultValue;

    public DefaultedMappedRegistry(String defaultKey, ResourceKey<? extends Registry<T>> key, Lifecycle registryLifecycle, boolean hasIntrusiveHolders) {
        super(key, registryLifecycle, hasIntrusiveHolders);
        this.defaultKey = Identifier.parse(defaultKey);
    }

    @Override
    public Holder.Reference<T> register(ResourceKey<T> p_321803_, T p_321739_, RegistrationInfo p_325995_) {
        Holder.Reference<T> reference = super.register(p_321803_, p_321739_, p_325995_);
        if (this.defaultKey.equals(p_321803_.identifier())) {
            this.defaultValue = reference;
        }

        return reference;
    }

    @Override
    public int getId(@Nullable T p_260033_) {
        int i = super.getId(p_260033_);
        return i == -1 ? super.getId(this.defaultValue.value()) : i;
    }

    @Override
    public Identifier getKey(T p_259233_) {
        Identifier identifier = super.getKey(p_259233_);
        return identifier == null ? this.defaultKey : identifier;
    }

    @Nullable
    @Override
    public Identifier getKeyOrNull(T element) {
        return super.getKey(element);
    }

    @Override
    public T getValue(@Nullable Identifier p_468616_) {
        T t = super.getValue(p_468616_);
        return t == null ? this.defaultValue.value() : t;
    }

    @Override
    public Optional<T> getOptional(@Nullable Identifier p_468535_) {
        return Optional.ofNullable(super.getValue(p_468535_));
    }

    @Override
    public Optional<Holder.Reference<T>> getAny() {
        return Optional.ofNullable(this.defaultValue);
    }

    @Override
    public T byId(int p_259534_) {
        T t = super.byId(p_259534_);
        return t == null ? this.defaultValue.value() : t;
    }

    @Override
    public Optional<Holder.Reference<T>> getRandom(RandomSource p_260255_) {
        return super.getRandom(p_260255_).or(() -> Optional.of(this.defaultValue));
    }

    @Override
    public Identifier getDefaultKey() {
        return this.defaultKey;
    }
}
