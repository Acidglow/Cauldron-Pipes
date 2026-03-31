package net.minecraft.world.level.storage.loot.entries;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.MapCodec;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A composite loot pool entry container that expands all its children in order until one of them succeeds.
 * This container succeeds if one of its children succeeds.
 */
public class AlternativesEntry extends CompositeEntryBase {
    public static final MapCodec<AlternativesEntry> CODEC = createCodec(AlternativesEntry::new);
    public static final ProblemReporter.Problem UNREACHABLE_PROBLEM = new ProblemReporter.Problem() {
        @Override
        public String description() {
            return "Unreachable entry!";
        }
    };

    AlternativesEntry(List<LootPoolEntryContainer> p_299062_, List<LootItemCondition> p_298668_) {
        super(p_299062_, p_298668_);
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.ALTERNATIVES;
    }

    @Override
    protected ComposableEntryContainer compose(List<? extends ComposableEntryContainer> p_298256_) {
        return switch (p_298256_.size()) {
            case 0 -> ALWAYS_FALSE;
            case 1 -> (ComposableEntryContainer)p_298256_.get(0);
            case 2 -> p_298256_.get(0).or(p_298256_.get(1));
            default -> (p_298004_, p_298005_) -> {
                for (ComposableEntryContainer composableentrycontainer : p_298256_) {
                    if (composableentrycontainer.expand(p_298004_, p_298005_)) {
                        return true;
                    }
                }

                return false;
            };
        };
    }

    @Override
    public void validate(ValidationContext p_79388_) {
        super.validate(p_79388_);

        for (int i = 0; i < this.children.size() - 1; i++) {
            if (this.children.get(i).conditions.isEmpty()) {
                p_79388_.reportProblem(UNREACHABLE_PROBLEM);
            }
        }
    }

    public static AlternativesEntry.Builder alternatives(LootPoolEntryContainer.Builder<?>... children) {
        return new AlternativesEntry.Builder(children);
    }

    public static <E> AlternativesEntry.Builder alternatives(Collection<E> childrenSources, Function<E, LootPoolEntryContainer.Builder<?>> toChildrenFunction) {
        return new AlternativesEntry.Builder(childrenSources.stream().map(toChildrenFunction::apply).toArray(LootPoolEntryContainer.Builder[]::new));
    }

    public static class Builder extends LootPoolEntryContainer.Builder<AlternativesEntry.Builder> {
        private final ImmutableList.Builder<LootPoolEntryContainer> entries = ImmutableList.builder();

        public Builder(LootPoolEntryContainer.Builder<?>... children) {
            for (LootPoolEntryContainer.Builder<?> builder : children) {
                this.entries.add(builder.build());
            }
        }

        protected AlternativesEntry.Builder getThis() {
            return this;
        }

        @Override
        public AlternativesEntry.Builder otherwise(LootPoolEntryContainer.Builder<?> p_79402_) {
            this.entries.add(p_79402_.build());
            return this;
        }

        @Override
        public LootPoolEntryContainer build() {
            return new AlternativesEntry(this.entries.build(), this.getConditions());
        }
    }
}
