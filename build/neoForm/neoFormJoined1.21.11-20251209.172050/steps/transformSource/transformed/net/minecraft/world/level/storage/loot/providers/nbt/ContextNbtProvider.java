package net.minecraft.world.level.storage.loot.providers.nbt;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Set;
import net.minecraft.advancements.criterion.NbtPredicate;
import net.minecraft.nbt.Tag;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootContextArg;
import org.jspecify.annotations.Nullable;

/**
 * A NbtProvider that provides either the {@linkplain LootContextParams#BLOCK_ENTITY block entity}'s NBT data or an entity's NBT data based on an {@link LootContext.EntityTarget}.
 */
public class ContextNbtProvider implements NbtProvider {
    private static final Codec<LootContextArg<Tag>> GETTER_CODEC = LootContextArg.createArgCodec(
        p_460618_ -> p_460618_.anyBlockEntity(ContextNbtProvider.BlockEntitySource::new).anyEntity(ContextNbtProvider.EntitySource::new)
    );
    public static final MapCodec<ContextNbtProvider> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_298866_ -> p_298866_.group(GETTER_CODEC.fieldOf("target").forGetter(p_460617_ -> p_460617_.source)).apply(p_298866_, ContextNbtProvider::new)
    );
    public static final Codec<ContextNbtProvider> INLINE_CODEC = GETTER_CODEC.xmap(ContextNbtProvider::new, p_460616_ -> p_460616_.source);
    private final LootContextArg<Tag> source;

    private ContextNbtProvider(LootContextArg<Tag> source) {
        this.source = source;
    }

    @Override
    public LootNbtProviderType getType() {
        return NbtProviders.CONTEXT;
    }

    @Override
    public @Nullable Tag get(LootContext p_165573_) {
        return this.source.get(p_165573_);
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(this.source.contextParam());
    }

    public static NbtProvider forContextEntity(LootContext.EntityTarget entityTarget) {
        return new ContextNbtProvider(new ContextNbtProvider.EntitySource(entityTarget.contextParam()));
    }

    record BlockEntitySource(ContextKey<? extends BlockEntity> contextParam) implements LootContextArg.Getter<BlockEntity, Tag> {
        public Tag get(BlockEntity p_451111_) {
            return p_451111_.saveWithFullMetadata(p_451111_.getLevel().registryAccess());
        }
    }

    record EntitySource(ContextKey<? extends Entity> contextParam) implements LootContextArg.Getter<Entity, Tag> {
        public Tag get(Entity p_451381_) {
            return NbtPredicate.getEntityTagToCompare(p_451381_);
        }
    }
}
