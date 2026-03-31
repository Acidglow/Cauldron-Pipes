package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.criterion.ItemPredicate;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class FilteredFunction extends LootItemConditionalFunction {
    public static final MapCodec<FilteredFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_466792_ -> commonFields(p_466792_)
            .and(
                p_466792_.group(
                    ItemPredicate.CODEC.fieldOf("item_filter").forGetter(p_466791_ -> p_466791_.filter),
                    LootItemFunctions.ROOT_CODEC.optionalFieldOf("on_pass").forGetter(p_458899_ -> p_458899_.onPass),
                    LootItemFunctions.ROOT_CODEC.optionalFieldOf("on_fail").forGetter(p_458898_ -> p_458898_.onFail)
                )
            )
            .apply(p_466792_, FilteredFunction::new)
    );
    private final ItemPredicate filter;
    private final Optional<LootItemFunction> onPass;
    private final Optional<LootItemFunction> onFail;

    public FilteredFunction(List<LootItemCondition> conditions, ItemPredicate filter, Optional<LootItemFunction> onPass, Optional<LootItemFunction> onFail) {
        super(conditions);
        this.filter = filter;
        this.onPass = onPass;
        this.onFail = onFail;
    }

    @Override
    public LootItemFunctionType<FilteredFunction> getType() {
        return LootItemFunctions.FILTERED;
    }

    @Override
    public ItemStack run(ItemStack p_340845_, LootContext p_341349_) {
        Optional<LootItemFunction> optional = this.filter.test(p_340845_) ? this.onPass : this.onFail;
        return optional.isPresent() ? optional.get().apply(p_340845_, p_341349_) : p_340845_;
    }

    @Override
    public void validate(ValidationContext p_341254_) {
        super.validate(p_341254_);
        this.onPass.ifPresent(p_458904_ -> p_458904_.validate(p_341254_.forChild(new ProblemReporter.FieldPathElement("on_pass"))));
        this.onFail.ifPresent(p_458902_ -> p_458902_.validate(p_341254_.forChild(new ProblemReporter.FieldPathElement("on_fail"))));
    }

    public static FilteredFunction.Builder filtered(ItemPredicate itemPredicate) {
        return new FilteredFunction.Builder(itemPredicate);
    }

    public static class Builder extends LootItemConditionalFunction.Builder<FilteredFunction.Builder> {
        private final ItemPredicate itemPredicate;
        private Optional<LootItemFunction> onPass = Optional.empty();
        private Optional<LootItemFunction> onFail = Optional.empty();

        Builder(ItemPredicate itemPredicate) {
            this.itemPredicate = itemPredicate;
        }

        protected FilteredFunction.Builder getThis() {
            return this;
        }

        public FilteredFunction.Builder onPass(Optional<LootItemFunction> onPass) {
            this.onPass = onPass;
            return this;
        }

        public FilteredFunction.Builder onFail(Optional<LootItemFunction> onFail) {
            this.onFail = onFail;
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new FilteredFunction(this.getConditions(), this.itemPredicate, this.onPass, this.onFail);
        }
    }
}
