package net.minecraft.world.item.component;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.netty.buffer.ByteBuf;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;

public final class TypedEntityData<IdType> implements TooltipProvider {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TYPE_TAG = "id";
    final IdType type;
    final CompoundTag tag;

    public static <T> Codec<TypedEntityData<T>> codec(final Codec<T> idCodec) {
        return new Codec<TypedEntityData<T>>() {
            @Override
            public <V> DataResult<Pair<TypedEntityData<T>, V>> decode(DynamicOps<V> p_436802_, V p_436757_) {
                return CustomData.COMPOUND_TAG_CODEC
                    .decode(p_436802_, p_436757_)
                    .flatMap(
                        p_466655_ -> {
                            CompoundTag compoundtag = p_466655_.getFirst().copy();
                            Tag tag = compoundtag.remove("id");
                            return tag == null
                                ? DataResult.error(() -> "Expected 'id' field in " + p_436757_)
                                : idCodec.parse(asNbtOps((DynamicOps<T>)p_436802_), tag)
                                    .map(p_466658_ -> Pair.of((TypedEntityData<T>)(new TypedEntityData<>(p_466658_, compoundtag)), (V)p_466655_.getSecond()));
                        }
                    );
            }

            public <V> DataResult<V> encode(TypedEntityData<T> p_436720_, DynamicOps<V> p_436614_, V p_436650_) {
                return idCodec.encodeStart(asNbtOps((DynamicOps<T>)p_436614_), p_436720_.type).flatMap(p_436684_ -> {
                    CompoundTag compoundtag = p_436720_.tag.copy();
                    compoundtag.put("id", p_436684_);
                    return CustomData.COMPOUND_TAG_CODEC.encode(compoundtag, p_436614_, p_436650_);
                });
            }

            private static <T> DynamicOps<Tag> asNbtOps(DynamicOps<T> p_436750_) {
                return (DynamicOps<Tag>)(p_436750_ instanceof RegistryOps<T> registryops ? registryops.withParent(NbtOps.INSTANCE) : NbtOps.INSTANCE);
            }
        };
    }

    public static <B extends ByteBuf, T> StreamCodec<B, TypedEntityData<T>> streamCodec(StreamCodec<B, T> idCodec) {
        return StreamCodec.composite(
            idCodec,
            (Function<TypedEntityData<T>, T>)(TypedEntityData::type),
            ByteBufCodecs.COMPOUND_TAG,
            TypedEntityData::tag,
            (BiFunction<T, CompoundTag, TypedEntityData<T>>)(TypedEntityData::new)
        );
    }

    TypedEntityData(IdType type, CompoundTag tag) {
        this.type = type;
        this.tag = stripId(tag);
    }

    public static <T> TypedEntityData<T> of(T type, CompoundTag tag) {
        return new TypedEntityData<>(type, tag);
    }

    private static CompoundTag stripId(CompoundTag tag) {
        if (tag.contains("id")) {
            CompoundTag compoundtag = tag.copy();
            compoundtag.remove("id");
            return compoundtag;
        } else {
            return tag;
        }
    }

    public IdType type() {
        return this.type;
    }

    public boolean contains(String key) {
        return this.tag.contains(key);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else {
            return !(other instanceof TypedEntityData<?> typedentitydata)
                ? false
                : this.type == typedentitydata.type && this.tag.equals(typedentitydata.tag);
        }
    }

    @Override
    public int hashCode() {
        return 31 * this.type.hashCode() + this.tag.hashCode();
    }

    @Override
    public String toString() {
        return this.type + " " + this.tag;
    }

    public void loadInto(Entity entity) {
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, entity.registryAccess());
            entity.saveWithoutId(tagvalueoutput);
            CompoundTag compoundtag = tagvalueoutput.buildResult();
            UUID uuid = entity.getUUID();
            compoundtag.merge(this.getUnsafe());
            entity.load(TagValueInput.create(problemreporter$scopedcollector, entity.registryAccess(), compoundtag));
            entity.setUUID(uuid);
        }
    }

    public boolean loadInto(BlockEntity entity, HolderLookup.Provider registries) {
        boolean $$6;
        try (ProblemReporter.ScopedCollector problemreporter$scopedcollector = new ProblemReporter.ScopedCollector(entity.problemPath(), LOGGER)) {
            TagValueOutput tagvalueoutput = TagValueOutput.createWithContext(problemreporter$scopedcollector, registries);
            entity.saveCustomOnly(tagvalueoutput);
            CompoundTag compoundtag = tagvalueoutput.buildResult();
            CompoundTag compoundtag1 = compoundtag.copy();
            compoundtag.merge(this.getUnsafe());
            if (!compoundtag.equals(compoundtag1)) {
                try {
                    entity.loadCustomOnly(TagValueInput.create(problemreporter$scopedcollector, registries, compoundtag));
                    entity.setChanged();
                    return true;
                } catch (Exception exception1) {
                    LOGGER.warn("Failed to apply custom data to block entity at {}", entity.getBlockPos(), exception1);

                    try {
                        entity.loadCustomOnly(TagValueInput.create(problemreporter$scopedcollector.forChild(() -> "(rollback)"), registries, compoundtag1));
                    } catch (Exception exception) {
                        LOGGER.warn("Failed to rollback block entity at {} after failure", entity.getBlockPos(), exception);
                    }
                }
            }

            $$6 = false;
        }

        return $$6;
    }

    private CompoundTag tag() {
        return this.tag;
    }

    @Deprecated
    public CompoundTag getUnsafe() {
        return this.tag;
    }

    public CompoundTag copyTagWithoutId() {
        return this.tag.copy();
    }

    @Override
    public void addToTooltip(Item.TooltipContext p_435732_, Consumer<Component> p_433356_, TooltipFlag p_435305_, DataComponentGetter p_434480_) {
        if (this.type.getClass() == EntityType.class) {
            EntityType<?> entitytype = (EntityType<?>)this.type;
            if (p_435732_.isPeaceful() && !entitytype.isAllowedInPeaceful()) {
                p_433356_.accept(Component.translatable("item.spawn_egg.peaceful").withStyle(ChatFormatting.RED));
            }
        }
    }
}
