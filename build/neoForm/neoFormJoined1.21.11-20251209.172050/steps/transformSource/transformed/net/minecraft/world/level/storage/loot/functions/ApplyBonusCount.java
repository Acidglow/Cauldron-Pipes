package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * LootItemFunction that modifies the stack's count based on an enchantment level on the {@linkplain LootContextParams#TOOL tool} using various formulas.
 */
public class ApplyBonusCount extends LootItemConditionalFunction {
    private static final Map<Identifier, ApplyBonusCount.FormulaType> FORMULAS = Stream.of(
            ApplyBonusCount.BinomialWithBonusCount.TYPE, ApplyBonusCount.OreDrops.TYPE, ApplyBonusCount.UniformBonusCount.TYPE
        )
        .collect(Collectors.toMap(ApplyBonusCount.FormulaType::id, Function.identity()));
    private static final Codec<ApplyBonusCount.FormulaType> FORMULA_TYPE_CODEC = Identifier.CODEC
        .comapFlatMap(
            p_469558_ -> {
                ApplyBonusCount.FormulaType applybonuscount$formulatype = FORMULAS.get(p_469558_);
                return applybonuscount$formulatype != null
                    ? DataResult.success(applybonuscount$formulatype)
                    : DataResult.error(() -> "No formula type with id: '" + p_469558_ + "'");
            },
            ApplyBonusCount.FormulaType::id
        );
    private static final MapCodec<ApplyBonusCount.Formula> FORMULA_CODEC = ExtraCodecs.dispatchOptionalValue(
        "formula", "parameters", FORMULA_TYPE_CODEC, ApplyBonusCount.Formula::getType, ApplyBonusCount.FormulaType::codec
    );
    public static final MapCodec<ApplyBonusCount> CODEC = RecordCodecBuilder.mapCodec(
        p_344674_ -> commonFields(p_344674_)
            .and(
                p_344674_.group(
                    Enchantment.CODEC.fieldOf("enchantment").forGetter(p_298051_ -> p_298051_.enchantment),
                    FORMULA_CODEC.forGetter(p_298061_ -> p_298061_.formula)
                )
            )
            .apply(p_344674_, ApplyBonusCount::new)
    );
    private final Holder<Enchantment> enchantment;
    private final ApplyBonusCount.Formula formula;

    private ApplyBonusCount(List<LootItemCondition> predicates, Holder<Enchantment> enchantment, ApplyBonusCount.Formula formula) {
        super(predicates);
        this.enchantment = enchantment;
        this.formula = formula;
    }

