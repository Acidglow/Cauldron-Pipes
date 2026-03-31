package net.minecraft.server.permissions;

public interface LevelBasedPermissionSet extends PermissionSet {
    @Deprecated
    LevelBasedPermissionSet ALL = create(PermissionLevel.ALL);
    LevelBasedPermissionSet MODERATOR = create(PermissionLevel.MODERATORS);
    LevelBasedPermissionSet GAMEMASTER = create(PermissionLevel.GAMEMASTERS);
    LevelBasedPermissionSet ADMIN = create(PermissionLevel.ADMINS);
    LevelBasedPermissionSet OWNER = create(PermissionLevel.OWNERS);

    PermissionLevel level();

    @Override
    default boolean hasPermission(Permission p_456036_) {
        if (p_456036_ instanceof Permission.HasCommandLevel permission$hascommandlevel) {
            return this.level().isEqualOrHigherThan(permission$hascommandlevel.level());
        } else {
            return p_456036_.equals(Permissions.COMMANDS_ENTITY_SELECTORS) ? this.level().isEqualOrHigherThan(PermissionLevel.GAMEMASTERS) : false;
        }
    }

    @Override
    default PermissionSet union(PermissionSet p_470855_) {
        if (p_470855_ instanceof LevelBasedPermissionSet levelbasedpermissionset) {
            return this.level().isEqualOrHigherThan(levelbasedpermissionset.level()) ? levelbasedpermissionset : this;
        } else {
            return PermissionSet.super.union(p_470855_);
        }
    }

    static LevelBasedPermissionSet forLevel(PermissionLevel level) {
        return switch (level) {
            case ALL -> ALL;
            case MODERATORS -> MODERATOR;
            case GAMEMASTERS -> GAMEMASTER;
            case ADMINS -> ADMIN;
            case OWNERS -> OWNER;
        };
    }

    private static LevelBasedPermissionSet create(final PermissionLevel level) {
        return new LevelBasedPermissionSet() {
            @Override
            public PermissionLevel level() {
                return level;
            }

            @Override
            public String toString() {
                return "permission level: " + level.name();
            }
        };
    }
}
