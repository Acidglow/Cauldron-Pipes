package net.minecraft.server.players;

import com.google.gson.JsonObject;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;

public class ServerOpListEntry extends StoredUserEntry<NameAndId> {
    private final LevelBasedPermissionSet permissions;
    private final boolean bypassesPlayerLimit;

    public ServerOpListEntry(NameAndId user, LevelBasedPermissionSet permissions, boolean bypassesPlayerLimit) {
        super(user);
        this.permissions = permissions;
        this.bypassesPlayerLimit = bypassesPlayerLimit;
    }

    public ServerOpListEntry(JsonObject entryData) {
        super(NameAndId.fromJson(entryData));
        PermissionLevel permissionlevel = entryData.has("level") ? PermissionLevel.byId(entryData.get("level").getAsInt()) : PermissionLevel.ALL;
        this.permissions = LevelBasedPermissionSet.forLevel(permissionlevel);
        this.bypassesPlayerLimit = entryData.has("bypassesPlayerLimit") && entryData.get("bypassesPlayerLimit").getAsBoolean();
    }

    public LevelBasedPermissionSet permissions() {
        return this.permissions;
    }

    public boolean getBypassesPlayerLimit() {
        return this.bypassesPlayerLimit;
    }

    @Override
    protected void serialize(JsonObject data) {
        if (this.getUser() != null) {
            this.getUser().appendTo(data);
            data.addProperty("level", this.permissions.level().id());
            data.addProperty("bypassesPlayerLimit", this.bypassesPlayerLimit);
        }
    }
}
