package net.minecraft.world.phys.shapes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

public class EntityCollisionContext implements CollisionContext {
    private final boolean descending;
    private final double entityBottom;
    private final boolean placement;
    private final ItemStack heldItem;
    private final boolean alwaysCollideWithFluid;
    private final @Nullable Entity entity;

    protected EntityCollisionContext(boolean descending, boolean placement, double entityBottom, ItemStack heldItem, boolean alwaysCollideWithFluid, @Nullable Entity entity) {
        this.descending = descending;
        this.placement = placement;
        this.entityBottom = entityBottom;
        this.heldItem = heldItem;
        this.alwaysCollideWithFluid = alwaysCollideWithFluid;
        this.entity = entity;
    }

    @Deprecated
    protected EntityCollisionContext(Entity entity, boolean alwaysCollideWithFluid, boolean placement) {
        this(
            entity.isDescending(),
            placement,
            entity.getY(),
            entity instanceof LivingEntity livingentity ? livingentity.getMainHandItem() : ItemStack.EMPTY,
            alwaysCollideWithFluid,
            entity
        );
    }

    @Override
    public boolean isHoldingItem(Item item) {
        return this.heldItem.is(item);
    }

    @Override
    public boolean alwaysCollideWithFluid() {
        return this.alwaysCollideWithFluid;
    }

    @Override
    public boolean canStandOnFluid(FluidState p_205115_, FluidState p_205116_) {
        return !(this.entity instanceof LivingEntity livingentity)
            ? false
            : livingentity.canStandOnFluid(p_205116_) && !p_205115_.getType().isSame(p_205116_.getType());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState p_366423_, CollisionGetter p_366608_, BlockPos p_366445_) {
        return p_366423_.getCollisionShape(p_366608_, p_366445_, this);
    }

    @Override
    public boolean isDescending() {
        return this.descending;
    }

    @Override
    public boolean isAbove(VoxelShape p_82886_, BlockPos p_82887_, boolean p_82888_) {
        return this.entityBottom > p_82887_.getY() + p_82886_.max(Direction.Axis.Y) - 1.0E-5F;
    }

    public @Nullable Entity getEntity() {
        return this.entity;
    }

    @Override
    public boolean isPlacement() {
        return this.placement;
    }

    protected static class Empty extends EntityCollisionContext {
        protected static final CollisionContext WITHOUT_FLUID_COLLISIONS = new EntityCollisionContext.Empty(false);
        protected static final CollisionContext WITH_FLUID_COLLISIONS = new EntityCollisionContext.Empty(true);

        public Empty(boolean alwaysCollideWithFluid) {
            super(false, false, -Double.MAX_VALUE, ItemStack.EMPTY, alwaysCollideWithFluid, null);
        }

        @Override
        public boolean isAbove(VoxelShape p_446315_, BlockPos p_446935_, boolean p_446648_) {
            return p_446648_;
        }
    }
}
