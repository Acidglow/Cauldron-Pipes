package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record ApplyExhaustion(LevelBasedValue amount) implements EnchantmentEntityEffect {
    public static final MapCodec<ApplyExhaustion> CODEC = RecordCodecBuilder.mapCodec(
        p_459209_ -> p_459209_.group(LevelBasedValue.CODEC.fieldOf("amount").forGetter(ApplyExhaustion::amount)).apply(p_459209_, ApplyExhaustion::new)
    );

    @Override
    public void apply(ServerLevel p_458965_, int p_458920_, EnchantedItemInUse p_459091_, Entity p_459043_, Vec3 p_459113_) {
        if (p_459043_ instanceof Player player) {
            player.causeFoodExhaustion(this.amount.calculate(p_458920_));
        }
    }

    @Override
    public MapCodec<ApplyExhaustion> codec() {
        return CODEC;
    }
}
