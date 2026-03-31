package net.minecraft.advancements.criterion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.loot.LootContext;

public class ChanneledLightningTrigger extends SimpleCriterionTrigger<ChanneledLightningTrigger.TriggerInstance> {
    @Override
    public Codec<ChanneledLightningTrigger.TriggerInstance> codec() {
        return ChanneledLightningTrigger.TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player, Collection<? extends Entity> entityTriggered) {
        List<LootContext> list = entityTriggered.stream().map(p_468596_ -> EntityPredicate.createContext(player, p_468596_)).collect(Collectors.toList());
        this.trigger(player, p_468918_ -> p_468918_.matches(list));
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player, List<ContextAwarePredicate> victims) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<ChanneledLightningTrigger.TriggerInstance> CODEC = RecordCodecBuilder.create(
            p_468656_ -> p_468656_.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(ChanneledLightningTrigger.TriggerInstance::player),
                    EntityPredicate.ADVANCEMENT_CODEC
                        .listOf()
                        .optionalFieldOf("victims", List.of())
                        .forGetter(ChanneledLightningTrigger.TriggerInstance::victims)
                )
                .apply(p_468656_, ChanneledLightningTrigger.TriggerInstance::new)
        );

        public static Criterion<ChanneledLightningTrigger.TriggerInstance> channeledLightning(EntityPredicate.Builder... victims) {
            return CriteriaTriggers.CHANNELED_LIGHTNING
                .createCriterion(new ChanneledLightningTrigger.TriggerInstance(Optional.empty(), EntityPredicate.wrap(victims)));
        }

        public boolean matches(Collection<? extends LootContext> victims) {
            for (ContextAwarePredicate contextawarepredicate : this.victims) {
                boolean flag = false;

                for (LootContext lootcontext : victims) {
                    if (contextawarepredicate.matches(lootcontext)) {
                        flag = true;
                        break;
                    }
                }

                if (!flag) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public void validate(CriterionValidator p_467628_) {
            SimpleCriterionTrigger.SimpleInstance.super.validate(p_467628_);
            p_467628_.validateEntities(this.victims, "victims");
        }
    }
}
