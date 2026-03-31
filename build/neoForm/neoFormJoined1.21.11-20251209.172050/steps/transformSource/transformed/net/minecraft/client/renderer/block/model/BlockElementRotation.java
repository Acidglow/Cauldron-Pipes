package net.minecraft.client.renderer.block.model;

import com.mojang.math.MatrixUtil;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public record BlockElementRotation(Vector3fc origin, BlockElementRotation.RotationValue value, boolean rescale, Matrix4fc transform) {
    public BlockElementRotation(Vector3fc p_454819_, BlockElementRotation.RotationValue p_470664_, boolean p_455033_) {
        this(p_454819_, p_470664_, p_455033_, computeTransform(p_470664_, p_455033_));
    }

    private static Matrix4f computeTransform(BlockElementRotation.RotationValue value, boolean rescale) {
        Matrix4f matrix4f = value.transformation();
        if (rescale && !MatrixUtil.isIdentity(matrix4f)) {
            Vector3fc vector3fc = computeRescale(matrix4f);
            matrix4f.scale(vector3fc);
        }

        return matrix4f;
    }

    private static Vector3fc computeRescale(Matrix4fc transformation) {
        Vector3f vector3f = new Vector3f();
        float f = scaleFactorForAxis(transformation, Direction.Axis.X, vector3f);
        float f1 = scaleFactorForAxis(transformation, Direction.Axis.Y, vector3f);
        float f2 = scaleFactorForAxis(transformation, Direction.Axis.Z, vector3f);
        return vector3f.set(f, f1, f2);
    }

    private static float scaleFactorForAxis(Matrix4fc transformation, Direction.Axis axis, Vector3f scratchVector) {
        Vector3f vector3f = scratchVector.set(axis.getPositive().getUnitVec3f());
        Vector3f vector3f1 = transformation.transformDirection(vector3f);
        float f = Math.abs(vector3f1.x);
        float f1 = Math.abs(vector3f1.y);
        float f2 = Math.abs(vector3f1.z);
        float f3 = Math.max(Math.max(f, f1), f2);
        return 1.0F / f3;
    }

    @OnlyIn(Dist.CLIENT)
    public record EulerXYZRotation(float x, float y, float z) implements BlockElementRotation.RotationValue {
        @Override
        public Matrix4f transformation() {
            return new Matrix4f()
                .rotationZYX(
                    this.z * (float) (java.lang.Math.PI / 180.0), this.y * (float) (java.lang.Math.PI / 180.0), this.x * (float) (java.lang.Math.PI / 180.0)
                );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface RotationValue {
        Matrix4f transformation();
    }

    @OnlyIn(Dist.CLIENT)
    public record SingleAxisRotation(Direction.Axis axis, float angle) implements BlockElementRotation.RotationValue {
        @Override
        public Matrix4f transformation() {
            Matrix4f matrix4f = new Matrix4f();
            if (this.angle == 0.0F) {
                return matrix4f;
            } else {
                Vector3fc vector3fc = this.axis.getPositive().getUnitVec3f();
                matrix4f.rotation(this.angle * (float) (java.lang.Math.PI / 180.0), vector3fc);
                return matrix4f;
            }
        }
    }
}
