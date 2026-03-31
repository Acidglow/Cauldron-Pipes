package net.minecraft.client.renderer;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public enum FaceInfo {
    DOWN(
        new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z)
    ),
    UP(
        new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z)
    ),
    NORTH(
        new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z)
    ),
    SOUTH(
        new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z)
    ),
    WEST(
        new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MIN_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z)
    ),
    EAST(
        new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MAX_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MAX_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MIN_Y, FaceInfo.Extent.MIN_Z),
        new FaceInfo.VertexInfo(FaceInfo.Extent.MAX_X, FaceInfo.Extent.MAX_Y, FaceInfo.Extent.MIN_Z)
    );

    private static final Map<Direction, FaceInfo> BY_FACING = Util.make(new EnumMap<>(Direction.class), p_470487_ -> {
        p_470487_.put(Direction.DOWN, DOWN);
        p_470487_.put(Direction.UP, UP);
        p_470487_.put(Direction.NORTH, NORTH);
        p_470487_.put(Direction.SOUTH, SOUTH);
        p_470487_.put(Direction.WEST, WEST);
        p_470487_.put(Direction.EAST, EAST);
    });
    private final FaceInfo.VertexInfo[] infos;

    public static FaceInfo fromFacing(Direction facing) {
        return BY_FACING.get(facing);
    }

    private FaceInfo(FaceInfo.VertexInfo... infos) {
        this.infos = infos;
    }

    public FaceInfo.VertexInfo getVertexInfo(int index) {
        return this.infos[index];
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Extent {
        MIN_X,
        MIN_Y,
        MIN_Z,
        MAX_X,
        MAX_Y,
        MAX_Z;

        public float select(Vector3fc minPos, Vector3fc maxPos) {
            return switch (this) {
                case MIN_X -> minPos.x();
                case MIN_Y -> minPos.y();
                case MIN_Z -> minPos.z();
                case MAX_X -> maxPos.x();
                case MAX_Y -> maxPos.y();
                case MAX_Z -> maxPos.z();
            };
        }

        public float select(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            return switch (this) {
                case MIN_X -> minX;
                case MIN_Y -> minY;
                case MIN_Z -> minZ;
                case MAX_X -> maxX;
                case MAX_Y -> maxY;
                case MAX_Z -> maxZ;
            };
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record VertexInfo(FaceInfo.Extent xFace, FaceInfo.Extent yFace, FaceInfo.Extent zFace) {
        public Vector3f select(Vector3fc minPos, Vector3fc maxPos) {
            return new Vector3f(this.xFace.select(minPos, maxPos), this.yFace.select(minPos, maxPos), this.zFace.select(minPos, maxPos));
        }
    }
}