    @Override
    public LootItemFunctionType<ApplyBonusCount> getType() {
        return LootItemFunctions.APPLY_BONUS;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.TOOL);
    }

    @Override
    public ItemStack run(ItemStack stack, LootContext context) {
        ItemStack itemstack = context.getOptionalParameter(LootContextParams.TOOL);
        if (itemstack != null) {
            int i = EnchantmentHelper.getItemEnchantmentLevel(this.enchantment, itemstack);
            int j = this.formula.calculateNewCount(context.getRandom(), stack.getCount(), i);
            stack.setCount(j);
        }

        return stack;
    }

    public static LootItemConditionalFunction.Builder<?> addBonusBinomialDistributionCount(Holder<Enchantment> enchantment, float probability, int extraRounds) {
        return simpleBuilder(p_344680_ -> new ApplyBonusCount(p_344680_, enchantment, new ApplyBonusCount.BinomialWithBonusCount(extraRounds, probability)));
    }

    public static LootItemConditionalFunction.Builder<?> addOreBonusCount(Holder<Enchantment> enchantment) {
        return simpleBuilder(p_466790_ -> new ApplyBonusCount(p_466790_, enchantment, ApplyBonusCount.OreDrops.INSTANCE));
    }

    public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(Holder<Enchantment> enchantment) {
        return simpleBuilder(p_344676_ -> new ApplyBonusCount(p_344676_, enchantment, new ApplyBonusCount.UniformBonusCount(1)));
    }

    public static LootItemConditionalFunction.Builder<?> addUniformBonusCount(Holder<Enchantment> enchantment, int bonusMultiplier) {
        return simpleBuilder(p_344673_ -> new ApplyBonusCount(p_344673_, enchantment, new ApplyBonusCount.UniformBonusCount(bonusMultiplier)));
    }

    /**
     * Applies a bonus based on a binomial distribution with {@code n = enchantmentLevel + extraRounds} and {@code p = probability}.
     */
    record BinomialWithBonusCount(int extraRounds, float probability) implements ApplyBonusCount.Formula {
        private static final Codec<ApplyBonusCount.BinomialWithBonusCount> CODEC = RecordCodecBuilder.create(
            p_298226_ -> p_298226_.group(
                    Codec.INT.fieldOf("extra").forGetter(ApplyBonusCount.BinomialWithBonusCount::extraRounds),
                    Codec.FLOAT.fieldOf("probability").forGetter(ApplyBonusCount.BinomialWithBonusCount::probability)
                )
                .apply(p_298226_, ApplyBonusCount.BinomialWithBonusCount::new)
        );
        public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(
            Identifier.withDefaultNamespace("binomial_with_bonus_count"), CODEC
        );

        @Override
        public int calculateNewCount(RandomSource p_230965_, int p_230966_, int p_230967_) {
            for (int i = 0; i < p_230967_ + this.extraRounds; i++) {
                if (p_230965_.nextFloat() < this.probability) {
                    p_230966_++;
                }
            }

            return p_230966_;
        }

        @Override
        public ApplyBonusCount.FormulaType getType() {
            return TYPE;
        }
    }

    interface Formula {
        int calculateNewCount(RandomSource random, int originalCount, int enchantmentLevel);

        ApplyBonusCount.FormulaType getType();
    }

    record FormulaType(Identifier id, Codec<? extends ApplyBonusCount.Formula> codec) {
    }

    /**
     * Applies a bonus count with a special formula used for fortune ore drops.
     */
    record OreDrops() implements ApplyBonusCount.Formula {
        public static final ApplyBonusCount.OreDrops INSTANCE = new ApplyBonusCount.OreDrops();
        public static final Codec<ApplyBonusCount.OreDrops> CODEC = MapCodec.unitCodec(INSTANCE);
        public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(Identifier.withDefaultNamespace("ore_drops"), CODEC);

        @Override
        public int calculateNewCount(RandomSource p_230972_, int p_230973_, int p_230974_) {
            if (p_230974_ > 0) {
                int i = p_230972_.nextInt(p_230974_ + 2) - 1;
                if (i < 0) {
                    i = 0;
                }

                return p_230973_ * (i + 1);
            } else {
                return p_230973_;
            }
        }

        @Override
        public ApplyBonusCount.FormulaType getType() {
            return TYPE;
        }
    }

    /**
     * Adds a bonus count based on the enchantment level scaled by a constant multiplier.
     */
    record UniformBonusCount(int bonusMultiplier) implements ApplyBonusCount.Formula {
        public static final Codec<ApplyBonusCount.UniformBonusCount> CODEC = RecordCodecBuilder.create(
            p_298501_ -> p_298501_.group(Codec.INT.fieldOf("bonusMultiplier").forGetter(ApplyBonusCount.UniformBonusCount::bonusMultiplier))
                .apply(p_298501_, ApplyBonusCount.UniformBonusCount::new)
        );
        public static final ApplyBonusCount.FormulaType TYPE = new ApplyBonusCount.FormulaType(Identifier.withDefaultNamespace("uniform_bonus_count"), CODEC);

        @Override
        public int calculateNewCount(RandomSource p_230976_, int p_230977_, int p_230978_) {
            return p_230977_ + p_230976_.nextInt(this.bonusMultiplier * p_230978_ + 1);
        }

        @Override
        public ApplyBonusCount.FormulaType getType() {
            return TYPE;
        }
    }
}
