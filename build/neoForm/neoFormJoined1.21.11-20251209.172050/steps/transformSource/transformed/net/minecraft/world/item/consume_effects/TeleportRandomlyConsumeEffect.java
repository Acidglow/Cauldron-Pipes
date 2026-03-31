package net.minecraft.world.item.consume_effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

public record TeleportRandomlyConsumeEffect(float diameter) implements ConsumeEffect {
    private static final float DEFAULT_DIAMETER = 16.0F;
    public static final MapCodec<TeleportRandomlyConsumeEffect> CODEC = RecordCodecBuilder.mapCodec(
        p_366612_ -> p_366612_.group(ExtraCodecs.POSITIVE_FLOAT.optionalFieldOf("diameter", 16.0F).forGetter(TeleportRandomlyConsumeEffect::diameter))
            .apply(p_366612_, TeleportRandomlyConsumeEffect::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, TeleportRandomlyConsumeEffect> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, TeleportRandomlyConsumeEffect::diameter, TeleportRandomlyConsumeEffect::new
    );

    public TeleportRandomlyConsumeEffect() {
        this(16.0F);
    }

    @Override
    public ConsumeEffect.Type<TeleportRandomlyConsumeEffect> getType() {
        return ConsumeEffect.Type.TELEPORT_RANDOMLY;
    }

    @Override
    public boolean apply(Level p_366648_, ItemStack p_366476_, LivingEntity p_366884_) {
        boolean flag = false;

        for (int i = 0; i < 16; i++) {
            double d0 = p_366884_.getX() + (p_366884_.getRandom().nextDouble() - 0.5) * this.diameter;
            double d1 = Mth.clamp(
                p_366884_.getY() + (p_366884_.getRandom().nextDouble() - 0.5) * this.diameter,
                (double)p_366648_.getMinY(),
                (double)(p_366648_.getMinY() + ((ServerLevel)p_366648_).getLogicalHeight() - 1)
            );
            double d2 = p_366884_.getZ() + (p_366884_.getRandom().nextDouble() - 0.5) * this.diameter;
            if (p_366884_.isPassenger()) {
                p_366884_.stopRiding();
            }

            Vec3 vec3 = p_366884_.position();
            var event = net.neoforged.neoforge.event.EventHooks.onItemConsumptionTeleport(p_366884_, p_366476_, d0, d1, d2);
            if (event.isCanceled()) return false;
            if (p_366884_.randomTeleport(event.getTargetX(), event.getTargetY(), event.getTargetZ(), true)) {
                p_366648_.gameEvent(GameEvent.TELEPORT, vec3, GameEvent.Context.of(p_366884_));
                SoundSource soundsource;
                SoundEvent soundevent;
                if (p_366884_ instanceof Fox) {
                    soundevent = SoundEvents.FOX_TELEPORT;
                    soundsource = SoundSource.NEUTRAL;
                } else {
                    soundevent = SoundEvents.CHORUS_FRUIT_TELEPORT;
                    soundsource = SoundSource.PLAYERS;
                }

                p_366648_.playSound(null, p_366884_.getX(), p_366884_.getY(), p_366884_.getZ(), soundevent, soundsource);
                p_366884_.resetFallDistance();
                flag = true;
                break;
            }
        }

        if (flag && p_366884_ instanceof Player player) {
            player.resetCurrentImpulseContext();
        }

        return flag;
    }
}
