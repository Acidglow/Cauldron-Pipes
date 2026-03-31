package net.minecraft.world.item.enchantment;

import net.minecraft.core.Holder;

/**
 * Defines an immutable instance of an enchantment and its level.
 */
public record EnchantmentInstance(Holder<Enchantment> enchantment, int level) {
    public int weight() {
        return this.enchantment().value().getWeight();
    }
}
