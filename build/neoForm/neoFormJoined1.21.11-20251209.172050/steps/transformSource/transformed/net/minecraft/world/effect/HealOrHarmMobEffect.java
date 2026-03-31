package net.minecraft.world.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

class HealOrHarmMobEffect extends InstantenousMobEffect {
    private final boolean isHarm;

    public HealOrHarmMobEffect(MobEffectCategory category, int color, boolean isHarm) {
        super(category, color);
        this.isHarm = isHarm;
    }

    @Override
    public boolean applyEffectTick(ServerLevel p_376486_, LivingEntity p_295255_, int p_295147_) {
        if (this.isHarm == p_295255_.isInvertedHealAndHarm()) {
            p_295255_.heal(Math.max(4 << p_295147_, 0));
        } else {
            p_295255_.hurtServer(p_376486_, p_295255_.damageSources().magic(), 6 << p_295147_);
        }

        return true;
    }

    @Override
    public void applyInstantenousEffect(
        ServerLevel p_376760_, @Nullable Entity p_294574_, @Nullable Entity p_295692_, LivingEntity p_296483_, int p_296095_, double p_295178_
    ) {
        if (this.isHarm == p_296483_.isInvertedHealAndHarm()) {
            int i = (int)(p_295178_ * (4 << p_296095_) + 0.5);
            p_296483_.heal(i);
        } else {
            int j = (int)(p_295178_ * (6 << p_296095_) + 0.5);
            if (p_294574_ == null) {
                p_296483_.hurtServer(p_376760_, p_296483_.damageSources().magic(), j);
            } else {
                p_296483_.hurtServer(p_376760_, p_296483_.damageSources().indirectMagic(p_294574_, p_295692_), j);
            }
        }
    }
}
