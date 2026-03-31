package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SpecialModelWrapper<T> implements ItemModel {
    private final SpecialModelRenderer<T> specialRenderer;
    private final ModelRenderProperties properties;
    private final Supplier<Vector3fc[]> extents;

    public SpecialModelWrapper(SpecialModelRenderer<T> specialRenderer, ModelRenderProperties properties) {
        this.specialRenderer = specialRenderer;
        this.properties = properties;
        this.extents = Suppliers.memoize(() -> {
            Set<Vector3fc> set = new HashSet<>();
            specialRenderer.getExtents(set::add);
            return set.toArray(new Vector3fc[0]);
        });
    }

    @Override
    public void update(
        ItemStackRenderState p_388134_,
        ItemStack p_387781_,
        ItemModelResolver p_387931_,
        ItemDisplayContext p_388057_,
        @Nullable ClientLevel p_388213_,
        @Nullable ItemOwner p_435146_,
        int p_387759_
    ) {
        p_388134_.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = p_388134_.newLayer();
        if (p_387781_.hasFoil()) {
            ItemStackRenderState.FoilType itemstackrenderstate$foiltype = ItemStackRenderState.FoilType.STANDARD;
            itemstackrenderstate$layerrenderstate.setFoilType(itemstackrenderstate$foiltype);
            p_388134_.setAnimated();
            p_388134_.appendModelIdentityElement(itemstackrenderstate$foiltype);
        }

        T t = this.specialRenderer.extractArgument(p_387781_);
        itemstackrenderstate$layerrenderstate.setExtents(this.extents);
        itemstackrenderstate$layerrenderstate.setupSpecialModel(this.specialRenderer, t);
        if (t != null) {
            p_388134_.appendModelIdentityElement(t);
        }

        this.properties.applyToLayer(itemstackrenderstate$layerrenderstate, p_388057_);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(Identifier base, SpecialModelRenderer.Unbaked specialModel) implements ItemModel.Unbaked {
        public static final MapCodec<SpecialModelWrapper.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_465665_ -> p_465665_.group(
                    Identifier.CODEC.fieldOf("base").forGetter(SpecialModelWrapper.Unbaked::base),
                    SpecialModelRenderers.CODEC.fieldOf("model").forGetter(SpecialModelWrapper.Unbaked::specialModel)
                )
                .apply(p_465665_, SpecialModelWrapper.Unbaked::new)
        );

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_387171_) {
            p_387171_.markDependency(this.base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext p_388760_) {
            SpecialModelRenderer<?> specialmodelrenderer = this.specialModel.bake(p_388760_);
            if (specialmodelrenderer == null) {
                return p_388760_.missingItemModel();
            } else {
                ModelRenderProperties modelrenderproperties = this.getProperties(p_388760_);
                return new SpecialModelWrapper<>(specialmodelrenderer, modelrenderproperties);
            }
        }

        private ModelRenderProperties getProperties(ItemModel.BakingContext context) {
            ModelBaker modelbaker = context.blockModelBaker();
            ResolvedModel resolvedmodel = modelbaker.getModel(this.base);
            TextureSlots textureslots = resolvedmodel.getTopTextureSlots();
            return ModelRenderProperties.fromResolvedModel(modelbaker, resolvedmodel, textureslots);
        }

        @Override
        public MapCodec<SpecialModelWrapper.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
