package com.mojang.math;

import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.util.Mth;

public enum Quadrant {
    R0(0, OctahedralGroup.IDENTITY, OctahedralGroup.IDENTITY, OctahedralGroup.IDENTITY),
    R90(1, OctahedralGroup.BLOCK_ROT_X_90, OctahedralGroup.BLOCK_ROT_Y_90, OctahedralGroup.BLOCK_ROT_Z_90),
    R180(2, OctahedralGroup.BLOCK_ROT_X_180, OctahedralGroup.BLOCK_ROT_Y_180, OctahedralGroup.BLOCK_ROT_Z_180),
    R270(3, OctahedralGroup.BLOCK_ROT_X_270, OctahedralGroup.BLOCK_ROT_Y_270, OctahedralGroup.BLOCK_ROT_Z_270);

    public static final Codec<Quadrant> CODEC = Codec.INT.comapFlatMap(p_405170_ -> {
        return switch (Mth.positiveModulo(p_405170_, 360)) {
            case 0 -> DataResult.success(R0);
            case 90 -> DataResult.success(R90);
            case 180 -> DataResult.success(R180);
            case 270 -> DataResult.success(R270);
            default -> DataResult.error(() -> "Invalid rotation " + p_405170_ + " found, only 0/90/180/270 allowed");
        };
    }, p_405217_ -> {
        return switch (p_405217_) {
            case R0 -> 0;
            case R90 -> 90;
            case R180 -> 180;
            case R270 -> 270;
        };
    });
    public final int shift;
    public final OctahedralGroup rotationX;
    public final OctahedralGroup rotationY;
    public final OctahedralGroup rotationZ;

    private Quadrant(int shift, OctahedralGroup rotationX, OctahedralGroup rotationY, OctahedralGroup rotationZ) {
        this.shift = shift;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
    }

    @Deprecated
    public static Quadrant parseJson(int rotation) {
        return switch (Mth.positiveModulo(rotation, 360)) {
            case 0 -> R0;
            case 90 -> R90;
            case 180 -> R180;
            case 270 -> R270;
            default -> throw new JsonParseException("Invalid rotation " + rotation + " found, only 0/90/180/270 allowed");
        };
    }

    public static OctahedralGroup fromXYAngles(Quadrant x, Quadrant y) {
        return y.rotationY.compose(x.rotationX);
    }

    public static OctahedralGroup fromXYZAngles(Quadrant x, Quadrant y, Quadrant z) {
        return z.rotationZ.compose(y.rotationY.compose(x.rotationX));
    }

    public int rotateVertexIndex(int shift) {
        return (shift + this.shift) % 4;
    }
}
