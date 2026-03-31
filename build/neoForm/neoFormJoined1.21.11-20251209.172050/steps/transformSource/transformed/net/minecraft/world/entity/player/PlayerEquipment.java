package net.minecraft.world.entity.player;

import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class PlayerEquipment extends EntityEquipment {
    private final Player player;

    public PlayerEquipment(Player player) {
        this.player = player;
    }

    @Override
    public ItemStack set(EquipmentSlot p_401903_, ItemStack p_401882_) {
        return p_401903_ == EquipmentSlot.MAINHAND ? this.player.getInventory().setSelectedItem(p_401882_) : super.set(p_401903_, p_401882_);
    }

    @Override
    public ItemStack get(EquipmentSlot p_401776_) {
        return p_401776_ == EquipmentSlot.MAINHAND ? this.player.getInventory().getSelectedItem() : super.get(p_401776_);
    }

    @Override
    public boolean isEmpty() {
        return this.player.getInventory().getSelectedItem().isEmpty() && super.isEmpty();
    }
}
