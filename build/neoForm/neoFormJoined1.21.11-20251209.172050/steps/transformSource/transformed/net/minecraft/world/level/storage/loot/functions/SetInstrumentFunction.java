package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Instrument;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetInstrumentFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetInstrumentFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_298123_ -> commonFields(p_298123_)
            .and(TagKey.hashedCodec(Registries.INSTRUMENT).fieldOf("options").forGetter(p_298122_ -> p_298122_.options))
            .apply(p_298123_, SetInstrumentFunction::new)
    );
    private final TagKey<Instrument> options;

    private SetInstrumentFunction(List<LootItemCondition> conditions, TagKey<Instrument> options) {
        super(conditions);
        this.options = options;
    }

    @Override
    public LootItemFunctionType<SetInstrumentFunction> getType() {
        return LootItemFunctions.SET_INSTRUMENT;
    }

    @Override
    public ItemStack run(ItemStack p_231017_, LootContext p_231018_) {
        Registry<Instrument> registry = p_231018_.getLevel().registryAccess().lookupOrThrow(Registries.INSTRUMENT);
        Optional<Holder<Instrument>> optional = registry.getRandomElementOf(this.options, p_231018_.getRandom());
        if (optional.isPresent()) {
            p_231017_.set(DataComponents.INSTRUMENT, new InstrumentComponent(optional.get()));
        }

        return p_231017_;
    }

    public static LootItemConditionalFunction.Builder<?> setInstrumentOptions(TagKey<Instrument> instrumentOptions) {
        return simpleBuilder(p_298125_ -> new SetInstrumentFunction(p_298125_, instrumentOptions));
    }
}
