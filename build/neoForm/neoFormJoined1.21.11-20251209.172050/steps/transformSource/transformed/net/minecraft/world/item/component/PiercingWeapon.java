package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public record PiercingWeapon(boolean dealsKnockback, boolean dismounts, Optional<Holder<SoundEvent>> sound, Optional<Holder<SoundEvent>> hitSound) {
    public static final Codec<PiercingWeapon> CODEC = RecordCodecBuilder.create(
        p_477902_ -> p_477902_.group(
                Codec.BOOL.optionalFieldOf("deals_knockback", true).forGetter(PiercingWeapon::dealsKnockback),
                Codec.BOOL.optionalFieldOf("dismounts", false).forGetter(PiercingWeapon::dismounts),
                SoundEvent.CODEC.optionalFieldOf("sound").forGetter(PiercingWeapon::sound),
                SoundEvent.CODEC.optionalFieldOf("hit_sound").forGetter(PiercingWeapon::hitSound)
            )
            .apply(p_477902_, PiercingWeapon::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, PiercingWeapon> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.BOOL,
        PiercingWeapon::dealsKnockback,
        ByteBufCodecs.BOOL,
        PiercingWeapon::dismounts,
        SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
        PiercingWeapon::sound,
        SoundEvent.STREAM_CODEC.apply(ByteBufCodecs::optional),
        PiercingWeapon::hitSound,
        PiercingWeapon::new
    );

    public void makeSound(Entity entity) {
        this.sound
            .ifPresent(
                p_454914_ -> entity.level()
                    .playSound(
                        entity, entity.getX(), entity.getY(), entity.getZ(), (Holder<SoundEvent>)p_454914_, entity.getSoundSource(), 1.0F, 1.0F
                    )
            );
    }

    public void makeHitSound(Entity entity) {
        this.hitSound
            .ifPresent(
                p_454940_ -> entity.level()
                    .playSound(
                        null, entity.getX(), entity.getY(), entity.getZ(), (Holder<SoundEvent>)p_454940_, entity.getSoundSource(), 1.0F, 1.0F
                    )
            );
    }

    public static boolean canHitEntity(Entity attacker, Entity entity) {
        if (entity.isInvulnerable() || !entity.isAlive()) {
            return false;
        } else if (entity instanceof Interaction) {
            return true;
        } else if (!entity.canBeHitByProjectile()) {
            return false;
        } else {
            return entity instanceof Player player && attacker instanceof Player player1 && !player1.canHarmPlayer(player)
                ? false
                : !attacker.isPassengerOfSameVehicle(entity);
        }
    }

    public void attack(LivingEntity attacker, EquipmentSlot slot) {
        float f = (float)attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
        AttackRange attackrange = attacker.entityAttackRange();
        boolean flag = false;

        for (EntityHitResult entityhitresult : ProjectileUtil.getHitEntitiesAlong(
                attacker, attackrange, p_455447_ -> canHitEntity(attacker, p_455447_), ClipContext.Block.COLLIDER
            )
            .<Collection<EntityHitResult>>map(p_477900_ -> List.of(), p_477901_ -> p_477901_)) {
            flag |= attacker.stabAttack(slot, entityhitresult.getEntity(), f, true, this.dealsKnockback, this.dismounts);
        }

        attacker.onAttack();
        attacker.lungeForwardMaybe();
        if (flag) {
            this.makeHitSound(attacker);
        }

        this.makeSound(attacker);
        attacker.swing(InteractionHand.MAIN_HAND, false);
    }
}
