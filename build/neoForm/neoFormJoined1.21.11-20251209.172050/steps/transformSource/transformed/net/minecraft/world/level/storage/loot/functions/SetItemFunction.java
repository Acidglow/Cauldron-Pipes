package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetItemFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetItemFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_381617_ -> commonFields(p_381617_).and(Item.CODEC.fieldOf("item").forGetter(p_340993_ -> p_340993_.item)).apply(p_381617_, SetItemFunction::new)
    );
    private final Holder<Item> item;

    public SetItemFunction(List<LootItemCondition> conditions, Holder<Item> item) {
        super(conditions);
        this.item = item;
    }

    @Override
    public LootItemFunctionType<SetItemFunction> getType() {
        return LootItemFunctions.SET_ITEM;
    }

    @Override
    public ItemStack run(ItemStack p_340909_, LootContext p_341298_) {
        return p_340909_.transmuteCopy(this.item.value());
    }
}
