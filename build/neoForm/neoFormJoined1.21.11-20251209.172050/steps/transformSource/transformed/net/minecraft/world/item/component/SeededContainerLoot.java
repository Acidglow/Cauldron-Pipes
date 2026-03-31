package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.storage.loot.LootTable;

public record SeededContainerLoot(ResourceKey<LootTable> lootTable, long seed) implements TooltipProvider {
    private static final Component UNKNOWN_CONTENTS = Component.translatable("item.container.loot_table.unknown");
    public static final Codec<SeededContainerLoot> CODEC = RecordCodecBuilder.create(
        p_404538_ -> p_404538_.group(
                LootTable.KEY_CODEC.fieldOf("loot_table").forGetter(SeededContainerLoot::lootTable),
                Codec.LONG.optionalFieldOf("seed", 0L).forGetter(SeededContainerLoot::seed)
            )
            .apply(p_404538_, SeededContainerLoot::new)
    );

    @Override
    public void addToTooltip(Item.TooltipContext p_399542_, Consumer<Component> p_399663_, TooltipFlag p_399773_, DataComponentGetter p_399905_) {
        p_399663_.accept(UNKNOWN_CONTENTS);
    }
}
