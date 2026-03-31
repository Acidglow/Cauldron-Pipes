package net.minecraft.world.entity.vehicle.minecart;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class MinecartSpawner extends AbstractMinecart {
    private final BaseSpawner spawner = new BaseSpawner() {
        @Override
        public void broadcastEvent(Level p_478554_, BlockPos p_481522_, int p_478409_) {
            p_478554_.broadcastEntityEvent(MinecartSpawner.this, (byte)p_478409_);
        }

        @Override
        public com.mojang.datafixers.util.Either<net.minecraft.world.level.block.entity.BlockEntity, net.minecraft.world.entity.Entity> getOwner() {
            return com.mojang.datafixers.util.Either.right(MinecartSpawner.this);
        }
    };
    private final Runnable ticker;

    public MinecartSpawner(EntityType<? extends MinecartSpawner> p_478578_, Level p_480458_) {
        super(p_478578_, p_480458_);
        this.ticker = this.createTicker(p_480458_);
    }

    @Override
    protected Item getDropItem() {
        return Items.MINECART;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.MINECART);
    }

    private Runnable createTicker(Level level) {
        return level instanceof ServerLevel
            ? () -> this.spawner.serverTick((ServerLevel)level, this.blockPosition())
            : () -> this.spawner.clientTick(level, this.blockPosition());
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.SPAWNER.defaultBlockState();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_480107_) {
        super.readAdditionalSaveData(p_480107_);
        this.spawner.load(this.level(), this.blockPosition(), p_480107_);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_481439_) {
        super.addAdditionalSaveData(p_481439_);
        this.spawner.save(p_481439_);
    }

    @Override
    public void handleEntityEvent(byte p_479945_) {
        this.spawner.onEventTriggered(this.level(), p_479945_);
    }

    @Override
    public void tick() {
        super.tick();
        this.ticker.run();
    }

    public BaseSpawner getSpawner() {
        return this.spawner;
    }
}
