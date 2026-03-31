package net.minecraft.world.item;

import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.InstrumentComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class InstrumentItem extends Item {
    public InstrumentItem(Item.Properties p_220099_) {
        super(p_220099_);
    }

    public static ItemStack create(Item item, Holder<Instrument> instrument) {
        ItemStack itemstack = new ItemStack(item);
        itemstack.set(DataComponents.INSTRUMENT, new InstrumentComponent(instrument));
        return itemstack;
    }

    @Override
    public InteractionResult use(Level p_220123_, Player p_220124_, InteractionHand p_220125_) {
        ItemStack itemstack = p_220124_.getItemInHand(p_220125_);
        Optional<? extends Holder<Instrument>> optional = this.getInstrument(itemstack, p_220124_.registryAccess());
        if (optional.isPresent()) {
            Instrument instrument = optional.get().value();
            p_220124_.startUsingItem(p_220125_);
            play(p_220123_, p_220124_, instrument);
            p_220124_.getCooldowns().addCooldown(itemstack, Mth.floor(instrument.useDuration() * 20.0F));
            p_220124_.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResult.CONSUME;
        } else {
            return InteractionResult.FAIL;
        }
    }

    @Override
    public int getUseDuration(ItemStack p_220131_, LivingEntity p_345916_) {
        Optional<Holder<Instrument>> optional = this.getInstrument(p_220131_, p_345916_.registryAccess());
        return optional.<Integer>map(p_360033_ -> Mth.floor(p_360033_.value().useDuration() * 20.0F)).orElse(0);
    }

    private Optional<Holder<Instrument>> getInstrument(ItemStack stack, HolderLookup.Provider registries) {
        InstrumentComponent instrumentcomponent = stack.get(DataComponents.INSTRUMENT);
        return instrumentcomponent != null ? instrumentcomponent.unwrap(registries) : Optional.empty();
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack p_220133_) {
        return ItemUseAnimation.TOOT_HORN;
    }

    private static void play(Level level, Player player, Instrument instrument) {
        SoundEvent soundevent = instrument.soundEvent().value();
        float f = instrument.range() / 16.0F;
        level.playSound(player, player, soundevent, SoundSource.RECORDS, f, 1.0F);
        level.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
    }
}
