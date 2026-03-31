package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class DiscardItem extends LootItemConditionalFunction {
    public static final MapCodec<DiscardItem> CODEC = RecordCodecBuilder.mapCodec(p_459198_ -> commonFields(p_459198_).apply(p_459198_, DiscardItem::new));

    protected DiscardItem(List<LootItemCondition> p_459079_) {
        super(p_459079_);
    }

    @Override
    public LootItemFunctionType<DiscardItem> getType() {
        return LootItemFunctions.DISCARD;
    }

    @Override
    protected ItemStack run(ItemStack p_459120_, LootContext p_459191_) {
        return ItemStack.EMPTY;
    }

    public static LootItemConditionalFunction.Builder<?> discardItem() {
        return simpleBuilder(DiscardItem::new);
    }
}
