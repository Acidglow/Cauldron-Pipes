package net.minecraft.gizmos;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

public record RectGizmo(Vec3 a, Vec3 b, Vec3 c, Vec3 d, GizmoStyle style) implements Gizmo {
    public static RectGizmo fromCuboidFace(Vec3 corner1, Vec3 corner2, Direction face, GizmoStyle style) {
        return switch (face) {
            case DOWN -> new RectGizmo(
                new Vec3(corner1.x, corner1.y, corner1.z),
                new Vec3(corner2.x, corner1.y, corner1.z),
                new Vec3(corner2.x, corner1.y, corner2.z),
                new Vec3(corner1.x, corner1.y, corner2.z),
                style
            );
            case UP -> new RectGizmo(
                new Vec3(corner1.x, corner2.y, corner1.z),
                new Vec3(corner1.x, corner2.y, corner2.z),
                new Vec3(corner2.x, corner2.y, corner2.z),
                new Vec3(corner2.x, corner2.y, corner1.z),
                style
            );
            case NORTH -> new RectGizmo(
                new Vec3(corner1.x, corner1.y, corner1.z),
                new Vec3(corner1.x, corner2.y, corner1.z),
                new Vec3(corner2.x, corner2.y, corner1.z),
                new Vec3(corner2.x, corner1.y, corner1.z),
                style
            );
            case SOUTH -> new RectGizmo(
                new Vec3(corner1.x, corner1.y, corner2.z),
                new Vec3(corner2.x, corner1.y, corner2.z),
                new Vec3(corner2.x, corner2.y, corner2.z),
                new Vec3(corner1.x, corner2.y, corner2.z),
                style
            );
            case WEST -> new RectGizmo(
                new Vec3(corner1.x, corner1.y, corner1.z),
                new Vec3(corner1.x, corner1.y, corner2.z),
                new Vec3(corner1.x, corner2.y, corner2.z),
                new Vec3(corner1.x, corner2.y, corner1.z),
                style
            );
            case EAST -> new RectGizmo(
                new Vec3(corner2.x, corner1.y, corner1.z),
                new Vec3(corner2.x, corner2.y, corner1.z),
                new Vec3(corner2.x, corner2.y, corner2.z),
                new Vec3(corner2.x, corner1.y, corner2.z),
                style
            );
        };
    }

    @Override
    public void emit(GizmoPrimitives p_455410_, float p_458133_) {
        if (this.style.hasFill()) {
            int i = this.style.multipliedFill(p_458133_);
            p_455410_.addQuad(this.a, this.b, this.c, this.d, i);
        }

        if (this.style.hasStroke()) {
            int j = this.style.multipliedStroke(p_458133_);
            p_455410_.addLine(this.a, this.b, j, this.style.strokeWidth());
            p_455410_.addLine(this.b, this.c, j, this.style.strokeWidth());
            p_455410_.addLine(this.c, this.d, j, this.style.strokeWidth());
            p_455410_.addLine(this.d, this.a, j, this.style.strokeWidth());
        }
    }
}
