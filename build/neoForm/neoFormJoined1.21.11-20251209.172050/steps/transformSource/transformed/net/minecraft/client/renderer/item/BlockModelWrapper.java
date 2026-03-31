package net.minecraft.client.renderer.item;

import com.google.common.base.Suppliers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BlockModelWrapper implements ItemModel {
    private static final Function<ItemStack, RenderType> ITEM_RENDER_TYPE_GETTER = p_465662_ -> Sheets.translucentItemSheet();
    private static final Function<ItemStack, RenderType> BLOCK_RENDER_TYPE_GETTER = p_482423_ -> {
        if (p_482423_.getItem() instanceof BlockItem blockitem) {
            ChunkSectionLayer chunksectionlayer = ItemBlockRenderTypes.getChunkRenderType(blockitem.getBlock().defaultBlockState());
            if (chunksectionlayer != ChunkSectionLayer.TRANSLUCENT) {
                return Sheets.cutoutBlockSheet();
            }
        }

        return Sheets.translucentBlockItemSheet();
    };
    private final List<ItemTintSource> tints;
    private final List<BakedQuad> quads;
    private final Supplier<Vector3fc[]> extents;
    private final ModelRenderProperties properties;
    private final boolean animated;
    private final Function<ItemStack, RenderType> renderType;

    public BlockModelWrapper(List<ItemTintSource> tints, List<BakedQuad> quads, ModelRenderProperties properties, Function<ItemStack, RenderType> renderType) {
        this.tints = tints;
        this.quads = quads;
        this.properties = properties;
        this.renderType = renderType;
        this.extents = Suppliers.memoize(() -> computeExtents(this.quads));
        boolean flag = false;

        for (BakedQuad bakedquad : quads) {
            if (bakedquad.sprite().contents().isAnimated()) {
                flag = true;
                break;
            }
        }

        this.animated = flag;
    }

    public static Vector3fc[] computeExtents(List<BakedQuad> quads) {
        Set<Vector3fc> set = new HashSet<>();

        for (BakedQuad bakedquad : quads) {
            for (int i = 0; i < 4; i++) {
                set.add(bakedquad.position(i));
            }
        }

        return set.toArray(Vector3fc[]::new);
    }

    @Override
    public void update(
        ItemStackRenderState p_386488_,
        ItemStack p_386443_,
        ItemModelResolver p_388726_,
        ItemDisplayContext p_388231_,
        @Nullable ClientLevel p_387522_,
        @Nullable ItemOwner p_434975_,
        int p_388300_
    ) {
        p_386488_.appendModelIdentityElement(this);
        ItemStackRenderState.LayerRenderState itemstackrenderstate$layerrenderstate = p_386488_.newLayer();
        if (p_386443_.hasFoil()) {
            ItemStackRenderState.FoilType itemstackrenderstate$foiltype = hasSpecialAnimatedTexture(p_386443_)
                ? ItemStackRenderState.FoilType.SPECIAL
                : ItemStackRenderState.FoilType.STANDARD;
            itemstackrenderstate$layerrenderstate.setFoilType(itemstackrenderstate$foiltype);
            p_386488_.setAnimated();
            p_386488_.appendModelIdentityElement(itemstackrenderstate$foiltype);
        }

        int k = this.tints.size();
        int[] aint = itemstackrenderstate$layerrenderstate.prepareTintLayers(k);

        for (int i = 0; i < k; i++) {
            int j = this.tints.get(i).calculate(p_386443_, p_387522_, p_434975_ == null ? null : p_434975_.asLivingEntity());
            aint[i] = j;
            p_386488_.appendModelIdentityElement(j);
        }

        itemstackrenderstate$layerrenderstate.setExtents(this.extents);
        itemstackrenderstate$layerrenderstate.setRenderType(this.renderType.apply(p_386443_));
        this.properties.applyToLayer(itemstackrenderstate$layerrenderstate, p_388231_);
        itemstackrenderstate$layerrenderstate.prepareQuadList().addAll(this.quads);
        if (this.animated) {
            p_386488_.setAnimated();
        }
    }

    static Function<ItemStack, RenderType> detectRenderType(List<BakedQuad> quads) {
        Iterator<BakedQuad> iterator = quads.iterator();
        if (!iterator.hasNext()) {
            return ITEM_RENDER_TYPE_GETTER;
        } else {
            Identifier identifier = iterator.next().sprite().atlasLocation();

            while (iterator.hasNext()) {
                BakedQuad bakedquad = iterator.next();
                Identifier identifier1 = bakedquad.sprite().atlasLocation();
                if (!identifier1.equals(identifier)) {
                    throw new IllegalStateException("Multiple atlases used in model, expected " + identifier + ", but also got " + identifier1);
                }
            }

            if (identifier.equals(TextureAtlas.LOCATION_ITEMS)) {
                return ITEM_RENDER_TYPE_GETTER;
            } else if (identifier.equals(TextureAtlas.LOCATION_BLOCKS)) {
                return BLOCK_RENDER_TYPE_GETTER;
            } else {
                throw new IllegalArgumentException("Atlas " + identifier + " can't be usef for item models");
            }
        }
    }

    private static boolean hasSpecialAnimatedTexture(ItemStack stack) {
        return stack.is(ItemTags.COMPASSES) || stack.is(Items.CLOCK);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(Identifier model, List<ItemTintSource> tints) implements ItemModel.Unbaked {
        public static final MapCodec<BlockModelWrapper.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_465664_ -> p_465664_.group(
                    Identifier.CODEC.fieldOf("model").forGetter(BlockModelWrapper.Unbaked::model),
                    ItemTintSources.CODEC.listOf().optionalFieldOf("tints", List.of()).forGetter(BlockModelWrapper.Unbaked::tints)
                )
                .apply(p_465664_, BlockModelWrapper.Unbaked::new)
        );

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_387532_) {
            p_387532_.markDependency(this.model);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext p_388226_) {
            ModelBaker modelbaker = p_388226_.blockModelBaker();
            ResolvedModel resolvedmodel = modelbaker.getModel(this.model);
            TextureSlots textureslots = resolvedmodel.getTopTextureSlots();
            List<BakedQuad> list = resolvedmodel.bakeTopGeometry(textureslots, modelbaker, BlockModelRotation.IDENTITY).getAll();
            ModelRenderProperties modelrenderproperties = ModelRenderProperties.fromResolvedModel(modelbaker, resolvedmodel, textureslots);
            var renderTypeGroup = resolvedmodel.getTopAdditionalProperties().getOptional(
                    net.neoforged.neoforge.client.model.NeoForgeModelProperties.RENDER_TYPE);
            Function<ItemStack, RenderType> function = renderTypeGroup == null ? BlockModelWrapper.detectRenderType(list) : net.neoforged.neoforge.client.RenderTypeHelper.detectItemModelRenderType(list, renderTypeGroup);
            return new BlockModelWrapper(this.tints, list, modelrenderproperties, function);
        }

        @Override
        public MapCodec<BlockModelWrapper.Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
