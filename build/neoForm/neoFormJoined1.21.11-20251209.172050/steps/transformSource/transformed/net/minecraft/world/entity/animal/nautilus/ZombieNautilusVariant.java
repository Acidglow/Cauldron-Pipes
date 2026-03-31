package net.minecraft.world.entity.animal.nautilus;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.SpawnCondition;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;

public record ZombieNautilusVariant(ModelAndTexture<ZombieNautilusVariant.ModelType> modelAndTexture, SpawnPrioritySelectors spawnConditions)
    implements PriorityProvider<SpawnContext, SpawnCondition> {
    public static final Codec<ZombieNautilusVariant> DIRECT_CODEC = RecordCodecBuilder.create(
        p_478804_ -> p_478804_.group(
                ModelAndTexture.codec(ZombieNautilusVariant.ModelType.CODEC, ZombieNautilusVariant.ModelType.NORMAL)
                    .forGetter(ZombieNautilusVariant::modelAndTexture),
                SpawnPrioritySelectors.CODEC.fieldOf("spawn_conditions").forGetter(ZombieNautilusVariant::spawnConditions)
            )
            .apply(p_478804_, ZombieNautilusVariant::new)
    );
    public static final Codec<ZombieNautilusVariant> NETWORK_CODEC = RecordCodecBuilder.create(
        p_481425_ -> p_481425_.group(
                ModelAndTexture.codec(ZombieNautilusVariant.ModelType.CODEC, ZombieNautilusVariant.ModelType.NORMAL)
                    .forGetter(ZombieNautilusVariant::modelAndTexture)
            )
            .apply(p_481425_, ZombieNautilusVariant::new)
    );
    public static final Codec<Holder<ZombieNautilusVariant>> CODEC = RegistryFixedCodec.create(Registries.ZOMBIE_NAUTILUS_VARIANT);
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<ZombieNautilusVariant>> STREAM_CODEC = ByteBufCodecs.holderRegistry(
        Registries.ZOMBIE_NAUTILUS_VARIANT
    );

    private ZombieNautilusVariant(ModelAndTexture<ZombieNautilusVariant.ModelType> p_480167_) {
        this(p_480167_, SpawnPrioritySelectors.EMPTY);
    }

    @Override
    public List<PriorityProvider.Selector<SpawnContext, SpawnCondition>> selectors() {
        return this.spawnConditions.selectors();
    }

    public static enum ModelType implements StringRepresentable {
        NORMAL("normal"),
        WARM("warm");

        public static final Codec<ZombieNautilusVariant.ModelType> CODEC = StringRepresentable.fromEnum(ZombieNautilusVariant.ModelType::values);
        private final String name;

        private ModelType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}
