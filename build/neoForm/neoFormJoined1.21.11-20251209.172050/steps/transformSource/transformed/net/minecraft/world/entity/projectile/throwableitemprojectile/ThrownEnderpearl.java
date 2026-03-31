package net.minecraft.world.entity.projectile.throwableitemprojectile;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class ThrownEnderpearl extends ThrowableItemProjectile {
    private long ticketTimer = 0L;

    public ThrownEnderpearl(EntityType<? extends ThrownEnderpearl> p_478915_, Level p_480017_) {
        super(p_478915_, p_480017_);
    }

    public ThrownEnderpearl(Level level, LivingEntity owner, ItemStack item) {
        super(EntityType.ENDER_PEARL, owner, level, item);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.ENDER_PEARL;
    }

    @Override
    protected void setOwner(@Nullable EntityReference<Entity> p_482142_) {
        this.deregisterFromCurrentOwner();
        super.setOwner(p_482142_);
        this.registerToCurrentOwner();
    }

    private void deregisterFromCurrentOwner() {
        if (this.getOwner() instanceof ServerPlayer serverplayer) {
            serverplayer.deregisterEnderPearl(this);
        }
    }

    private void registerToCurrentOwner() {
        if (this.getOwner() instanceof ServerPlayer serverplayer) {
            serverplayer.registerEnderPearl(this);
        }
    }

    @Override
    public @Nullable Entity getOwner() {
        return this.owner != null && this.level() instanceof ServerLevel serverlevel ? this.owner.getEntity(serverlevel, Entity.class) : super.getOwner();
    }

    private static @Nullable Entity findOwnerIncludingDeadPlayer(ServerLevel level, UUID uuid) {
        Entity entity = level.getEntityInAnyDimension(uuid);
        return (Entity)(entity != null ? entity : level.getServer().getPlayerList().getPlayer(uuid));
    }

    /**
     * Called when the arrow hits an entity
     */
    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), 0.0F);
    }

    /**
     * Called when this EntityFireball hits a block or entity.
     */
    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        for (int i = 0; i < 32; i++) {
            this.level()
                .addParticle(
                    ParticleTypes.PORTAL,
                    this.getX(),
                    this.getY() + this.random.nextDouble() * 2.0,
                    this.getZ(),
                    this.random.nextGaussian(),
                    0.0,
                    this.random.nextGaussian()
                );
        }

        if (this.level() instanceof ServerLevel serverlevel && !this.isRemoved()) {
            Entity entity = this.getOwner();
            if (entity != null && isAllowedToTeleportOwner(entity, serverlevel)) {
                Vec3 vec3 = this.oldPosition();
                if (entity instanceof ServerPlayer serverplayer) {
                    if (serverplayer.connection.isAcceptingMessages()) {
                        net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderPearl event = net.neoforged.neoforge.event.EventHooks.onEnderPearlLand(serverplayer, this.getX(), this.getY(), this.getZ(), this, 5.0F, result);
                        if (!event.isCanceled()) { // Don't indent to lower patch size
                        if (this.random.nextFloat() < 0.05F && serverlevel.isSpawningMonsters()) {
                            Endermite endermite = EntityType.ENDERMITE.create(serverlevel, EntitySpawnReason.TRIGGERED);
                            if (endermite != null) {
                                endermite.snapTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
                                serverlevel.addFreshEntity(endermite);
                            }
                        }

                        if (this.isOnPortalCooldown()) {
                            entity.setPortalCooldown();
                        }

                        ServerPlayer serverplayer1 = serverplayer.teleport(
                            new TeleportTransition(
                                serverlevel, event.getTarget(), entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(), TeleportTransition.DO_NOTHING
                            )
                        );
                        if (serverplayer1 != null) {
                            serverplayer1.resetFallDistance();
                            serverplayer1.resetCurrentImpulseContext();
                            serverplayer1.hurtServer(serverplayer.level(), this.damageSources().fall(), event.getAttackDamage());
                        }

                        this.playSound(serverlevel, vec3);
                        } //Forge: End
                    }
                } else {
                    Entity entity1 = entity.teleport(
                        new TeleportTransition(serverlevel, vec3, entity.getDeltaMovement(), entity.getYRot(), entity.getXRot(), TeleportTransition.DO_NOTHING)
                    );
                    if (entity1 != null) {
                        entity1.resetFallDistance();
                    }

                    this.playSound(serverlevel, vec3);
                }

                this.discard();
            } else {
                this.discard();
            }
        }
    }

    private static boolean isAllowedToTeleportOwner(Entity entity, Level level) {
        if (entity.level().dimension() == level.dimension()) {
            return !(entity instanceof LivingEntity livingentity) ? entity.isAlive() : livingentity.isAlive() && !livingentity.isSleeping();
        } else {
            return entity.canUsePortal(true);
        }
    }

    @Override
    public void tick() {
        if (this.level() instanceof ServerLevel serverlevel) {
            int j = SectionPos.blockToSectionCoord(this.position().x());
            int $$3 = SectionPos.blockToSectionCoord(this.position().z());
            Entity entity = this.owner != null ? findOwnerIncludingDeadPlayer(serverlevel, this.owner.getUUID()) : null;
            if (entity instanceof ServerPlayer serverplayer
                && !entity.isAlive()
                && !serverplayer.wonGame
                && serverplayer.level().getGameRules().get(GameRules.ENDER_PEARLS_VANISH_ON_DEATH)) {
                this.discard();
            } else {
                super.tick();
            }

            if (this.isAlive()) {
                BlockPos blockpos = BlockPos.containing(this.position());
                if ((--this.ticketTimer <= 0L || j != SectionPos.blockToSectionCoord(blockpos.getX()) || $$3 != SectionPos.blockToSectionCoord(blockpos.getZ()))
                    && entity instanceof ServerPlayer serverplayer1) {
                    this.ticketTimer = serverplayer1.registerAndUpdateEnderPearlTicket(this);
                }
            }
        } else {
            super.tick();
        }
    }

    private void playSound(Level level, Vec3 pos) {
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS);
    }

    @Override
    public @Nullable Entity teleport(TeleportTransition p_478267_) {
        Entity entity = super.teleport(p_478267_);
        if (entity != null) {
            entity.placePortalTicket(BlockPos.containing(entity.position()));
        }

        return entity;
    }

    @Override
    public boolean canTeleport(Level p_477924_, Level p_481306_) {
        return p_477924_.dimension() == Level.END && p_481306_.dimension() == Level.OVERWORLD && this.getOwner() instanceof ServerPlayer serverplayer
            ? super.canTeleport(p_477924_, p_481306_) && serverplayer.seenCredits
            : super.canTeleport(p_477924_, p_481306_);
    }

    @Override
    protected void onInsideBlock(BlockState p_479792_) {
        super.onInsideBlock(p_479792_);
        if (p_479792_.is(Blocks.END_GATEWAY) && this.getOwner() instanceof ServerPlayer serverplayer) {
            serverplayer.onInsideBlock(p_479792_);
        }
    }

    @Override
    public void onRemoval(Entity.RemovalReason p_480439_) {
        if (p_480439_ != Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            this.deregisterFromCurrentOwner();
        }

        super.onRemoval(p_480439_);
    }

    @Override
    public void onAboveBubbleColumn(boolean p_481576_, BlockPos p_482147_) {
        Entity.handleOnAboveBubbleColumn(this, p_481576_, p_482147_);
    }

    @Override
    public void onInsideBubbleColumn(boolean p_478992_) {
        Entity.handleOnInsideBubbleColumn(this, p_478992_);
    }
}
