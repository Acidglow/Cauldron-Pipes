package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;

public class ServerOpList extends StoredUserList<NameAndId, ServerOpListEntry> {
    public ServerOpList(File p_11345_, NotificationService p_442529_) {
        super(p_11345_, p_442529_);
    }

    @Override
    protected StoredUserEntry<NameAndId> createEntry(JsonObject entryData) {
        return new ServerOpListEntry(entryData);
    }

    @Override
    public String[] getUserList() {
        return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(NameAndId::name).toArray(String[]::new);
    }

    public boolean add(ServerOpListEntry p_442500_) {
        if (super.add(p_442500_)) {
            if (p_442500_.getUser() != null) {
                this.notificationService.playerOped(p_442500_);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean remove(NameAndId p_442592_) {
        ServerOpListEntry serveroplistentry = this.get(p_442592_);
        if (super.remove(p_442592_)) {
            if (serveroplistentry != null) {
                this.notificationService.playerDeoped(serveroplistentry);
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        for (ServerOpListEntry serveroplistentry : this.getEntries()) {
            if (serveroplistentry.getUser() != null) {
                this.notificationService.playerDeoped(serveroplistentry);
            }
        }

        super.clear();
    }

    public boolean canBypassPlayerLimit(NameAndId nameAndId) {
        ServerOpListEntry serveroplistentry = this.get(nameAndId);
        return serveroplistentry != null ? serveroplistentry.getBypassesPlayerLimit() : false;
    }

    protected String getKeyForUser(NameAndId p_433823_) {
        return p_433823_.id().toString();
    }
}
