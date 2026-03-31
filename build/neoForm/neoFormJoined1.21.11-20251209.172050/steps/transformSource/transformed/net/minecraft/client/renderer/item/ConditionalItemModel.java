package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.client.multiplayer.CacheSlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.ItemModelPropertyTest;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ConditionalItemModel implements ItemModel {
    private final ItemModelPropertyTest property;
    private final ItemModel onTrue;
    private final ItemModel onFalse;

    public ConditionalItemModel(ItemModelPropertyTest property, ItemModel onTrue, ItemModel onFalse) {
        this.property = property;
        this.onTrue = onTrue;
        this.onFalse = onFalse;
    }

    @Override
    public void update(
        ItemStackRenderState p_387756_,
        ItemStack p_387286_,
        ItemModelResolver p_386644_,
        ItemDisplayContext p_387754_,
        @Nullable ClientLevel p_388301_,
        @Nullable ItemOwner p_435949_,
        int p_387025_
    ) {
        p_387756_.appendModelIdentityElement(this);
        (this.property.get(p_387286_, p_388301_, p_435949_ == null ? null : p_435949_.asLivingEntity(), p_387025_, p_387754_) ? this.onTrue : this.onFalse)
            .update(p_387756_, p_387286_, p_386644_, p_387754_, p_388301_, p_435949_, p_387025_);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(ConditionalItemModelProperty property, ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse) implements ItemModel.Unbaked {
        public static final MapCodec<ConditionalItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_388916_ -> p_388916_.group(
                    ConditionalItemModelProperties.MAP_CODEC.forGetter(ConditionalItemModel.Unbaked::property),
                    ItemModels.CODEC.fieldOf("on_true").forGetter(ConditionalItemModel.Unbaked::onTrue),
                    ItemModels.CODEC.fieldOf("on_false").forGetter(ConditionalItemModel.Unbaked::onFalse)
                )
                .apply(p_388916_, ConditionalItemModel.Unbaked::new)
        );

        @Override
        public MapCodec<ConditionalItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext p_388309_) {
            return new ConditionalItemModel(
                this.adaptProperty(this.property, p_388309_.contextSwapper()), this.onTrue.bake(p_388309_), this.onFalse.bake(p_388309_)
            );
        }

        private ItemModelPropertyTest adaptProperty(ConditionalItemModelProperty property, @Nullable RegistryContextSwapper contextSwapper) {
            if (contextSwapper == null) {
                return property;
            } else {
                CacheSlot<ClientLevel, ItemModelPropertyTest> cacheslot = new CacheSlot<>(p_399329_ -> swapContext(property, contextSwapper, p_399329_));
                return (p_399322_, p_399323_, p_399324_, p_399325_, p_399326_) -> {
                    ItemModelPropertyTest itemmodelpropertytest = (ItemModelPropertyTest)(p_399323_ == null ? property : cacheslot.compute(p_399323_));
                    return itemmodelpropertytest.get(p_399322_, p_399323_, p_399324_, p_399325_, p_399326_);
                };
            }
        }

        private static <T extends ConditionalItemModelProperty> T swapContext(T property, RegistryContextSwapper contextSwapper, ClientLevel level) {
            return (T)contextSwapper.swapTo((com.mojang.serialization.Codec<T>)property.type().codec(), property, level.registryAccess()).result().orElse(property);
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_388796_) {
            this.onTrue.resolveDependencies(p_388796_);
            this.onFalse.resolveDependencies(p_388796_);
        }
    }
}
