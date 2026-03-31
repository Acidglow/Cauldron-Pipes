package net.minecraft.world.level.storage.loot.functions;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import org.slf4j.Logger;

public class FunctionReference extends LootItemConditionalFunction {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final MapCodec<FunctionReference> CODEC = RecordCodecBuilder.mapCodec(
        p_335341_ -> commonFields(p_335341_)
            .and(ResourceKey.codec(Registries.ITEM_MODIFIER).fieldOf("name").forGetter(p_335337_ -> p_335337_.name))
            .apply(p_335341_, FunctionReference::new)
    );
    private final ResourceKey<LootItemFunction> name;

    private FunctionReference(List<LootItemCondition> conditions, ResourceKey<LootItemFunction> name) {
        super(conditions);
        this.name = name;
    }

    @Override
    public LootItemFunctionType<FunctionReference> getType() {
        return LootItemFunctions.REFERENCE;
    }

    @Override
    public void validate(ValidationContext p_279281_) {
        if (!p_279281_.allowsReferences()) {
            p_279281_.reportProblem(new ValidationContext.ReferenceNotAllowedProblem(this.name));
        } else if (p_279281_.hasVisitedElement(this.name)) {
            p_279281_.reportProblem(new ValidationContext.RecursiveReferenceProblem(this.name));
        } else {
            super.validate(p_279281_);
            p_279281_.resolver()
                .get(this.name)
                .ifPresentOrElse(
                    p_421469_ -> p_421469_.value().validate(p_279281_.enterElement(new ProblemReporter.ElementReferencePathElement(this.name), this.name)),
                    () -> p_279281_.reportProblem(new ValidationContext.MissingReferenceProblem(this.name))
                );
        }
    }

    @Override
    protected ItemStack run(ItemStack p_279458_, LootContext p_279370_) {
        LootItemFunction lootitemfunction = p_279370_.getResolver().get(this.name).map(Holder::value).orElse(null);
        if (lootitemfunction == null) {
            LOGGER.warn("Unknown function: {}", this.name.identifier());
            return p_279458_;
        } else {
            LootContext.VisitedEntry<?> visitedentry = LootContext.createVisitedEntry(lootitemfunction);
            if (p_279370_.pushVisitedElement(visitedentry)) {
                ItemStack itemstack;
                try {
                    itemstack = lootitemfunction.apply(p_279458_, p_279370_);
                } finally {
                    p_279370_.popVisitedElement(visitedentry);
                }

                return itemstack;
            } else {
                LOGGER.warn("Detected infinite loop in loot tables");
                return p_279458_;
            }
        }
    }

    public static LootItemConditionalFunction.Builder<?> functionReference(ResourceKey<LootItemFunction> key) {
        return simpleBuilder(p_335336_ -> new FunctionReference(p_335336_, key));
    }
}
