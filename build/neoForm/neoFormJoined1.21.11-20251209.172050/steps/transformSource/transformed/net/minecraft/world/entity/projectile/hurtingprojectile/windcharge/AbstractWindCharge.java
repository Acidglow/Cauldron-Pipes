package net.minecraft.world.entity.projectile.hurtingprojectile.windcharge;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.hurtingprojectile.AbstractHurtingProjectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class AbstractWindCharge extends AbstractHurtingProjectile implements ItemSupplier {
    public static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new SimpleExplosionDamageCalculator(
        true, false, Optional.empty(), BuiltInRegistries.BLOCK.get(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity())
    );
    public static final double JUMP_SCALE = 0.25;

    public AbstractWindCharge(EntityType<? extends AbstractWindCharge> p_481652_, Level p_481698_) {
        super(p_481652_, p_481698_);
        this.accelerationPower = 0.0;
    }

    public AbstractWindCharge(
        EntityType<? extends AbstractWindCharge> entityType, Level level, Entity owner, double x, double y, double z
    ) {
        super(entityType, x, y, z, level);
        this.setOwner(owner);
        this.accelerationPower = 0.0;
    }

    AbstractWindCharge(
        EntityType<? extends AbstractWindCharge> p_478928_, double p_481918_, double p_478477_, double p_479186_, Vec3 p_481238_, Level p_482115_
    ) {
        super(p_478928_, p_481918_, p_478477_, p_479186_, p_481238_, p_482115_);
        this.accelerationPower = 0.0;
    }

    @Override
    protected AABB makeBoundingBox(Vec3 p_482094_) {
        float f = this.getType().getDimensions().width() / 2.0F;
        float f1 = this.getType().getDimensions().height();
        float f2 = 0.15F;
        return new AABB(p_482094_.x - f, p_482094_.y - 0.15F, p_482094_.z - f, p_482094_.x + f, p_482094_.y - 0.15F + f1, p_482094_.z + f);
    }

    @Override
    public boolean canCollideWith(Entity p_481660_) {
        return p_481660_ instanceof AbstractWindCharge ? false : super.canCollideWith(p_481660_);
    }

    @Override
    protected boolean canHitEntity(Entity p_481024_) {
        if (p_481024_ instanceof AbstractWindCharge) {
            return false;
        } else {
            return p_481024_.getType() == EntityType.END_CRYSTAL ? false : super.canHitEntity(p_481024_);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult p_481371_) {
        super.onHitEntity(p_481371_);
        if (this.level() instanceof ServerLevel serverlevel) {
            LivingEntity livingentity2 = this.getOwner() instanceof LivingEntity livingentity ? livingentity : null;
            Entity entity = p_481371_.getEntity();
            if (livingentity2 != null) {
                livingentity2.setLastHurtMob(entity);
            }

            DamageSource damagesource = this.damageSources().windCharge(this, livingentity2);
            if (entity.hurtServer(serverlevel, damagesource, 1.0F) && entity instanceof LivingEntity livingentity1) {
                EnchantmentHelper.doPostAttackEffects(serverlevel, livingentity1, damagesource);
            }

            this.explode(this.position());
        }
    }

    @Override
    public void push(double p_481268_, double p_479174_, double p_482039_) {
    }

    protected abstract void explode(Vec3 pos);

    @Override
    protected void onHitBlock(BlockHitResult p_481136_) {
        super.onHitBlock(p_481136_);
        if (!this.level().isClientSide()) {
            Vec3i vec3i = p_481136_.getDirection().getUnitVec3i();
            Vec3 vec3 = Vec3.atLowerCornerOf(vec3i).multiply(0.25, 0.25, 0.25);
            Vec3 vec31 = p_481136_.getLocation().add(vec3);
            this.explode(vec31);
            this.discard();
        }
    }

    @Override
    protected void onHit(HitResult p_477947_) {
        super.onHit(p_477947_);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    public ItemStack getItem() {
        return ItemStack.EMPTY;
    }

    @Override
    protected float getInertia() {
        return 1.0F;
    }

    @Override
    protected float getLiquidInertia() {
        return this.getInertia();
    }

    @Override
    protected @Nullable ParticleOptions getTrailParticle() {
        return null;
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide() && this.getBlockY() > this.level().getMaxY() + 30) {
            this.explode(this.position());
            this.discard();
        } else {
            super.tick();
        }
    }
}
