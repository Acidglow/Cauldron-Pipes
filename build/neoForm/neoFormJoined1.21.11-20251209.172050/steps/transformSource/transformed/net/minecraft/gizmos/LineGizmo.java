package net.minecraft.gizmos;

import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

public record LineGizmo(Vec3 start, Vec3 end, int color, float width) implements Gizmo {
    public static final float DEFAULT_WIDTH = 3.0F;

    @Override
    public void emit(GizmoPrimitives p_456172_, float p_457799_) {
        p_456172_.addLine(this.start, this.end, ARGB.multiplyAlpha(this.color, p_457799_), this.width);
    }
}
