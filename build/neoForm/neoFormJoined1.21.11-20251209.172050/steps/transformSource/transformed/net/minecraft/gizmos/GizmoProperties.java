package net.minecraft.gizmos;

public interface GizmoProperties {
    GizmoProperties setAlwaysOnTop();

    GizmoProperties persistForMillis(int millis);

    GizmoProperties fadeOut();
}
