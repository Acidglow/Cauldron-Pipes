package net.minecraft.client.renderer.debug;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SolidFaceRenderer implements DebugRenderer.SimpleDebugRenderer {
    private final Minecraft minecraft;

    public SolidFaceRenderer(Minecraft minecraft) {
        this.minecraft = minecraft;
    }

    @Override
    public void emitGizmos(double p_455109_, double p_455144_, double p_454709_, DebugValueAccess p_455392_, Frustum p_455997_, float p_456118_) {
        BlockGetter blockgetter = this.minecraft.player.level();
        BlockPos blockpos = BlockPos.containing(p_455109_, p_455144_, p_454709_);

        for (BlockPos blockpos1 : BlockPos.betweenClosed(blockpos.offset(-6, -6, -6), blockpos.offset(6, 6, 6))) {
            BlockState blockstate = blockgetter.getBlockState(blockpos1);
            if (!blockstate.is(Blocks.AIR)) {
                VoxelShape voxelshape = blockstate.getShape(blockgetter, blockpos1);

                for (AABB aabb : voxelshape.toAabbs()) {
                    AABB aabb1 = aabb.move(blockpos1).inflate(0.002);
                    int i = -2130771968;
                    Vec3 vec3 = aabb1.getMinPosition();
                    Vec3 vec31 = aabb1.getMaxPosition();
                    addFaceIfSturdy(blockpos1, blockstate, blockgetter, Direction.WEST, vec3, vec31, -2130771968);
                    addFaceIfSturdy(blockpos1, blockstate, blockgetter, Direction.SOUTH, vec3, vec31, -2130771968);
                    addFaceIfSturdy(blockpos1, blockstate, blockgetter, Direction.EAST, vec3, vec31, -2130771968);
                    addFaceIfSturdy(blockpos1, blockstate, blockgetter, Direction.NORTH, vec3, vec31, -2130771968);
                    addFaceIfSturdy(blockpos1, blockstate, blockgetter, Direction.DOWN, vec3, vec31, -2130771968);
                    addFaceIfSturdy(blockpos1, blockstate, blockgetter, Direction.UP, vec3, vec31, -2130771968);
                }
            }
        }
    }

    private static void addFaceIfSturdy(
        BlockPos pos, BlockState state, BlockGetter level, Direction direction, Vec3 minPos, Vec3 maxPos, int color
    ) {
        if (state.isFaceSturdy(level, pos, direction)) {
            Gizmos.rect(minPos, maxPos, direction, GizmoStyle.fill(color));
        }
    }
}
