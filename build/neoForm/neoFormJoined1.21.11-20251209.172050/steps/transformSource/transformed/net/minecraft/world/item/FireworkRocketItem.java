package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class FireworkRocketItem extends Item implements ProjectileItem {
    public static final byte[] CRAFTABLE_DURATIONS = new byte[]{1, 2, 3};
    public static final double ROCKET_PLACEMENT_OFFSET = 0.15;

    public FireworkRocketItem(Item.Properties p_41209_) {
        super(p_41209_);
    }

    /**
     * Called when this item is used when targeting a Block
     */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player != null && player.isFallFlying()) {
            return InteractionResult.PASS;
        } else {
            if (level instanceof ServerLevel serverlevel) {
                ItemStack itemstack = context.getItemInHand();
                Vec3 vec3 = context.getClickLocation();
                Direction direction = context.getClickedFace();
                Projectile.spawnProjectile(
                    new FireworkRocketEntity(
                        level,
                        context.getPlayer(),
                        vec3.x + direction.getStepX() * 0.15,
                        vec3.y + direction.getStepY() * 0.15,
                        vec3.z + direction.getStepZ() * 0.15,
                        itemstack
                    ),
                    serverlevel,
                    itemstack
                );
                itemstack.shrink(1);
            }

            return InteractionResult.SUCCESS;
        }
    }

    @Override
    public InteractionResult use(Level p_41218_, Player p_41219_, InteractionHand p_41220_) {
        if (p_41219_.isFallFlying()) {
            ItemStack itemstack = p_41219_.getItemInHand(p_41220_);
            if (p_41218_ instanceof ServerLevel serverlevel) {
                if (p_41219_.dropAllLeashConnections(null)) {
                    p_41218_.playSound(null, p_41219_, SoundEvents.LEAD_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
                }

                Projectile.spawnProjectile(new FireworkRocketEntity(p_41218_, itemstack, p_41219_), serverlevel, itemstack);
                itemstack.consume(1, p_41219_);
                p_41219_.awardStat(Stats.ITEM_USED.get(this));
            }

            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    public Projectile asProjectile(Level p_338390_, Position p_338574_, ItemStack p_338487_, Direction p_338368_) {
        return new FireworkRocketEntity(p_338390_, p_338487_.copyWithCount(1), p_338574_.x(), p_338574_.y(), p_338574_.z(), true);
    }

    @Override
    public ProjectileItem.DispenseConfig createDispenseConfig() {
        return ProjectileItem.DispenseConfig.builder()
            .positionFunction(FireworkRocketItem::getEntityJustOutsideOfBlockPos)
            .uncertainty(1.0F)
            .power(0.5F)
            .overrideDispenseEvent(1004)
            .build();
    }

    private static Vec3 getEntityJustOutsideOfBlockPos(BlockSource blockSource, Direction direction) {
        return blockSource.center()
            .add(direction.getStepX() * 0.5000099999997474, direction.getStepY() * 0.5000099999997474, direction.getStepZ() * 0.5000099999997474);
    }
}
