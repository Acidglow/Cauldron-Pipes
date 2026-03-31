package net.minecraft.world.item.slot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.Function;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;

public abstract class CompositeSlotSource implements SlotSource {
    protected final List<SlotSource> terms;
    private final Function<LootContext, SlotCollection> compositeSlotSource;

    protected CompositeSlotSource(List<SlotSource> terms) {
        this.terms = terms;
        this.compositeSlotSource = SlotSources.group(terms);
    }

    protected static <T extends CompositeSlotSource> MapCodec<T> createCodec(Function<List<SlotSource>, T> factory) {
        return RecordCodecBuilder.mapCodec(
            p_461057_ -> p_461057_.group(SlotSources.CODEC.listOf().fieldOf("terms").forGetter(p_461066_ -> p_461066_.terms)).apply(p_461057_, factory)
        );
    }

    protected static <T extends CompositeSlotSource> Codec<T> createInlineCodec(Function<List<SlotSource>, T> factory) {
        return SlotSources.CODEC.listOf().xmap(factory, p_461243_ -> p_461243_.terms);
    }

    @Override
    public abstract MapCodec<? extends CompositeSlotSource> codec();

    @Override
    public SlotCollection provide(LootContext p_460833_) {
        return this.compositeSlotSource.apply(p_460833_);
    }

    @Override
    public void validate(ValidationContext p_460875_) {
        SlotSource.super.validate(p_460875_);

        for (int i = 0; i < this.terms.size(); i++) {
            this.terms.get(i).validate(p_460875_.forChild(new ProblemReporter.IndexedFieldPathElement("terms", i)));
        }
    }
}
