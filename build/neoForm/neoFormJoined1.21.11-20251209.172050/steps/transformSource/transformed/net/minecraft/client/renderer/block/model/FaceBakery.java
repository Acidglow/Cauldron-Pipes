package net.minecraft.client.renderer.block.model;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.math.MatrixUtil;
import com.mojang.math.Quadrant;
import com.mojang.math.Transformation;
import java.util.Objects;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.GeometryUtils;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class FaceBakery {
    private static final Vector3fc BLOCK_MIDDLE = new Vector3f(0.5F, 0.5F, 0.5F);

    @VisibleForTesting
    public static BlockElementFace.UVs defaultFaceUV(Vector3fc posFrom, Vector3fc posTo, Direction facing) {
        return switch (facing) {
            case DOWN -> new BlockElementFace.UVs(posFrom.x(), 16.0F - posTo.z(), posTo.x(), 16.0F - posFrom.z());
            case UP -> new BlockElementFace.UVs(posFrom.x(), posFrom.z(), posTo.x(), posTo.z());
            case NORTH -> new BlockElementFace.UVs(16.0F - posTo.x(), 16.0F - posTo.y(), 16.0F - posFrom.x(), 16.0F - posFrom.y());
            case SOUTH -> new BlockElementFace.UVs(posFrom.x(), 16.0F - posTo.y(), posTo.x(), 16.0F - posFrom.y());
            case WEST -> new BlockElementFace.UVs(posFrom.z(), 16.0F - posTo.y(), posTo.z(), 16.0F - posFrom.y());
            case EAST -> new BlockElementFace.UVs(16.0F - posTo.z(), 16.0F - posTo.y(), 16.0F - posFrom.z(), 16.0F - posFrom.y());
        };
    }

    public static BakedQuad bakeQuad(
        ModelBaker.PartCache partCache,
        Vector3fc posFrom,
        Vector3fc posTo,
        BlockElementFace face,
        TextureAtlasSprite sprite,
        Direction facing,
        ModelState modelState,
        @Nullable BlockElementRotation rotation,
        boolean shade,
        int lightEmission
    ) {
        BlockElementFace.UVs blockelementface$uvs = face.uvs();
        if (blockelementface$uvs == null) {
            blockelementface$uvs = defaultFaceUV(posFrom, posTo, facing);
        }

        Matrix4fc matrix4fc = modelState.inverseFaceTransformation(facing);
        Vector3fc[] avector3fc = new Vector3fc[4];
        long[] along = new long[4];
        FaceInfo faceinfo = FaceInfo.fromFacing(facing);

        for (int i = 0; i < 4; i++) {
            bakeVertex(
                i,
                faceinfo,
                blockelementface$uvs,
                face.rotation(),
                matrix4fc,
                posFrom,
                posTo,
                sprite,
                modelState.transformation(),
                rotation,
                avector3fc,
                along,
                partCache
            );
        }

        Direction direction = calculateFacing(avector3fc);
        if (rotation == null && direction != null) {
            // Neo: Suppress winding re-calculation when the quads may not be axis-aligned due to root transforms
            if (!modelState.mayApplyArbitraryRotation())
            recalculateWinding(avector3fc, along, direction);
        }

        return new BakedQuad(
            avector3fc[0],
            avector3fc[1],
            avector3fc[2],
            avector3fc[3],
            along[0],
            along[1],
            along[2],
            along[3],
            face.tintIndex(),
            Objects.requireNonNullElse(direction, Direction.UP),
            sprite,
            shade,
            Math.max(lightEmission, face.faceData().lightEmission()),
            partCache.normals(net.neoforged.neoforge.client.model.quad.BakedNormals.of(net.neoforged.neoforge.client.model.quad.BakedNormals.computeQuadNormal(avector3fc[0], avector3fc[1], avector3fc[2], avector3fc[3]))),
            partCache.colors(net.neoforged.neoforge.client.model.quad.BakedColors.of(face.faceData().color())),
            face.faceData().ambientOcclusion()
        );
    }

    private static void bakeVertex(
        int vertexIndex,
        FaceInfo faceInfo,
        BlockElementFace.UVs uvs,
        Quadrant faceRotation,
        Matrix4fc inverseFaceTransform,
        Vector3fc posFrom,
        Vector3fc posTo,
        TextureAtlasSprite sprite,
        Transformation transformation,
        @Nullable BlockElementRotation rotation,
        Vector3fc[] positions,
        long[] packedUVs,
        ModelBaker.PartCache partCache
    ) {
        FaceInfo.VertexInfo faceinfo$vertexinfo = faceInfo.getVertexInfo(vertexIndex);
        Vector3f vector3f = faceinfo$vertexinfo.select(posFrom, posTo).div(16.0F);
        if (rotation != null) {
            rotateVertexBy(vector3f, rotation.origin(), rotation.transform());
        }

        if (transformation != Transformation.identity()) {
            rotateVertexBy(vector3f, BLOCK_MIDDLE, transformation.getMatrix());
        }

        float f = BlockElementFace.getU(uvs, faceRotation, vertexIndex);
        float f1 = BlockElementFace.getV(uvs, faceRotation, vertexIndex);
        float f2;
        float f3;
        if (MatrixUtil.isIdentity(inverseFaceTransform)) {
            f3 = f;
            f2 = f1;
        } else {
            Vector3f vector3f1 = inverseFaceTransform.transformPosition(new Vector3f(cornerToCenter(f), cornerToCenter(f1), 0.0F));
            f3 = centerToCorner(vector3f1.x);
            f2 = centerToCorner(vector3f1.y);
        }

        positions[vertexIndex] = partCache.vector(vector3f);
        packedUVs[vertexIndex] = UVPair.pack(sprite.getU(f3), sprite.getV(f2));
    }

    private static float cornerToCenter(float coord) {
        return coord - 0.5F;
    }

    private static float centerToCorner(float coord) {
        return coord + 0.5F;
    }

    private static void rotateVertexBy(Vector3f vertex, Vector3fc origin, Matrix4fc transform) {
        vertex.sub(origin);
        transform.transformPosition(vertex);
        vertex.add(origin);
    }

    private static @Nullable Direction calculateFacing(Vector3fc[] positions) {
        Vector3f vector3f = new Vector3f();
        GeometryUtils.normal(positions[0], positions[1], positions[2], vector3f);
        return findClosestDirection(vector3f);
    }

    private static @Nullable Direction findClosestDirection(Vector3f pos) {
        if (!pos.isFinite()) {
            return null;
        } else {
            Direction direction = null;
            float f = 0.0F;

            for (Direction direction1 : Direction.values()) {
                float f1 = pos.dot(direction1.getUnitVec3f());
                if (f1 >= 0.0F && f1 > f) {
                    f = f1;
                    direction = direction1;
                }
            }

            return direction;
        }
    }

    private static void recalculateWinding(Vector3fc[] positions, long[] packedUVs, Direction facing) {
        float f = 999.0F;
        float f1 = 999.0F;
        float f2 = 999.0F;
        float f3 = -999.0F;
        float f4 = -999.0F;
        float f5 = -999.0F;

        for (int i = 0; i < 4; i++) {
            Vector3fc vector3fc = positions[i];
            float f6 = vector3fc.x();
            float f7 = vector3fc.y();
            float f8 = vector3fc.z();
            if (f6 < f) {
                f = f6;
            }

            if (f7 < f1) {
                f1 = f7;
            }

            if (f8 < f2) {
                f2 = f8;
            }

            if (f6 > f3) {
                f3 = f6;
            }

            if (f7 > f4) {
                f4 = f7;
            }

            if (f8 > f5) {
                f5 = f8;
            }
        }

        FaceInfo faceinfo = FaceInfo.fromFacing(facing);

        for (int k = 0; k < 4; k++) {
            FaceInfo.VertexInfo faceinfo$vertexinfo = faceinfo.getVertexInfo(k);
            float f10 = faceinfo$vertexinfo.xFace().select(f, f1, f2, f3, f4, f5);
            float f11 = faceinfo$vertexinfo.yFace().select(f, f1, f2, f3, f4, f5);
            float f9 = faceinfo$vertexinfo.zFace().select(f, f1, f2, f3, f4, f5);
            int j = findVertex(positions, k, f10, f11, f9);
            if (j == -1) {
                throw new IllegalStateException("Can't find vertex to swap");
            }

            if (j != k) {
                swap(positions, j, k);
                swap(packedUVs, j, k);
            }
        }
    }

    private static int findVertex(Vector3fc[] positions, int startIndex, float x, float y, float z) {
        for (int i = startIndex; i < 4; i++) {
            Vector3fc vector3fc = positions[i];
            if (x == vector3fc.x() && y == vector3fc.y() && z == vector3fc.z()) {
                return i;
            }
        }

        return -1;
    }

    private static void swap(Vector3fc[] array, int index1, int index2) {
        Vector3fc vector3fc = array[index1];
        array[index1] = array[index2];
        array[index2] = vector3fc;
    }

    private static void swap(long[] array, int index1, int index2) {
        long i = array[index1];
        array[index1] = array[index2];
        array[index2] = i;
    }
}
