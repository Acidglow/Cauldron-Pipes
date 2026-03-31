package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityWithBoundingBoxRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BoundingBoxRenderable;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BlockEntityWithBoundingBoxRenderer<T extends BlockEntity & BoundingBoxRenderable>
    implements BlockEntityRenderer<T, BlockEntityWithBoundingBoxRenderState> {
    public static final int STRUCTURE_VOIDS_COLOR = ARGB.colorFromFloat(0.2F, 0.75F, 0.75F, 1.0F);

    public BlockEntityWithBoundingBoxRenderState createRenderState() {
        return new BlockEntityWithBoundingBoxRenderState();
    }

    public void extractRenderState(
        T p_447304_,
        BlockEntityWithBoundingBoxRenderState p_446774_,
        float p_447134_,
        Vec3 p_446736_,
        ModelFeatureRenderer.@Nullable CrumblingOverlay p_446663_
    ) {
        BlockEntityRenderer.super.extractRenderState(p_447304_, p_446774_, p_447134_, p_446736_, p_446663_);
        extract(p_447304_, p_446774_);
    }

    public static <T extends BlockEntity & BoundingBoxRenderable> void extract(T blockEntity, BlockEntityWithBoundingBoxRenderState renderState) {
        LocalPlayer localplayer = Minecraft.getInstance().player;
        renderState.isVisible = localplayer.canUseGameMasterBlocks() || localplayer.isSpectator();
        renderState.box = blockEntity.getRenderableBox();
        renderState.mode = blockEntity.renderMode();
        BlockPos blockpos = renderState.box.localPos();
        Vec3i vec3i = renderState.box.size();
        BlockPos blockpos1 = renderState.blockPos;
        BlockPos blockpos2 = blockpos1.offset(blockpos);
        if (renderState.isVisible && blockEntity.getLevel() != null && renderState.mode == BoundingBoxRenderable.Mode.BOX_AND_INVISIBLE_BLOCKS) {
            renderState.invisibleBlocks = new BlockEntityWithBoundingBoxRenderState.InvisibleBlockType[vec3i.getX() * vec3i.getY() * vec3i.getZ()];

            for (int i = 0; i < vec3i.getX(); i++) {
                for (int j = 0; j < vec3i.getY(); j++) {
                    for (int k = 0; k < vec3i.getZ(); k++) {
                        int l = k * vec3i.getX() * vec3i.getY() + j * vec3i.getX() + i;
                        BlockState blockstate = blockEntity.getLevel().getBlockState(blockpos2.offset(i, j, k));
                        if (blockstate.isAir()) {
                            renderState.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR;
                        } else if (blockstate.is(Blocks.STRUCTURE_VOID)) {
                            renderState.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.STRUCTURE_VOID;
                        } else if (blockstate.is(Blocks.BARRIER)) {
                            renderState.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.BARRIER;
                        } else if (blockstate.is(Blocks.LIGHT)) {
                            renderState.invisibleBlocks[l] = BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.LIGHT;
                        }
                    }
                }
            }
        } else {
            renderState.invisibleBlocks = null;
        }

        if (renderState.isVisible) {
        }

        renderState.structureVoids = null;
    }

    public void submit(BlockEntityWithBoundingBoxRenderState p_446796_, PoseStack p_445972_, SubmitNodeCollector p_445934_, CameraRenderState p_451074_) {
        if (p_446796_.isVisible) {
            BoundingBoxRenderable.Mode boundingboxrenderable$mode = p_446796_.mode;
            if (boundingboxrenderable$mode != BoundingBoxRenderable.Mode.NONE) {
                BoundingBoxRenderable.RenderableBox boundingboxrenderable$renderablebox = p_446796_.box;
                BlockPos blockpos = boundingboxrenderable$renderablebox.localPos();
                Vec3i vec3i = boundingboxrenderable$renderablebox.size();
                if (vec3i.getX() >= 1 && vec3i.getY() >= 1 && vec3i.getZ() >= 1) {
                    float f = 1.0F;
                    float f1 = 0.9F;
                    BlockPos blockpos1 = blockpos.offset(vec3i);
                    Gizmos.cuboid(
                        new AABB(blockpos.getX(), blockpos.getY(), blockpos.getZ(), blockpos1.getX(), blockpos1.getY(), blockpos1.getZ())
                            .move(p_446796_.blockPos),
                        GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.9F, 0.9F, 0.9F)),
                        true
                    );
                    this.renderInvisibleBlocks(p_446796_, blockpos, vec3i);
                }
            }
        }
    }

    private void renderInvisibleBlocks(BlockEntityWithBoundingBoxRenderState renderState, BlockPos pos, Vec3i size) {
        if (renderState.invisibleBlocks != null) {
            BlockPos blockpos = renderState.blockPos;
            BlockPos blockpos1 = blockpos.offset(pos);

            for (int i = 0; i < size.getX(); i++) {
                for (int j = 0; j < size.getY(); j++) {
                    for (int k = 0; k < size.getZ(); k++) {
                        int l = k * size.getX() * size.getY() + j * size.getX() + i;
                        BlockEntityWithBoundingBoxRenderState.InvisibleBlockType blockentitywithboundingboxrenderstate$invisibleblocktype = renderState.invisibleBlocks[l];
                        if (blockentitywithboundingboxrenderstate$invisibleblocktype != null) {
                            float f = blockentitywithboundingboxrenderstate$invisibleblocktype == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR
                                ? 0.05F
                                : 0.0F;
                            double d0 = blockpos1.getX() + i + 0.45F - f;
                            double d1 = blockpos1.getY() + j + 0.45F - f;
                            double d2 = blockpos1.getZ() + k + 0.45F - f;
                            double d3 = blockpos1.getX() + i + 0.55F + f;
                            double d4 = blockpos1.getY() + j + 0.55F + f;
                            double d5 = blockpos1.getZ() + k + 0.55F + f;
                            AABB aabb = new AABB(d0, d1, d2, d3, d4, d5);
                            if (blockentitywithboundingboxrenderstate$invisibleblocktype == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.AIR) {
                                Gizmos.cuboid(aabb, GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 0.5F, 0.5F, 1.0F)));
                            } else if (blockentitywithboundingboxrenderstate$invisibleblocktype
                                == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.STRUCTURE_VOID) {
                                Gizmos.cuboid(aabb, GizmoStyle.stroke(ARGB.colorFromFloat(1.0F, 1.0F, 0.75F, 0.75F)));
                            } else if (blockentitywithboundingboxrenderstate$invisibleblocktype
                                == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.BARRIER) {
                                Gizmos.cuboid(aabb, GizmoStyle.stroke(-65536));
                            } else if (blockentitywithboundingboxrenderstate$invisibleblocktype
                                == BlockEntityWithBoundingBoxRenderState.InvisibleBlockType.LIGHT) {
                                Gizmos.cuboid(aabb, GizmoStyle.stroke(-256));
                            }
                        }
                    }
                }
            }
        }
    }

    private void renderStructureVoids(BlockEntityWithBoundingBoxRenderState renderState, BlockPos pos, Vec3i size) {
        if (renderState.structureVoids != null) {
            DiscreteVoxelShape discretevoxelshape = new BitSetDiscreteVoxelShape(size.getX(), size.getY(), size.getZ());

            for (int i = 0; i < size.getX(); i++) {
                for (int j = 0; j < size.getY(); j++) {
                    for (int k = 0; k < size.getZ(); k++) {
                        int l = k * size.getX() * size.getY() + j * size.getX() + i;
                        if (renderState.structureVoids[l]) {
                            discretevoxelshape.fill(i, j, k);
                        }
                    }
                }
            }

            discretevoxelshape.forAllFaces((p_454264_, p_454265_, p_454266_, p_454267_) -> {
                float f = 0.48F;
                float f1 = p_454265_ + pos.getX() + 0.5F - 0.48F;
                float f2 = p_454266_ + pos.getY() + 0.5F - 0.48F;
                float f3 = p_454267_ + pos.getZ() + 0.5F - 0.48F;
                float f4 = p_454265_ + pos.getX() + 0.5F + 0.48F;
                float f5 = p_454266_ + pos.getY() + 0.5F + 0.48F;
                float f6 = p_454267_ + pos.getZ() + 0.5F + 0.48F;
                Gizmos.rect(new Vec3(f1, f2, f3), new Vec3(f4, f5, f6), p_454264_, GizmoStyle.fill(STRUCTURE_VOIDS_COLOR));
            });
        }
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(BlockEntity blockEntity) {
        return net.minecraft.world.phys.AABB.INFINITE;
    }
}
