package net.minecraft.world.entity.vehicle.boat;

import java.util.function.Supplier;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ContainerUser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import org.jspecify.annotations.Nullable;

public abstract class AbstractChestBoat extends AbstractBoat implements HasCustomInventoryScreen, ContainerEntity {
    private static final int CONTAINER_SIZE = 27;
    private NonNullList<ItemStack> itemStacks = NonNullList.withSize(27, ItemStack.EMPTY);
    private @Nullable ResourceKey<LootTable> lootTable;
    private long lootTableSeed;

    public AbstractChestBoat(EntityType<? extends AbstractChestBoat> p_481378_, Level p_480838_, Supplier<Item> p_481287_) {
        super(p_481378_, p_480838_, p_481287_);
    }

    @Override
    protected float getSinglePassengerXOffset() {
        return 0.15F;
    }

    @Override
    protected int getMaxPassengers() {
        return 1;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_479638_) {
        super.addAdditionalSaveData(p_479638_);
        this.addChestVehicleSaveData(p_479638_);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_479836_) {
        super.readAdditionalSaveData(p_479836_);
        this.readChestVehicleSaveData(p_479836_);
    }

    @Override
    public void destroy(ServerLevel p_480863_, DamageSource p_479168_) {
        this.destroy(p_480863_, this.getDropItem());
        this.chestVehicleDestroyed(p_479168_, p_480863_, this);
    }

    @Override
    public void remove(Entity.RemovalReason p_482069_) {
        if (!this.level().isClientSide() && p_482069_.shouldDestroy()) {
            Containers.dropContents(this.level(), this, this);
        }

        super.remove(p_482069_);
    }

    @Override
    public InteractionResult interact(Player p_479469_, InteractionHand p_478381_) {
        InteractionResult interactionresult = super.interact(p_479469_, p_478381_);
        if (interactionresult != InteractionResult.PASS) {
            return interactionresult;
        } else if (this.canAddPassenger(p_479469_) && !p_479469_.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        } else {
            InteractionResult interactionresult1 = this.interactWithContainerVehicle(p_479469_);
            if (interactionresult1.consumesAction() && p_479469_.level() instanceof ServerLevel serverlevel) {
                this.gameEvent(GameEvent.CONTAINER_OPEN, p_479469_);
                PiglinAi.angerNearbyPiglins(serverlevel, p_479469_, true);
            }

            return interactionresult1;
        }
    }

    @Override
    public void openCustomInventoryScreen(Player p_479971_) {
        p_479971_.openMenu(this);
        if (p_479971_.level() instanceof ServerLevel serverlevel) {
            this.gameEvent(GameEvent.CONTAINER_OPEN, p_479971_);
            PiglinAi.angerNearbyPiglins(serverlevel, p_479971_, true);
        }
    }

    @Override
    public void clearContent() {
        this.clearChestVehicleContent();
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    public ItemStack getItem(int p_479344_) {
        return this.getChestVehicleItem(p_479344_);
    }

    @Override
    public ItemStack removeItem(int p_478146_, int p_480416_) {
        return this.removeChestVehicleItem(p_478146_, p_480416_);
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_479843_) {
        return this.removeChestVehicleItemNoUpdate(p_479843_);
    }

    @Override
    public void setItem(int p_479955_, ItemStack p_480325_) {
        this.setChestVehicleItem(p_479955_, p_480325_);
    }

    @Override
    public SlotAccess getSlot(int p_479448_) {
        return this.getChestVehicleSlot(p_479448_);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player p_481292_) {
        return this.isChestVehicleStillValid(p_481292_);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int p_480389_, Inventory p_481494_, Player p_480896_) {
        if (this.lootTable != null && p_480896_.isSpectator()) {
            return null;
        } else {
            this.unpackLootTable(p_481494_.player);
            return ChestMenu.threeRows(p_480389_, p_481494_, this);
        }
    }

    public void unpackLootTable(@Nullable Player player) {
        this.unpackChestVehicleLootTable(player);
    }

    @Override
    public @Nullable ResourceKey<LootTable> getContainerLootTable() {
        return this.lootTable;
    }

    @Override
    public void setContainerLootTable(@Nullable ResourceKey<LootTable> p_481235_) {
        this.lootTable = p_481235_;
    }

    @Override
    public long getContainerLootTableSeed() {
        return this.lootTableSeed;
    }

    @Override
    public void setContainerLootTableSeed(long p_480185_) {
        this.lootTableSeed = p_480185_;
    }

    @Override
    public NonNullList<ItemStack> getItemStacks() {
        return this.itemStacks;
    }

    @Override
    public void clearItemStacks() {
        this.itemStacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
    }

    @Override
    public void stopOpen(ContainerUser p_478933_) {
        this.level().gameEvent(GameEvent.CONTAINER_CLOSE, this.position(), GameEvent.Context.of(p_478933_.getLivingEntity()));
    }
}
