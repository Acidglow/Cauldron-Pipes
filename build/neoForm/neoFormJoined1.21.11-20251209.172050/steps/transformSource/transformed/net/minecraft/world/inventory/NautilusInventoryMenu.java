package net.minecraft.world.inventory;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.nautilus.AbstractNautilus;
import net.minecraft.world.entity.player.Inventory;

public class NautilusInventoryMenu extends AbstractMountInventoryMenu {
    private static final Identifier SADDLE_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/saddle");
    private static final Identifier ARMOR_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot/nautilus_armor_inventory");

    public NautilusInventoryMenu(int containerId, Inventory playerInventory, Container mountContainer, final AbstractNautilus mount, int inventoryColumns) {
        super(containerId, playerInventory, mountContainer, mount);
        Container container = mount.createEquipmentSlotContainer(EquipmentSlot.SADDLE);
        this.addSlot(new ArmorSlot(container, mount, EquipmentSlot.SADDLE, 0, 8, 18, SADDLE_SLOT_SPRITE) {
            @Override
            public boolean isActive() {
                return mount.canUseSlot(EquipmentSlot.SADDLE);
            }
        });
        Container container1 = mount.createEquipmentSlotContainer(EquipmentSlot.BODY);
        this.addSlot(new ArmorSlot(container1, mount, EquipmentSlot.BODY, 0, 8, 36, ARMOR_SLOT_SPRITE) {
            @Override
            public boolean isActive() {
                return mount.canUseSlot(EquipmentSlot.BODY);
            }
        });
        this.addStandardInventorySlots(playerInventory, 8, 84);
    }

    @Override
    protected boolean hasInventoryChanged(Container p_470578_) {
        return ((AbstractNautilus)this.mount).hasInventoryChanged(p_470578_);
    }
}
