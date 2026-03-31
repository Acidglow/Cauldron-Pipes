package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LiquidBlockRenderer {
    private static final float MAX_FLUID_HEIGHT = 0.8888889F;
    private final TextureAtlasSprite lavaStill;
    private final TextureAtlasSprite lavaFlowing;
    private final TextureAtlasSprite waterStill;
    private final TextureAtlasSprite waterFlowing;
    private final TextureAtlasSprite waterOverlay;

    public LiquidBlockRenderer(MaterialSet materials) {
        this.lavaStill = materials.get(ModelBakery.LAVA_STILL);
        this.lavaFlowing = materials.get(ModelBakery.LAVA_FLOW);
        this.waterStill = materials.get(ModelBakery.WATER_STILL);
        this.waterFlowing = materials.get(ModelBakery.WATER_FLOW);
        this.waterOverlay = materials.get(ModelBakery.WATER_OVERLAY);
        net.neoforged.neoforge.client.textures.FluidSpriteCache.reload();
    }

    private static boolean isNeighborSameFluid(FluidState firstState, FluidState secondState) {
        return secondState.getType().isSame(firstState.getType());
    }

    private static boolean isNeighborStateHidingOverlay(FluidState selfState, BlockState otherState, Direction neighborFace) {
        return otherState.shouldHideAdjacentFluidFace(neighborFace, selfState);
    }

    private static boolean isFaceOccludedByState(Direction face, float height, BlockState state) {
        VoxelShape voxelshape = state.getFaceOcclusionShape(face.getOpposite());
        if (voxelshape == Shapes.empty()) {
            return false;
        } else if (voxelshape == Shapes.block()) {
            boolean flag = height == 1.0F;
            return face != Direction.UP || flag;
        } else {
            VoxelShape voxelshape1 = Shapes.box(0.0, 0.0, 0.0, 1.0, height, 1.0);
            return Shapes.blockOccludes(voxelshape1, voxelshape, face);
        }
    }

    private static boolean isFaceOccludedByNeighbor(Direction face, float height, BlockState state) {
        return isFaceOccludedByState(face, height, state);
    }

    private static boolean isFaceOccludedBySelf(BlockState state, Direction face) {
        return isFaceOccludedByState(face.getOpposite(), 1.0F, state);
    }

    /**
 * @deprecated Neo: use overload that accepts BlockState
 */
    @Deprecated
    public static boolean shouldRenderFace(FluidState fluidState, BlockState blockState, Direction side, FluidState neighborFluid) {
        return !isFaceOccludedBySelf(blockState, side) && !isNeighborSameFluid(fluidState, neighborFluid);
    }

    public static boolean shouldRenderFace(FluidState fluidState, BlockState selfState, Direction direction, BlockState otherState) {
        return !isFaceOccludedBySelf(selfState, direction) && !isNeighborStateHidingOverlay(fluidState, otherState, direction.getOpposite());
    }

    public void tesselate(BlockAndTintGetter level, BlockPos pos, VertexConsumer buffer, BlockState blockState, FluidState fluidState) {
        TextureAtlasSprite[] atextureatlassprite = net.neoforged.neoforge.client.textures.FluidSpriteCache.getFluidSprites(level, pos, fluidState);
        TextureAtlasSprite textureatlassprite = atextureatlassprite[0]; // Neo: Custom still sprite
        TextureAtlasSprite textureatlassprite1 = atextureatlassprite[1]; // Neo: Custom flowing sprite
        int i = net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(fluidState).getTintColor(fluidState, level, pos);
        float alpha = (i >> 24 & 255) / 255.0F;
        float f = (i >> 16 & 0xFF) / 255.0F;
        float f1 = (i >> 8 & 0xFF) / 255.0F;
        float f2 = (i & 0xFF) / 255.0F;
        BlockState blockstate = level.getBlockState(pos.relative(Direction.DOWN));
        FluidState fluidstate = blockstate.getFluidState();
        BlockState blockstate1 = level.getBlockState(pos.relative(Direction.UP));
        FluidState fluidstate1 = blockstate1.getFluidState();
        BlockState blockstate2 = level.getBlockState(pos.relative(Direction.NORTH));
        FluidState fluidstate2 = blockstate2.getFluidState();
        BlockState blockstate3 = level.getBlockState(pos.relative(Direction.SOUTH));
        FluidState fluidstate3 = blockstate3.getFluidState();
        BlockState blockstate4 = level.getBlockState(pos.relative(Direction.WEST));
        FluidState fluidstate4 = blockstate4.getFluidState();
        BlockState blockstate5 = level.getBlockState(pos.relative(Direction.EAST));
        FluidState fluidstate5 = blockstate5.getFluidState();
        boolean flag1 = !isNeighborStateHidingOverlay(fluidState, blockstate1, Direction.DOWN);
        boolean flag2 = shouldRenderFace(fluidState, blockState, Direction.DOWN, blockstate) && !isFaceOccludedByNeighbor(Direction.DOWN, 0.8888889F, blockstate);
        boolean flag3 = shouldRenderFace(fluidState, blockState, Direction.NORTH, blockstate2);
        boolean flag4 = shouldRenderFace(fluidState, blockState, Direction.SOUTH, blockstate3);
        boolean flag5 = shouldRenderFace(fluidState, blockState, Direction.WEST, blockstate4);
        boolean flag6 = shouldRenderFace(fluidState, blockState, Direction.EAST, blockstate5);
        if (flag1 || flag2 || flag6 || flag5 || flag3 || flag4) {
            float f3 = level.getShade(Direction.DOWN, true);
            float f4 = level.getShade(Direction.UP, true);
            float f5 = level.getShade(Direction.NORTH, true);
            float f6 = level.getShade(Direction.WEST, true);
            Fluid fluid = fluidState.getType();
            float f11 = this.getHeight(level, fluid, pos, blockState, fluidState);
            float f7;
            float f8;
            float f9;
            float f10;
            if (f11 >= 1.0F) {
                f7 = 1.0F;
                f8 = 1.0F;
                f9 = 1.0F;
                f10 = 1.0F;
            } else {
                float f12 = this.getHeight(level, fluid, pos.north(), blockstate2, fluidstate2);
                float f13 = this.getHeight(level, fluid, pos.south(), blockstate3, fluidstate3);
                float f14 = this.getHeight(level, fluid, pos.east(), blockstate5, fluidstate5);
                float f15 = this.getHeight(level, fluid, pos.west(), blockstate4, fluidstate4);
                f7 = this.calculateAverageHeight(level, fluid, f11, f12, f14, pos.relative(Direction.NORTH).relative(Direction.EAST));
                f8 = this.calculateAverageHeight(level, fluid, f11, f12, f15, pos.relative(Direction.NORTH).relative(Direction.WEST));
                f9 = this.calculateAverageHeight(level, fluid, f11, f13, f14, pos.relative(Direction.SOUTH).relative(Direction.EAST));
                f10 = this.calculateAverageHeight(level, fluid, f11, f13, f15, pos.relative(Direction.SOUTH).relative(Direction.WEST));
            }

            float f37 = pos.getX() & 15;
            float f38 = pos.getY() & 15;
            float f39 = pos.getZ() & 15;
            float f40 = 0.001F;
            float f16 = flag2 ? 0.001F : 0.0F;
            if (flag1 && !isFaceOccludedByNeighbor(Direction.UP, Math.min(Math.min(f8, f10), Math.min(f9, f7)), blockstate1)) {
                f8 -= 0.001F;
                f10 -= 0.001F;
                f9 -= 0.001F;
                f7 -= 0.001F;
                Vec3 vec3 = fluidState.getFlow(level, pos);
                float f17;
                float f18;
                float f19;
                float f20;
                float f21;
                float f22;
                float f23;
                float f24;
                if (vec3.x == 0.0 && vec3.z == 0.0) {
                    f17 = textureatlassprite.getU(0.0F);
                    f21 = textureatlassprite.getV(0.0F);
                    f18 = f17;
                    f22 = textureatlassprite.getV(1.0F);
                    f19 = textureatlassprite.getU(1.0F);
                    f23 = f22;
                    f20 = f19;
                    f24 = f21;
                } else {
                    float f25 = (float)Mth.atan2(vec3.z, vec3.x) - (float) (Math.PI / 2);
                    float f26 = Mth.sin(f25) * 0.25F;
                    float f27 = Mth.cos(f25) * 0.25F;
                    float f28 = 0.5F;
                    f17 = textureatlassprite1.getU(0.5F + (-f27 - f26));
                    f21 = textureatlassprite1.getV(0.5F + (-f27 + f26));
                    f18 = textureatlassprite1.getU(0.5F + (-f27 + f26));
                    f22 = textureatlassprite1.getV(0.5F + (f27 + f26));
                    f19 = textureatlassprite1.getU(0.5F + (f27 + f26));
                    f23 = textureatlassprite1.getV(0.5F + (f27 - f26));
                    f20 = textureatlassprite1.getU(0.5F + (f27 - f26));
                    f24 = textureatlassprite1.getV(0.5F + (-f27 - f26));
                }

                int l = this.getLightColor(level, pos);
                float f54 = f4 * f;
                float f55 = f4 * f1;
                float f56 = f4 * f2;
                this.vertex(buffer, f37 + 0.0F, f38 + f8, f39 + 0.0F, f54, f55, f56, alpha, f17, f21, l);
                this.vertex(buffer, f37 + 0.0F, f38 + f10, f39 + 1.0F, f54, f55, f56, alpha, f18, f22, l);
                this.vertex(buffer, f37 + 1.0F, f38 + f9, f39 + 1.0F, f54, f55, f56, alpha, f19, f23, l);
                this.vertex(buffer, f37 + 1.0F, f38 + f7, f39 + 0.0F, f54, f55, f56, alpha, f20, f24, l);
                if (fluidState.shouldRenderBackwardUpFace(level, pos.above())) {
                    this.vertex(buffer, f37 + 0.0F, f38 + f8, f39 + 0.0F, f54, f55, f56, alpha, f17, f21, l);
                    this.vertex(buffer, f37 + 1.0F, f38 + f7, f39 + 0.0F, f54, f55, f56, alpha, f20, f24, l);
                    this.vertex(buffer, f37 + 1.0F, f38 + f9, f39 + 1.0F, f54, f55, f56, alpha, f19, f23, l);
                    this.vertex(buffer, f37 + 0.0F, f38 + f10, f39 + 1.0F, f54, f55, f56, alpha, f18, f22, l);
                }
            }

            if (flag2) {
                float f41 = textureatlassprite.getU0();
                float f42 = textureatlassprite.getU1();
                float f43 = textureatlassprite.getV0();
                float f44 = textureatlassprite.getV1();
                int k = this.getLightColor(level, pos.below());
                float f47 = f3 * f;
                float f49 = f3 * f1;
                float f51 = f3 * f2;
                this.vertex(buffer, f37, f38 + f16, f39 + 1.0F, f47, f49, f51, alpha, f41, f44, k);
                this.vertex(buffer, f37, f38 + f16, f39, f47, f49, f51, alpha, f41, f43, k);
                this.vertex(buffer, f37 + 1.0F, f38 + f16, f39, f47, f49, f51, alpha, f42, f43, k);
                this.vertex(buffer, f37 + 1.0F, f38 + f16, f39 + 1.0F, f47, f49, f51, alpha, f42, f44, k);
            }

            int j = this.getLightColor(level, pos);

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                float f45;
                float f46;
                float f48;
                float f50;
                float f52;
                float f53;
                boolean flag7;
                switch (direction) {
                    case NORTH:
                        f45 = f8;
                        f46 = f7;
                        f48 = f37;
                        f52 = f37 + 1.0F;
                        f50 = f39 + 0.001F;
                        f53 = f39 + 0.001F;
                        flag7 = flag3;
                        break;
                    case SOUTH:
                        f45 = f9;
                        f46 = f10;
                        f48 = f37 + 1.0F;
                        f52 = f37;
                        f50 = f39 + 1.0F - 0.001F;
                        f53 = f39 + 1.0F - 0.001F;
                        flag7 = flag4;
                        break;
                    case WEST:
                        f45 = f10;
                        f46 = f8;
                        f48 = f37 + 0.001F;
                        f52 = f37 + 0.001F;
                        f50 = f39 + 1.0F;
                        f53 = f39;
                        flag7 = flag5;
                        break;
                    default:
                        f45 = f7;
                        f46 = f9;
                        f48 = f37 + 1.0F - 0.001F;
                        f52 = f37 + 1.0F - 0.001F;
                        f50 = f39;
                        f53 = f39 + 1.0F;
                        flag7 = flag6;
                }

                if (flag7 && !isFaceOccludedByNeighbor(direction, Math.max(f45, f46), level.getBlockState(pos.relative(direction)))) {
                    BlockPos blockpos = pos.relative(direction);
                    TextureAtlasSprite textureatlassprite2 = textureatlassprite1;
                    if (atextureatlassprite[2] != null) {
                        if (level.getBlockState(blockpos).shouldDisplayFluidOverlay(level, blockpos, fluidState)) {
                            textureatlassprite2 = atextureatlassprite[2];
                        }
                    }

                    float f57 = textureatlassprite2.getU(0.0F);
                    float f29 = textureatlassprite2.getU(0.5F);
                    float f30 = textureatlassprite2.getV((1.0F - f45) * 0.5F);
                    float f31 = textureatlassprite2.getV((1.0F - f46) * 0.5F);
                    float f32 = textureatlassprite2.getV(0.5F);
                    float f33 = direction.getAxis() == Direction.Axis.Z ? f5 : f6;
                    float f34 = f4 * f33 * f;
                    float f35 = f4 * f33 * f1;
                    float f36 = f4 * f33 * f2;
                    this.vertex(buffer, f48, f38 + f45, f50, f34, f35, f36, alpha, f57, f30, j);
                    this.vertex(buffer, f52, f38 + f46, f53, f34, f35, f36, alpha, f29, f31, j);
                    this.vertex(buffer, f52, f38 + f16, f53, f34, f35, f36, alpha, f29, f32, j);
                    this.vertex(buffer, f48, f38 + f16, f50, f34, f35, f36, alpha, f57, f32, j);
                    if (textureatlassprite2 != atextureatlassprite[2]) { // Neo: use custom fluid's overlay texture
                        this.vertex(buffer, f48, f38 + f16, f50, f34, f35, f36, alpha, f57, f32, j);
                        this.vertex(buffer, f52, f38 + f16, f53, f34, f35, f36, alpha, f29, f32, j);
                        this.vertex(buffer, f52, f38 + f46, f53, f34, f35, f36, alpha, f29, f31, j);
                        this.vertex(buffer, f48, f38 + f45, f50, f34, f35, f36, alpha, f57, f30, j);
                    }
                }
            }
        }
    }

    private float calculateAverageHeight(BlockAndTintGetter level, Fluid fluid, float currentHeight, float height1, float height2, BlockPos pos) {
        if (!(height2 >= 1.0F) && !(height1 >= 1.0F)) {
            float[] afloat = new float[2];
            if (height2 > 0.0F || height1 > 0.0F) {
                float f = this.getHeight(level, fluid, pos);
                if (f >= 1.0F) {
                    return 1.0F;
                }

                this.addWeightedHeight(afloat, f);
            }

            this.addWeightedHeight(afloat, currentHeight);
            this.addWeightedHeight(afloat, height2);
            this.addWeightedHeight(afloat, height1);
            return afloat[0] / afloat[1];
        } else {
            return 1.0F;
        }
    }

    private void addWeightedHeight(float[] output, float height) {
        if (height >= 0.8F) {
            output[0] += height * 10.0F;
            output[1] += 10.0F;
        } else if (height >= 0.0F) {
            output[0] += height;
            output[1]++;
        }
    }

    private float getHeight(BlockAndTintGetter level, Fluid fluid, BlockPos pos) {
        BlockState blockstate = level.getBlockState(pos);
        return this.getHeight(level, fluid, pos, blockstate, blockstate.getFluidState());
    }

    private void vertex(
            VertexConsumer p_110985_,
            float p_110989_,
            float p_110990_,
            float p_110991_,
            float p_110992_,
            float p_110993_,
            float p_350595_,
            float alpha,
            float p_350459_,
            float p_350437_,
            int p_110994_
    ) {
        p_110985_.addVertex(p_110989_, p_110990_, p_110991_)
                .setColor(p_110992_, p_110993_, p_350595_, alpha)
                .setUv(p_350459_, p_350437_)
                .setLight(p_110994_)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    private float getHeight(BlockAndTintGetter level, Fluid fluid, BlockPos pos, BlockState blockState, FluidState fluidState) {
        if (fluid.isSame(fluidState.getType())) {
            BlockState blockstate = level.getBlockState(pos.above());
            return fluid.isSame(blockstate.getFluidState().getType()) ? 1.0F : fluidState.getOwnHeight();
        } else {
            return !blockState.isSolid() ? 0.0F : -1.0F;
        }
    }

    private void vertex(
        VertexConsumer buffer,
        float x,
        float y,
        float z,
        float red,
        float green,
        float blue,
        float u,
        float v,
        int packedLight
    ) {
        buffer.addVertex(x, y, z)
            .setColor(red, green, blue, 1.0F)
            .setUv(u, v)
            .setLight(packedLight)
            .setNormal(0.0F, 1.0F, 0.0F);
    }

    private int getLightColor(BlockAndTintGetter level, BlockPos pos) {
        int i = LevelRenderer.getLightColor(level, pos);
        int j = LevelRenderer.getLightColor(level, pos.above());
        int k = i & 0xFF;
        int l = j & 0xFF;
        int i1 = i >> 16 & 0xFF;
        int j1 = j >> 16 & 0xFF;
        return (k > l ? k : l) | (i1 > j1 ? i1 : j1) << 16;
    }
}
