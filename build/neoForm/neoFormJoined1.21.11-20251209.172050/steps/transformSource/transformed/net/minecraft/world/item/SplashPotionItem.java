package net.minecraft.world.item;

import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.level.Level;

public class SplashPotionItem extends ThrowablePotionItem {
    public SplashPotionItem(Item.Properties p_43241_) {
        super(p_43241_);
    }

    @Override
    public InteractionResult use(Level p_43243_, Player p_43244_, InteractionHand p_43245_) {
        p_43243_.playSound(
            null,
            p_43244_.getX(),
            p_43244_.getY(),
            p_43244_.getZ(),
            SoundEvents.SPLASH_POTION_THROW,
            SoundSource.PLAYERS,
            0.5F,
            0.4F / (p_43243_.getRandom().nextFloat() * 0.4F + 0.8F)
        );
        return super.use(p_43243_, p_43244_, p_43245_);
    }

    @Override
    protected AbstractThrownPotion createPotion(ServerLevel p_481019_, LivingEntity p_479852_, ItemStack p_399625_) {
        return new ThrownSplashPotion(p_481019_, p_479852_, p_399625_);
    }

    @Override
    protected AbstractThrownPotion createPotion(Level p_481940_, Position p_480276_, ItemStack p_399587_) {
        return new ThrownSplashPotion(p_481940_, p_480276_.x(), p_480276_.y(), p_480276_.z(), p_399587_);
    }
}
