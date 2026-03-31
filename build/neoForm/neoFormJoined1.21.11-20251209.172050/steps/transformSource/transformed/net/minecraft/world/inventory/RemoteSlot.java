package net.minecraft.world.inventory;

import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.HashedStack;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public interface RemoteSlot {
    RemoteSlot PLACEHOLDER = new RemoteSlot() {
        @Override
        public void receive(HashedStack p_412712_) {
        }

        @Override
        public void force(ItemStack p_412418_) {
        }

        @Override
        public boolean matches(ItemStack p_412140_) {
            return true;
        }
    };

    void force(ItemStack stack);

    void receive(HashedStack stack);

    boolean matches(ItemStack stack);

    public static class Synchronized implements RemoteSlot {
        private final HashedPatchMap.HashGenerator hasher;
        private @Nullable ItemStack remoteStack = null;
        private @Nullable HashedStack remoteHash = null;

        public Synchronized(HashedPatchMap.HashGenerator hasher) {
            this.hasher = hasher;
        }

        @Override
        public void force(ItemStack p_412714_) {
            this.remoteStack = p_412714_.copy();
            this.remoteHash = null;
        }

        @Override
        public void receive(HashedStack p_412040_) {
            this.remoteStack = null;
            this.remoteHash = p_412040_;
        }

        @Override
        public boolean matches(ItemStack p_412453_) {
            if (this.remoteStack != null) {
                return ItemStack.matches(this.remoteStack, p_412453_);
            } else if (this.remoteHash != null && this.remoteHash.matches(p_412453_, this.hasher)) {
                this.remoteStack = p_412453_.copy();
                return true;
            } else {
                return false;
            }
        }

        public void copyFrom(RemoteSlot.Synchronized other) {
            this.remoteStack = other.remoteStack;
            this.remoteHash = other.remoteHash;
        }
    }
}
