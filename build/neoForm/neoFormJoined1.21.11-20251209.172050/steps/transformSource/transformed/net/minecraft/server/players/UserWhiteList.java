package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;

public class UserWhiteList extends StoredUserList<NameAndId, UserWhiteListEntry> {
    public UserWhiteList(File p_11449_, NotificationService p_443514_) {
        super(p_11449_, p_443514_);
    }

    @Override
    protected StoredUserEntry<NameAndId> createEntry(JsonObject entryData) {
        return new UserWhiteListEntry(entryData);
    }

    public boolean isWhiteListed(NameAndId user) {
        return this.contains(user);
    }

    public boolean add(UserWhiteListEntry p_442730_) {
        if (super.add(p_442730_)) {
            if (p_442730_.getUser() != null) {
                this.notificationService.playerAddedToAllowlist(p_442730_.getUser());
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean remove(NameAndId p_442801_) {
        if (super.remove(p_442801_)) {
            this.notificationService.playerRemovedFromAllowlist(p_442801_);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        for (UserWhiteListEntry userwhitelistentry : this.getEntries()) {
            if (userwhitelistentry.getUser() != null) {
                this.notificationService.playerRemovedFromAllowlist(userwhitelistentry.getUser());
            }
        }

        super.clear();
    }

    @Override
    public String[] getUserList() {
        return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(NameAndId::name).toArray(String[]::new);
    }

    protected String getKeyForUser(NameAndId p_435738_) {
        return p_435738_.id().toString();
    }
}
