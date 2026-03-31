package net.minecraft.world.entity.projectile.hurtingprojectile.windcharge;

import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SimpleExplosionDamageCalculator;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class WindCharge extends AbstractWindCharge {
    private static final ExplosionDamageCalculator EXPLOSION_DAMAGE_CALCULATOR = new SimpleExplosionDamageCalculator(
        true, false, Optional.of(1.22F), BuiltInRegistries.BLOCK.get(BlockTags.BLOCKS_WIND_CHARGE_EXPLOSIONS).map(Function.identity())
    );
    private static final float RADIUS = 1.2F;
    private static final float MIN_CAMERA_DISTANCE_SQUARED = Mth.square(3.5F);
    private int noDeflectTicks = 5;

    public WindCharge(EntityType<? extends AbstractWindCharge> p_482201_, Level p_479730_) {
        super(p_482201_, p_479730_);
    }

    public WindCharge(Player player, Level level, double x, double y, double z) {
        super(EntityType.WIND_CHARGE, level, player, x, y, z);
    }

    public WindCharge(Level level, double x, double y, double z, Vec3 movement) {
        super(EntityType.WIND_CHARGE, x, y, z, movement, level);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.noDeflectTicks > 0) {
            this.noDeflectTicks--;
        }
    }

    @Override
    public boolean deflect(ProjectileDeflection p_478545_, @Nullable Entity p_482092_, @Nullable EntityReference<Entity> p_480343_, boolean p_478437_) {
        return this.noDeflectTicks > 0 ? false : super.deflect(p_478545_, p_482092_, p_480343_, p_478437_);
    }

    @Override
    protected void explode(Vec3 p_479115_) {
        this.level()
            .explode(
                this,
                null,
                EXPLOSION_DAMAGE_CALCULATOR,
                p_479115_.x(),
                p_479115_.y(),
                p_479115_.z(),
                1.2F,
                false,
                Level.ExplosionInteraction.TRIGGER,
                ParticleTypes.GUST_EMITTER_SMALL,
                ParticleTypes.GUST_EMITTER_LARGE,
                WeightedList.of(),
                SoundEvents.WIND_CHARGE_BURST
            );
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double p_478101_) {
        return this.tickCount < 2 && p_478101_ < MIN_CAMERA_DISTANCE_SQUARED ? false : super.shouldRenderAtSqrDistance(p_478101_);
    }
}
