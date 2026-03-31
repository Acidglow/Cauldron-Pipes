package net.minecraft.server.permissions;

public interface PermissionSet {
    PermissionSet NO_PERMISSIONS = p_454771_ -> false;
    PermissionSet ALL_PERMISSIONS = p_454708_ -> true;

    boolean hasPermission(Permission permission);

    default PermissionSet union(PermissionSet other) {
        return (PermissionSet)(other instanceof PermissionSetUnion ? other.union(this) : new PermissionSetUnion(this, other));
    }
}
