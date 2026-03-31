package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.slot.SlotSource;
import net.minecraft.world.item.slot.SlotSources;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SlotLoot extends LootPoolSingletonContainer {
    public static final MapCodec<SlotLoot> CODEC = RecordCodecBuilder.mapCodec(
        p_460683_ -> p_460683_.group(SlotSources.CODEC.fieldOf("slot_source").forGetter(p_461227_ -> p_461227_.slotSource))
            .and(singletonFields(p_460683_))
            .apply(p_460683_, SlotLoot::new)
    );
    private final SlotSource slotSource;

    private SlotLoot(SlotSource slotSource, int weight, int quality, List<LootItemCondition> conditions, List<LootItemFunction> functions) {
        super(weight, quality, conditions, functions);
        this.slotSource = slotSource;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.SLOTS;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> p_460754_, LootContext p_461257_) {
        this.slotSource.provide(p_461257_).itemCopies().filter(p_460967_ -> !p_460967_.isEmpty()).forEach(p_460754_);
    }

    @Override
    public void validate(ValidationContext p_460975_) {
        super.validate(p_460975_);
        this.slotSource.validate(p_460975_.forChild(new ProblemReporter.FieldPathElement("slot_source")));
    }
}
