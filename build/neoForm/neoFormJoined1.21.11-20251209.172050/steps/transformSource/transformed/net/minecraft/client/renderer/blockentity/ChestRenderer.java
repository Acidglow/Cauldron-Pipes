package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.Direction;
import net.minecraft.util.SpecialDates;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CopperChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.LidBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ChestRenderer<T extends BlockEntity & LidBlockEntity> implements BlockEntityRenderer<T, ChestRenderState> {
    private final MaterialSet materials;
    private final ChestModel singleModel;
    private final ChestModel doubleLeftModel;
    private final ChestModel doubleRightModel;
    private final boolean xmasTextures;

    public ChestRenderer(BlockEntityRendererProvider.Context context) {
        this.materials = context.materials();
        this.xmasTextures = xmasTextures();
        this.singleModel = new ChestModel(context.bakeLayer(ModelLayers.CHEST));
        this.doubleLeftModel = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_LEFT));
        this.doubleRightModel = new ChestModel(context.bakeLayer(ModelLayers.DOUBLE_CHEST_RIGHT));
    }

    public static boolean xmasTextures() {
        return SpecialDates.isExtendedChristmas();
    }

    public ChestRenderState createRenderState() {
        return new ChestRenderState();
    }

    public void extractRenderState(
        T p_446382_, ChestRenderState p_445744_, float p_446088_, Vec3 p_447190_, ModelFeatureRenderer.@Nullable CrumblingOverlay p_446187_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_446382_, p_445744_, p_446088_, p_447190_, p_446187_);
        boolean flag = p_446382_.getLevel() != null;
        BlockState blockstate = flag ? p_446382_.getBlockState() : Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH);
        p_445744_.type = blockstate.hasProperty(ChestBlock.TYPE) ? blockstate.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        p_445744_.angle = blockstate.getValue(ChestBlock.FACING).toYRot();
        p_445744_.material = this.getChestMaterial(p_446382_, this.xmasTextures);
        DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> neighborcombineresult;
        if (flag && blockstate.getBlock() instanceof ChestBlock chestblock) {
            neighborcombineresult = chestblock.combine(blockstate, p_446382_.getLevel(), p_446382_.getBlockPos(), true);
        } else {
            neighborcombineresult = DoubleBlockCombiner.Combiner::acceptNone;
        }

        p_445744_.open = neighborcombineresult.apply(ChestBlock.opennessCombiner(p_446382_)).get(p_446088_);
        if (p_445744_.type != ChestType.SINGLE) {
            p_445744_.lightCoords = neighborcombineresult.apply(new BrightnessCombiner<>()).applyAsInt(p_445744_.lightCoords);
        }

        p_445744_.customMaterial = getCustomMaterial(p_446382_, p_445744_);
    }

    public void submit(ChestRenderState p_446740_, PoseStack p_445661_, SubmitNodeCollector p_445570_, CameraRenderState p_451060_) {
        p_445661_.pushPose();
        p_445661_.translate(0.5F, 0.5F, 0.5F);
        p_445661_.mulPose(Axis.YP.rotationDegrees(-p_446740_.angle));
        p_445661_.translate(-0.5F, -0.5F, -0.5F);
        float f = p_446740_.open;
        f = 1.0F - f;
        f = 1.0F - f * f * f;
        Material material = p_446740_.customMaterial != null ? p_446740_.customMaterial : Sheets.chooseMaterial(p_446740_.material, p_446740_.type);
        RenderType rendertype = material.renderType(RenderTypes::entityCutout);
        TextureAtlasSprite textureatlassprite = this.materials.get(material);
        if (p_446740_.type != ChestType.SINGLE) {
            if (p_446740_.type == ChestType.LEFT) {
                p_445570_.submitModel(
                    this.doubleLeftModel,
                    f,
                    p_445661_,
                    rendertype,
                    p_446740_.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    -1,
                    textureatlassprite,
                    0,
                    p_446740_.breakProgress
                );
            } else {
                p_445570_.submitModel(
                    this.doubleRightModel,
                    f,
                    p_445661_,
                    rendertype,
                    p_446740_.lightCoords,
                    OverlayTexture.NO_OVERLAY,
                    -1,
                    textureatlassprite,
                    0,
                    p_446740_.breakProgress
                );
            }
        } else {
            p_445570_.submitModel(
                this.singleModel,
                f,
                p_445661_,
                rendertype,
                p_446740_.lightCoords,
                OverlayTexture.NO_OVERLAY,
                -1,
                textureatlassprite,
                0,
                p_446740_.breakProgress
            );
        }

        p_445661_.popPose();
    }

    private ChestRenderState.ChestMaterialType getChestMaterial(BlockEntity blockEntity, boolean xmasTextures) {
        if (blockEntity instanceof EnderChestBlockEntity) {
            return ChestRenderState.ChestMaterialType.ENDER_CHEST;
        } else if (xmasTextures) {
            return ChestRenderState.ChestMaterialType.CHRISTMAS;
        } else if (blockEntity instanceof TrappedChestBlockEntity) {
            return ChestRenderState.ChestMaterialType.TRAPPED;
        } else if (blockEntity.getBlockState().getBlock() instanceof CopperChestBlock copperchestblock) {
            return switch (copperchestblock.getState()) {
                case UNAFFECTED -> ChestRenderState.ChestMaterialType.COPPER_UNAFFECTED;
                case EXPOSED -> ChestRenderState.ChestMaterialType.COPPER_EXPOSED;
                case WEATHERED -> ChestRenderState.ChestMaterialType.COPPER_WEATHERED;
                case OXIDIZED -> ChestRenderState.ChestMaterialType.COPPER_OXIDIZED;
            };
        } else {
            return ChestRenderState.ChestMaterialType.REGULAR;
        }
    }

    /**
     * Neo: Return a custom {@link Material} to render the chest with or {@code null} to
     * fall back to the vanilla material selection.
     */
    @Nullable
    protected Material getCustomMaterial(T blockEntity, ChestRenderState renderState) {
        return null;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(T blockEntity) {
        net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
        return net.minecraft.world.phys.AABB.encapsulatingFullBlocks(pos.offset(-1, 0, -1), pos.offset(1, 1, 1));
    }
}
