package com.mojang.math;

import java.util.Arrays;
import net.minecraft.core.Direction;
import net.minecraft.util.Util;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * The symmetric group S3, also known as all the permutation orders of three elements.
 */
public enum SymmetricGroup3 {
    P123(0, 1, 2),
    P213(1, 0, 2),
    P132(0, 2, 1),
    P312(2, 0, 1),
    P231(1, 2, 0),
    P321(2, 1, 0);

    private final int p0;
    private final int p1;
    private final int p2;
    private final Matrix3fc transformation;
    private static final SymmetricGroup3[][] CAYLEY_TABLE = Util.make(
        () -> {
            SymmetricGroup3[] asymmetricgroup3 = values();
            SymmetricGroup3[][] asymmetricgroup31 = new SymmetricGroup3[asymmetricgroup3.length][asymmetricgroup3.length];

            for (SymmetricGroup3 symmetricgroup3 : asymmetricgroup3) {
                for (SymmetricGroup3 symmetricgroup31 : asymmetricgroup3) {
                    int i = symmetricgroup3.permute(symmetricgroup31.p0);
                    int j = symmetricgroup3.permute(symmetricgroup31.p1);
                    int k = symmetricgroup3.permute(symmetricgroup31.p2);
                    SymmetricGroup3 symmetricgroup32 = Arrays.stream(asymmetricgroup3)
                        .filter(p_454084_ -> p_454084_.p0 == i && p_454084_.p1 == j && p_454084_.p2 == k)
                        .findFirst()
                        .get();
                    asymmetricgroup31[symmetricgroup3.ordinal()][symmetricgroup31.ordinal()] = symmetricgroup32;
                }
            }

            return asymmetricgroup31;
        }
    );
    private static final SymmetricGroup3[] INVERSE_TABLE = Util.make(
        () -> {
            SymmetricGroup3[] asymmetricgroup3 = values();
            return Arrays.stream(asymmetricgroup3)
                .map(p_454088_ -> Arrays.stream(values()).filter(p_454087_ -> p_454088_.compose(p_454087_) == P123).findAny().get())
                .toArray(SymmetricGroup3[]::new);
        }
    );

    private SymmetricGroup3(int first, int second, int third) {
        this.p0 = first;
        this.p1 = second;
        this.p2 = third;
        this.transformation = new Matrix3f().zero().set(this.permute(0), 0, 1.0F).set(this.permute(1), 1, 1.0F).set(this.permute(2), 2, 1.0F);
    }

    public SymmetricGroup3 compose(SymmetricGroup3 other) {
        return CAYLEY_TABLE[this.ordinal()][other.ordinal()];
    }

    public SymmetricGroup3 inverse() {
        return INVERSE_TABLE[this.ordinal()];
    }

    public int permute(int axis) {
        return switch (axis) {
            case 0 -> this.p0;
            case 1 -> this.p1;
            case 2 -> this.p2;
            default -> throw new IllegalArgumentException("Must be 0, 1 or 2, but got " + axis);
        };
    }

    public Direction.Axis permuteAxis(Direction.Axis axis) {
        return Direction.Axis.VALUES[this.permute(axis.ordinal())];
    }

    public Vector3f permuteVector(Vector3f vector) {
        float f = vector.get(this.p0);
        float f1 = vector.get(this.p1);
        float f2 = vector.get(this.p2);
        return vector.set(f, f1, f2);
    }

    public Vector3i permuteVector(Vector3i vector) {
        int i = vector.get(this.p0);
        int j = vector.get(this.p1);
        int k = vector.get(this.p2);
        return vector.set(i, j, k);
    }

    public Matrix3fc transformation() {
        return this.transformation;
    }
}
