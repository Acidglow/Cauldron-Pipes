package net.minecraft.client.model.geom.builders;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record UVPair(float u, float v) {
    @Override
    public String toString() {
        return "(" + this.u + "," + this.v + ")";
    }

    public static long pack(float u, float v) {
        long i = Float.floatToIntBits(u) & 4294967295L;
        long j = Float.floatToIntBits(v) & 4294967295L;
        return i << 32 | j;
    }

    public static float unpackU(long packed) {
        int i = (int)(packed >> 32);
        return Float.intBitsToFloat(i);
    }

    public static float unpackV(long packed) {
        return Float.intBitsToFloat((int)packed);
    }
}
