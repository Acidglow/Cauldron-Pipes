package net.minecraft.gizmos;

public interface GizmoCollector {
    GizmoProperties IGNORED = new GizmoProperties() {
        @Override
        public GizmoProperties setAlwaysOnTop() {
            return this;
        }

        @Override
        public GizmoProperties persistForMillis(int p_458934_) {
            return this;
        }

        @Override
        public GizmoProperties fadeOut() {
            return this;
        }
    };
    GizmoCollector NOOP = p_459080_ -> IGNORED;

    GizmoProperties add(Gizmo gizmo);
}
