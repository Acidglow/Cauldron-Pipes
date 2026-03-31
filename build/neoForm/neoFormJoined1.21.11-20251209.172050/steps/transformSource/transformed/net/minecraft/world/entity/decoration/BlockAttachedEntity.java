package net.minecraft.world.entity.decoration;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public abstract class BlockAttachedEntity extends Entity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private int checkInterval;
    protected BlockPos pos;

    protected BlockAttachedEntity(EntityType<? extends BlockAttachedEntity> p_345070_, Level p_345079_) {
        super(p_345070_, p_345079_);
    }

    protected BlockAttachedEntity(EntityType<? extends BlockAttachedEntity> entityType, Level level, BlockPos pos) {
        this(entityType, level);
        this.pos = pos;
    }

    protected abstract void recalculateBoundingBox();

    @Override
    public void tick() {
        if (this.level() instanceof ServerLevel serverlevel) {
            this.checkBelowWorld();
            if (this.checkInterval++ == 100) {
                this.checkInterval = 0;
                if (!this.isRemoved() && !this.survives()) {
                    this.discard();
                    this.dropItem(serverlevel, null);
                }
            }
        }
    }

    public abstract boolean survives();

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity p_346423_) {
        if (p_346423_ instanceof Player player) {
            return !this.level().mayInteract(player, this.pos) ? true : this.hurtOrSimulate(this.damageSources().playerAttack(player), 0.0F);
        } else {
            return false;
        }
    }

    @Override
    public boolean hurtClient(DamageSource p_376476_) {
        return !this.isInvulnerableToBase(p_376476_);
    }

    @Override
    public boolean hurtServer(ServerLevel p_376632_, DamageSource p_376099_, float p_376549_) {
        if (this.isInvulnerableToBase(p_376099_)) {
            return false;
        } else if (!p_376632_.getGameRules().get(GameRules.MOB_GRIEFING) && p_376099_.getEntity() instanceof Mob) {
            return false;
        } else {
            if (!this.isRemoved()) {
                this.kill(p_376632_);
                this.markHurt();
                this.dropItem(p_376632_, p_376099_.getEntity());
            }

            return true;
        }
    }

    @Override
    public boolean ignoreExplosion(Explosion p_360311_) {
        Entity entity = p_360311_.getDirectSourceEntity();
        if (entity != null && entity.isInWater()) {
            return true;
        } else {
            return p_360311_.shouldAffectBlocklikeEntities() ? super.ignoreExplosion(p_360311_) : true;
        }
    }

    @Override
    public void move(MoverType p_345778_, Vec3 p_345301_) {
        if (this.level() instanceof ServerLevel serverlevel && !this.isRemoved() && p_345301_.lengthSqr() > 0.0) {
            this.kill(serverlevel);
            this.dropItem(serverlevel, null);
        }
    }

    @Override
    public void push(double p_345288_, double p_346171_, double p_345389_) {
        if (this.level() instanceof ServerLevel serverlevel && !this.isRemoved() && p_345288_ * p_345288_ + p_346171_ * p_346171_ + p_345389_ * p_345389_ > 0.0
            )
         {
            this.kill(serverlevel);
            this.dropItem(serverlevel, null);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput p_422179_) {
        p_422179_.store("block_pos", BlockPos.CODEC, this.getPos());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput p_421739_) {
        BlockPos blockpos = p_421739_.read("block_pos", BlockPos.CODEC).orElse(null);
        if (blockpos != null && blockpos.closerThan(this.blockPosition(), 16.0)) {
            this.pos = blockpos;
        } else {
            LOGGER.error("Block-attached entity at invalid position: {}", blockpos);
        }
    }

    public abstract void dropItem(ServerLevel level, @Nullable Entity entity);

    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }

    @Override
    public void setPos(double p_346360_, double p_344743_, double p_345636_) {
        this.pos = BlockPos.containing(p_346360_, p_344743_, p_345636_);
        this.recalculateBoundingBox();
        this.needsSync = true;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    @Override
    public void thunderHit(ServerLevel p_345825_, LightningBolt p_346288_) {
    }

    @Override
    public void refreshDimensions() {
    }
}
