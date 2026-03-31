package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A loot pool entry container that will generate the dynamic drops with a given name.
 *
 * @see LootContext.DynamicDrops
 */
public class DynamicLoot extends LootPoolSingletonContainer {
    public static final MapCodec<DynamicLoot> CODEC = RecordCodecBuilder.mapCodec(
        p_466782_ -> p_466782_.group(Identifier.CODEC.fieldOf("name").forGetter(p_466783_ -> p_466783_.name))
            .and(singletonFields(p_466782_))
            .apply(p_466782_, DynamicLoot::new)
    );
    private final Identifier name;

    private DynamicLoot(Identifier name, int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions) {
        super(weight, quality, conditions, functions);
        this.name = name;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.DYNAMIC;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> p_79481_, LootContext p_79482_) {
        p_79482_.addDynamicDrops(this.name, p_79481_);
    }

    public static LootPoolSingletonContainer.Builder<?> dynamicEntry(Identifier dynamicDropsName) {
        return simpleBuilder((p_466785_, p_466786_, p_466787_, p_466788_) -> new DynamicLoot(dynamicDropsName, p_466785_, p_466786_, p_466787_, p_466788_));
    }
}
