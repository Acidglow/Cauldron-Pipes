package net.minecraft.world.entity.projectile.hurtingprojectile;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class Fireball extends AbstractHurtingProjectile implements ItemSupplier {
    private static final float MIN_CAMERA_DISTANCE_SQUARED = 12.25F;
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(Fireball.class, EntityDataSerializers.ITEM_STACK);

    public Fireball(EntityType<? extends Fireball> p_480495_, Level p_479095_) {
        super(p_480495_, p_479095_);
    }

    public Fireball(EntityType<? extends Fireball> p_480722_, double p_479687_, double p_479497_, double p_478924_, Vec3 p_481721_, Level p_480636_) {
        super(p_480722_, p_479687_, p_479497_, p_478924_, p_481721_, p_480636_);
    }

    public Fireball(EntityType<? extends Fireball> p_478135_, LivingEntity p_477928_, Vec3 p_480172_, Level p_478309_) {
        super(p_478135_, p_477928_, p_480172_, p_478309_);
    }

    public void setItem(ItemStack stack) {
        if (stack.isEmpty()) {
            this.getEntityData().set(DATA_ITEM_STACK, this.getDefaultItem());
        } else {
            this.getEntityData().set(DATA_ITEM_STACK, stack.copyWithCount(1));
        }
    }

    @Override
    protected void playEntityOnFireExtinguishedSound() {
    }

    @Override
    public ItemStack getItem() {
        return this.getEntityData().get(DATA_ITEM_STACK);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_479152_) {
        p_479152_.define(DATA_ITEM_STACK, this.getDefaultItem());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_479408_) {
        super.addAdditionalSaveData(p_479408_);
        p_479408_.store("Item", ItemStack.CODEC, this.getItem());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_479509_) {
        super.readAdditionalSaveData(p_479509_);
        this.setItem(p_479509_.read("Item", ItemStack.CODEC).orElse(this.getDefaultItem()));
    }

    private ItemStack getDefaultItem() {
        return new ItemStack(Items.FIRE_CHARGE);
    }

    @Override
    public @Nullable SlotAccess getSlot(int p_479474_) {
        return p_479474_ == 0 ? SlotAccess.of(this::getItem, this::setItem) : super.getSlot(p_479474_);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double p_477936_) {
        return this.tickCount < 2 && p_477936_ < 12.25 ? false : super.shouldRenderAtSqrDistance(p_477936_);
    }
}
