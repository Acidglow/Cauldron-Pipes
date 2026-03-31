package net.minecraft.world.phys.shapes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.math.DoubleMath;
import com.google.common.math.IntMath;
import com.mojang.math.OctahedralGroup;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.AxisCycle;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Shapes {
    public static final double EPSILON = 1.0E-7;
    public static final double BIG_EPSILON = 1.0E-6;
    private static final VoxelShape BLOCK = Util.make(() -> {
        DiscreteVoxelShape discretevoxelshape = new BitSetDiscreteVoxelShape(1, 1, 1);
        discretevoxelshape.fill(0, 0, 0);
        return new CubeVoxelShape(discretevoxelshape);
    });
    private static final Vec3 BLOCK_CENTER = new Vec3(0.5, 0.5, 0.5);
    public static final VoxelShape INFINITY = box(
        Double.NEGATIVE_INFINITY,
        Double.NEGATIVE_INFINITY,
        Double.NEGATIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY,
        Double.POSITIVE_INFINITY
    );
    private static final VoxelShape EMPTY = new ArrayVoxelShape(
        new BitSetDiscreteVoxelShape(0, 0, 0),
        new DoubleArrayList(new double[]{0.0}),
        new DoubleArrayList(new double[]{0.0}),
        new DoubleArrayList(new double[]{0.0})
    );

    public static VoxelShape empty() {
        return EMPTY;
    }

    public static VoxelShape block() {
        return BLOCK;
    }

    public static VoxelShape box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!(minX > maxX) && !(minY > maxY) && !(minZ > maxZ)) {
            return create(minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            throw new IllegalArgumentException("The min values need to be smaller or equals to the max values");
        }
    }

    public static VoxelShape create(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        if (!(maxX - minX < 1.0E-7) && !(maxY - minY < 1.0E-7) && !(maxZ - minZ < 1.0E-7)) {
            int i = findBits(minX, maxX);
            int j = findBits(minY, maxY);
            int k = findBits(minZ, maxZ);
            if (i < 0 || j < 0 || k < 0) {
                return new ArrayVoxelShape(
                    BLOCK.shape,
                    DoubleArrayList.wrap(new double[]{minX, maxX}),
                    DoubleArrayList.wrap(new double[]{minY, maxY}),
                    DoubleArrayList.wrap(new double[]{minZ, maxZ})
                );
            } else if (i == 0 && j == 0 && k == 0) {
                return block();
            } else {
                int l = 1 << i;
                int i1 = 1 << j;
                int j1 = 1 << k;
                BitSetDiscreteVoxelShape bitsetdiscretevoxelshape = BitSetDiscreteVoxelShape.withFilledBounds(
                    l,
                    i1,
                    j1,
                    (int)Math.round(minX * l),
                    (int)Math.round(minY * i1),
                    (int)Math.round(minZ * j1),
                    (int)Math.round(maxX * l),
                    (int)Math.round(maxY * i1),
                    (int)Math.round(maxZ * j1)
                );
                return new CubeVoxelShape(bitsetdiscretevoxelshape);
            }
        } else {
            return empty();
        }
    }

    public static VoxelShape create(AABB aabb) {
        return create(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    @VisibleForTesting
    protected static int findBits(double minBits, double maxBits) {
        if (!(minBits < -1.0E-7) && !(maxBits > 1.0000001)) {
            for (int i = 0; i <= 3; i++) {
                int j = 1 << i;
                double d0 = minBits * j;
                double d1 = maxBits * j;
                boolean flag = Math.abs(d0 - Math.round(d0)) < 1.0E-7 * j;
                boolean flag1 = Math.abs(d1 - Math.round(d1)) < 1.0E-7 * j;
                if (flag && flag1) {
                    return i;
                }
            }

            return -1;
        } else {
            return -1;
        }
    }

    protected static long lcm(int aa, int bb) {
        return (long)aa * (bb / IntMath.gcd(aa, bb));
    }

    public static VoxelShape or(VoxelShape shape1, VoxelShape shape2) {
        return join(shape1, shape2, BooleanOp.OR);
    }

    public static VoxelShape or(VoxelShape shape1, VoxelShape... others) {
        return Arrays.stream(others).reduce(shape1, Shapes::or);
    }

    public static VoxelShape join(VoxelShape shape1, VoxelShape shape2, BooleanOp function) {
        return joinUnoptimized(shape1, shape2, function).optimize();
    }

    public static VoxelShape joinUnoptimized(VoxelShape shape1, VoxelShape shape2, BooleanOp function) {
        if (function.apply(false, false)) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
        } else if (shape1 == shape2) {
            return function.apply(true, true) ? shape1 : empty();
        } else {
            boolean flag = function.apply(true, false);
            boolean flag1 = function.apply(false, true);
            if (shape1.isEmpty()) {
                return flag1 ? shape2 : empty();
            } else if (shape2.isEmpty()) {
                return flag ? shape1 : empty();
            } else {
                IndexMerger indexmerger = createIndexMerger(1, shape1.getCoords(Direction.Axis.X), shape2.getCoords(Direction.Axis.X), flag, flag1);
                IndexMerger indexmerger1 = createIndexMerger(
                    indexmerger.size() - 1, shape1.getCoords(Direction.Axis.Y), shape2.getCoords(Direction.Axis.Y), flag, flag1
                );
                IndexMerger indexmerger2 = createIndexMerger(
                    (indexmerger.size() - 1) * (indexmerger1.size() - 1),
                    shape1.getCoords(Direction.Axis.Z),
                    shape2.getCoords(Direction.Axis.Z),
                    flag,
                    flag1
                );
                BitSetDiscreteVoxelShape bitsetdiscretevoxelshape = BitSetDiscreteVoxelShape.join(
                    shape1.shape, shape2.shape, indexmerger, indexmerger1, indexmerger2, function
                );
                return (VoxelShape)(indexmerger instanceof DiscreteCubeMerger
                        && indexmerger1 instanceof DiscreteCubeMerger
                        && indexmerger2 instanceof DiscreteCubeMerger
                    ? new CubeVoxelShape(bitsetdiscretevoxelshape)
                    : new ArrayVoxelShape(bitsetdiscretevoxelshape, indexmerger.getList(), indexmerger1.getList(), indexmerger2.getList()));
            }
        }
    }

    public static boolean joinIsNotEmpty(VoxelShape shape1, VoxelShape shape2, BooleanOp resultOperator) {
        if (resultOperator.apply(false, false)) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException());
        } else {
            boolean flag = shape1.isEmpty();
            boolean flag1 = shape2.isEmpty();
            if (!flag && !flag1) {
                if (shape1 == shape2) {
                    return resultOperator.apply(true, true);
                } else {
                    boolean flag2 = resultOperator.apply(true, false);
                    boolean flag3 = resultOperator.apply(false, true);

                    for (Direction.Axis direction$axis : AxisCycle.AXIS_VALUES) {
                        if (shape1.max(direction$axis) < shape2.min(direction$axis) - 1.0E-7) {
                            return flag2 || flag3;
                        }

                        if (shape2.max(direction$axis) < shape1.min(direction$axis) - 1.0E-7) {
                            return flag2 || flag3;
                        }
                    }

                    IndexMerger indexmerger = createIndexMerger(1, shape1.getCoords(Direction.Axis.X), shape2.getCoords(Direction.Axis.X), flag2, flag3);
                    IndexMerger indexmerger1 = createIndexMerger(
                        indexmerger.size() - 1, shape1.getCoords(Direction.Axis.Y), shape2.getCoords(Direction.Axis.Y), flag2, flag3
                    );
                    IndexMerger indexmerger2 = createIndexMerger(
                        (indexmerger.size() - 1) * (indexmerger1.size() - 1),
                        shape1.getCoords(Direction.Axis.Z),
                        shape2.getCoords(Direction.Axis.Z),
                        flag2,
                        flag3
                    );
                    return joinIsNotEmpty(indexmerger, indexmerger1, indexmerger2, shape1.shape, shape2.shape, resultOperator);
                }
            } else {
                return resultOperator.apply(!flag, !flag1);
            }
        }
    }

    private static boolean joinIsNotEmpty(
        IndexMerger mergerX, IndexMerger mergerY, IndexMerger mergerZ, DiscreteVoxelShape primaryShape, DiscreteVoxelShape secondaryShape, BooleanOp resultOperator
    ) {
        return !mergerX.forMergedIndexes(
            (p_83100_, p_83101_, p_83102_) -> mergerY.forMergedIndexes(
                (p_166046_, p_166047_, p_166048_) -> mergerZ.forMergedIndexes(
                    (p_166036_, p_166037_, p_166038_) -> !resultOperator.apply(
                        primaryShape.isFullWide(p_83100_, p_166046_, p_166036_), secondaryShape.isFullWide(p_83101_, p_166047_, p_166037_)
                    )
                )
            )
        );
    }

    public static double collide(Direction.Axis movementAxis, AABB collisionBox, Iterable<VoxelShape> possibleHits, double desiredOffset) {
        for (VoxelShape voxelshape : possibleHits) {
            if (Math.abs(desiredOffset) < 1.0E-7) {
                return 0.0;
            }

            desiredOffset = voxelshape.collide(movementAxis, collisionBox, desiredOffset);
        }

        return desiredOffset;
    }

    public static boolean blockOccludes(VoxelShape shape, VoxelShape adjacentShape, Direction side) {
        if (shape == block() && adjacentShape == block()) {
            return true;
        } else if (adjacentShape.isEmpty()) {
            return false;
        } else {
            Direction.Axis direction$axis = side.getAxis();
            Direction.AxisDirection direction$axisdirection = side.getAxisDirection();
            VoxelShape voxelshape = direction$axisdirection == Direction.AxisDirection.POSITIVE ? shape : adjacentShape;
            VoxelShape voxelshape1 = direction$axisdirection == Direction.AxisDirection.POSITIVE ? adjacentShape : shape;
            BooleanOp booleanop = direction$axisdirection == Direction.AxisDirection.POSITIVE ? BooleanOp.ONLY_FIRST : BooleanOp.ONLY_SECOND;
            return DoubleMath.fuzzyEquals(voxelshape.max(direction$axis), 1.0, 1.0E-7)
                && DoubleMath.fuzzyEquals(voxelshape1.min(direction$axis), 0.0, 1.0E-7)
                && !joinIsNotEmpty(
                    new SliceShape(voxelshape, direction$axis, voxelshape.shape.getSize(direction$axis) - 1),
                    new SliceShape(voxelshape1, direction$axis, 0),
                    booleanop
                );
        }
    }

    public static boolean mergedFaceOccludes(VoxelShape shape, VoxelShape adjacentShape, Direction side) {
        if (shape != block() && adjacentShape != block()) {
            Direction.Axis direction$axis = side.getAxis();
            Direction.AxisDirection direction$axisdirection = side.getAxisDirection();
            VoxelShape voxelshape = direction$axisdirection == Direction.AxisDirection.POSITIVE ? shape : adjacentShape;
            VoxelShape voxelshape1 = direction$axisdirection == Direction.AxisDirection.POSITIVE ? adjacentShape : shape;
            if (!DoubleMath.fuzzyEquals(voxelshape.max(direction$axis), 1.0, 1.0E-7)) {
                voxelshape = empty();
            }

            if (!DoubleMath.fuzzyEquals(voxelshape1.min(direction$axis), 0.0, 1.0E-7)) {
                voxelshape1 = empty();
            }

            return !joinIsNotEmpty(
                block(),
                joinUnoptimized(
                    new SliceShape(voxelshape, direction$axis, voxelshape.shape.getSize(direction$axis) - 1),
                    new SliceShape(voxelshape1, direction$axis, 0),
                    BooleanOp.OR
                ),
                BooleanOp.ONLY_FIRST
            );
        } else {
            return true;
        }
    }

    public static boolean faceShapeOccludes(VoxelShape voxelShape1, VoxelShape voxelShape2) {
        if (voxelShape1 == block() || voxelShape2 == block()) {
            return true;
        } else {
            return voxelShape1.isEmpty() && voxelShape2.isEmpty()
                ? false
                : !joinIsNotEmpty(block(), joinUnoptimized(voxelShape1, voxelShape2, BooleanOp.OR), BooleanOp.ONLY_FIRST);
        }
    }

    @VisibleForTesting
    protected static IndexMerger createIndexMerger(int size, DoubleList list1, DoubleList list2, boolean excludeUpper, boolean excludeLower) {
        int i = list1.size() - 1;
        int j = list2.size() - 1;
        if (list1 instanceof CubePointRange && list2 instanceof CubePointRange) {
            long k = lcm(i, j);
            if (size * k <= 256L) {
                return new DiscreteCubeMerger(i, j);
            }
        }

        if (list1.getDouble(i) < list2.getDouble(0) - 1.0E-7) {
            return new NonOverlappingMerger(list1, list2, false);
        } else if (list2.getDouble(j) < list1.getDouble(0) - 1.0E-7) {
            return new NonOverlappingMerger(list2, list1, true);
        } else {
            return (IndexMerger)(i == j && Objects.equals(list1, list2)
                ? new IdenticalMerger(list1)
                : new IndirectMerger(list1, list2, excludeUpper, excludeLower));
        }
    }

    public static VoxelShape rotate(VoxelShape shape, OctahedralGroup octohedralGroup) {
        return rotate(shape, octohedralGroup, BLOCK_CENTER);
    }

    public static VoxelShape rotate(VoxelShape shape, OctahedralGroup octohedralGroup, Vec3 pos) {
        if (octohedralGroup == OctahedralGroup.IDENTITY) {
            return shape;
        } else {
            DiscreteVoxelShape discretevoxelshape = shape.shape.rotate(octohedralGroup);
            if (shape instanceof CubeVoxelShape && BLOCK_CENTER.equals(pos)) {
                return new CubeVoxelShape(discretevoxelshape);
            } else {
                Direction.Axis direction$axis = octohedralGroup.permutation().permuteAxis(Direction.Axis.X);
                Direction.Axis direction$axis1 = octohedralGroup.permutation().permuteAxis(Direction.Axis.Y);
                Direction.Axis direction$axis2 = octohedralGroup.permutation().permuteAxis(Direction.Axis.Z);
                DoubleList doublelist = shape.getCoords(direction$axis);
                DoubleList doublelist1 = shape.getCoords(direction$axis1);
                DoubleList doublelist2 = shape.getCoords(direction$axis2);
                boolean flag = octohedralGroup.inverts(Direction.Axis.X);
                boolean flag1 = octohedralGroup.inverts(Direction.Axis.Y);
                boolean flag2 = octohedralGroup.inverts(Direction.Axis.Z);
                return new ArrayVoxelShape(
                    discretevoxelshape,
                    flipAxisIfNeeded(doublelist, flag, pos.get(direction$axis), pos.x),
                    flipAxisIfNeeded(doublelist1, flag1, pos.get(direction$axis1), pos.y),
                    flipAxisIfNeeded(doublelist2, flag2, pos.get(direction$axis2), pos.z)
                );
            }
        }
    }

    @VisibleForTesting
    static DoubleList flipAxisIfNeeded(DoubleList coords, boolean flip, double originalOrigin, double newOrigin) {
        if (!flip && originalOrigin == newOrigin) {
            return coords;
        } else {
            int i = coords.size();
            DoubleList doublelist = new DoubleArrayList(i);
            if (flip) {
                for (int j = i - 1; j >= 0; j--) {
                    doublelist.add(-(coords.getDouble(j) - originalOrigin) + newOrigin);
                }
            } else {
                for (int k = 0; k >= 0 && k < i; k++) {
                    doublelist.add(coords.getDouble(k) - originalOrigin + newOrigin);
                }
            }

            return doublelist;
        }
    }

    public static boolean equal(VoxelShape first, VoxelShape second) {
        return !joinIsNotEmpty(first, second, BooleanOp.NOT_SAME);
    }

    public static Map<Direction.Axis, VoxelShape> rotateHorizontalAxis(VoxelShape shape) {
        return rotateHorizontalAxis(shape, BLOCK_CENTER);
    }

    public static Map<Direction.Axis, VoxelShape> rotateHorizontalAxis(VoxelShape shape, Vec3 pos) {
        return Maps.newEnumMap(Map.of(Direction.Axis.Z, shape, Direction.Axis.X, rotate(shape, OctahedralGroup.BLOCK_ROT_Y_90, pos)));
    }

    public static Map<Direction.Axis, VoxelShape> rotateAllAxis(VoxelShape shape) {
        return rotateAllAxis(shape, BLOCK_CENTER);
    }

    public static Map<Direction.Axis, VoxelShape> rotateAllAxis(VoxelShape shape, Vec3 pos) {
        return Maps.newEnumMap(
            Map.of(
                Direction.Axis.Z,
                shape,
                Direction.Axis.X,
                rotate(shape, OctahedralGroup.BLOCK_ROT_Y_90, pos),
                Direction.Axis.Y,
                rotate(shape, OctahedralGroup.BLOCK_ROT_X_90, pos)
            )
        );
    }

    public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape shape) {
        return rotateHorizontal(shape, OctahedralGroup.IDENTITY, BLOCK_CENTER);
    }

    public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape shape, OctahedralGroup rotation) {
        return rotateHorizontal(shape, rotation, BLOCK_CENTER);
    }

    public static Map<Direction, VoxelShape> rotateHorizontal(VoxelShape shape, OctahedralGroup rotation, Vec3 origin) {
        return Maps.newEnumMap(
            Map.of(
                Direction.NORTH,
                rotate(shape, rotation),
                Direction.EAST,
                rotate(shape, OctahedralGroup.BLOCK_ROT_Y_90.compose(rotation), origin),
                Direction.SOUTH,
                rotate(shape, OctahedralGroup.BLOCK_ROT_Y_180.compose(rotation), origin),
                Direction.WEST,
                rotate(shape, OctahedralGroup.BLOCK_ROT_Y_270.compose(rotation), origin)
            )
        );
    }

    public static Map<Direction, VoxelShape> rotateAll(VoxelShape shape) {
        return rotateAll(shape, OctahedralGroup.IDENTITY, BLOCK_CENTER);
    }

    public static Map<Direction, VoxelShape> rotateAll(VoxelShape shape, Vec3 pos) {
        return rotateAll(shape, OctahedralGroup.IDENTITY, pos);
    }

    public static Map<Direction, VoxelShape> rotateAll(VoxelShape shape, OctahedralGroup rotation, Vec3 origin) {
        return Maps.newEnumMap(
            Map.of(
                Direction.NORTH,
                rotate(shape, rotation),
                Direction.EAST,
                rotate(shape, OctahedralGroup.BLOCK_ROT_Y_90.compose(rotation), origin),
                Direction.SOUTH,
                rotate(shape, OctahedralGroup.BLOCK_ROT_Y_180.compose(rotation), origin),
                Direction.WEST,
                rotate(shape, OctahedralGroup.BLOCK_ROT_Y_270.compose(rotation), origin),
                Direction.UP,
                rotate(shape, OctahedralGroup.BLOCK_ROT_X_270.compose(rotation), origin),
                Direction.DOWN,
                rotate(shape, OctahedralGroup.BLOCK_ROT_X_90.compose(rotation), origin)
            )
        );
    }

    public static Map<AttachFace, Map<Direction, VoxelShape>> rotateAttachFace(VoxelShape shape) {
        return rotateAttachFace(shape, OctahedralGroup.IDENTITY);
    }

    public static Map<AttachFace, Map<Direction, VoxelShape>> rotateAttachFace(VoxelShape shape, OctahedralGroup rortation) {
        return Map.of(
            AttachFace.WALL,
            rotateHorizontal(shape, rortation),
            AttachFace.FLOOR,
            rotateHorizontal(shape, OctahedralGroup.BLOCK_ROT_X_270.compose(rortation)),
            AttachFace.CEILING,
            rotateHorizontal(shape, OctahedralGroup.BLOCK_ROT_Y_180.compose(OctahedralGroup.BLOCK_ROT_X_90).compose(rortation))
        );
    }

    public interface DoubleLineConsumer {
        void consume(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);
    }
}
