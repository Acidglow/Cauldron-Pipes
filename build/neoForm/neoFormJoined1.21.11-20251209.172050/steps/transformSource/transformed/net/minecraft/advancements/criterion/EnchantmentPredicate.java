package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public record EnchantmentPredicate(Optional<HolderSet<Enchantment>> enchantments, MinMaxBounds.Ints level) {
    public static final Codec<EnchantmentPredicate> CODEC = RecordCodecBuilder.create(
        p_467482_ -> p_467482_.group(
                RegistryCodecs.homogeneousList(Registries.ENCHANTMENT).optionalFieldOf("enchantments").forGetter(EnchantmentPredicate::enchantments),
                MinMaxBounds.Ints.CODEC.optionalFieldOf("levels", MinMaxBounds.Ints.ANY).forGetter(EnchantmentPredicate::level)
            )
            .apply(p_467482_, EnchantmentPredicate::new)
    );

    public EnchantmentPredicate(Holder<Enchantment> p_467292_, MinMaxBounds.Ints p_469595_) {
        this(Optional.of(HolderSet.direct(p_467292_)), p_469595_);
    }

    public EnchantmentPredicate(HolderSet<Enchantment> p_468396_, MinMaxBounds.Ints p_468943_) {
        this(Optional.of(p_468396_), p_468943_);
    }

    public boolean containedIn(ItemEnchantments enchantments) {
        if (this.enchantments.isPresent()) {
            for (Holder<Enchantment> holder : this.enchantments.get()) {
                if (this.matchesEnchantment(enchantments, holder)) {
                    return true;
                }
            }

            return false;
        } else if (this.level != MinMaxBounds.Ints.ANY) {
            for (Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                if (this.level.matches(entry.getIntValue())) {
                    return true;
                }
            }

            return false;
        } else {
            return !enchantments.isEmpty();
        }
    }

    private boolean matchesEnchantment(ItemEnchantments itemEnchantments, Holder<Enchantment> enchantment) {
        int i = itemEnchantments.getLevel(enchantment);
        if (i == 0) {
            return false;
        } else {
            return this.level == MinMaxBounds.Ints.ANY ? true : this.level.matches(i);
        }
    }
}
