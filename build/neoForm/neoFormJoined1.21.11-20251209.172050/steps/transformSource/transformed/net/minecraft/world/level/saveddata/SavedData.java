package net.minecraft.world.level.saveddata;

public abstract class SavedData {
    private boolean dirty;

    public void setDirty() {
        this.setDirty(true);
    }

    /**
     * Sets the dirty state of this {@code SavedData}, whether it needs saving to disk.
     */
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public boolean isDirty() {
        return this.dirty;
    }
}
