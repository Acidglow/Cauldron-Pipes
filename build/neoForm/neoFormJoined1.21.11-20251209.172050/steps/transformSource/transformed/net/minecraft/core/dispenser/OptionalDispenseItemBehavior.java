package net.minecraft.core.dispenser;

public abstract class OptionalDispenseItemBehavior extends DefaultDispenseItemBehavior {
    private boolean success = true;

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    @Override
    protected void playSound(BlockSource p_302438_) {
        p_302438_.level().levelEvent(this.isSuccess() ? 1000 : 1001, p_302438_.pos(), 0);
    }
}
