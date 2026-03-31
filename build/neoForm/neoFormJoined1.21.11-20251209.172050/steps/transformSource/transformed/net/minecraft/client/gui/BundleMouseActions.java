package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2i;

@OnlyIn(Dist.CLIENT)
public class BundleMouseActions implements ItemSlotMouseAction {
    private final Minecraft minecraft;
    private final ScrollWheelHandler scrollWheelHandler;

    public BundleMouseActions(Minecraft minecraft) {
        this.minecraft = minecraft;
        this.scrollWheelHandler = new ScrollWheelHandler();
    }

    @Override
    public boolean matches(Slot p_360559_) {
        return p_360559_.getItem().is(ItemTags.BUNDLES);
    }

    @Override
    public boolean onMouseScrolled(double p_360390_, double p_362650_, int p_363161_, ItemStack p_364763_) {
        int i = BundleItem.getNumberOfItemsToShow(p_364763_);
        if (i == 0) {
            return false;
        } else {
            Vector2i vector2i = this.scrollWheelHandler.onMouseScroll(p_360390_, p_362650_);
            int j = vector2i.y == 0 ? -vector2i.x : vector2i.y;
            if (j != 0) {
                int k = BundleItem.getSelectedItem(p_364763_);
                int l = ScrollWheelHandler.getNextScrollWheelSelection(j, k, i);
                if (k != l) {
                    this.toggleSelectedBundleItem(p_364763_, p_363161_, l);
                }
            }

            return true;
        }
    }

    @Override
    public void onStopHovering(Slot p_363289_) {
        this.unselectedBundleItem(p_363289_.getItem(), p_363289_.index);
    }

    @Override
    public void onSlotClicked(Slot p_372932_, ClickType p_372800_) {
        if (p_372800_ == ClickType.QUICK_MOVE || p_372800_ == ClickType.SWAP) {
            this.unselectedBundleItem(p_372932_.getItem(), p_372932_.index);
        }
    }

    private void toggleSelectedBundleItem(ItemStack stack, int index, int nextIndex) {
        if (this.minecraft.getConnection() != null && nextIndex < BundleItem.getNumberOfItemsToShow(stack)) {
            ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
            BundleItem.toggleSelectedItem(stack, nextIndex);
            clientpacketlistener.send(new ServerboundSelectBundleItemPacket(index, nextIndex));
        }
    }

    public void unselectedBundleItem(ItemStack bundle, int slotIndex) {
        this.toggleSelectedBundleItem(bundle, slotIndex, -1);
    }
}
