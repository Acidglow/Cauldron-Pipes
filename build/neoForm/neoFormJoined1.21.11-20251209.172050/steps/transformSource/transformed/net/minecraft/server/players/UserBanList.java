package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.util.Objects;
import net.minecraft.server.notifications.NotificationService;

public class UserBanList extends StoredUserList<NameAndId, UserBanListEntry> {
    public UserBanList(File p_11402_, NotificationService p_443108_) {
        super(p_11402_, p_443108_);
    }

    @Override
    protected StoredUserEntry<NameAndId> createEntry(JsonObject entryData) {
        return new UserBanListEntry(entryData);
    }

    public boolean isBanned(NameAndId user) {
        return this.contains(user);
    }

    @Override
    public String[] getUserList() {
        return this.getEntries().stream().map(StoredUserEntry::getUser).filter(Objects::nonNull).map(NameAndId::name).toArray(String[]::new);
    }

    protected String getKeyForUser(NameAndId p_434532_) {
        return p_434532_.id().toString();
    }

    public boolean add(UserBanListEntry p_442765_) {
        if (super.add(p_442765_)) {
            if (p_442765_.getUser() != null) {
                this.notificationService.playerBanned(p_442765_);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean remove(NameAndId p_443373_) {
        if (super.remove(p_443373_)) {
            this.notificationService.playerUnbanned(p_443373_);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        for (UserBanListEntry userbanlistentry : this.getEntries()) {
            if (userbanlistentry.getUser() != null) {
                this.notificationService.playerUnbanned(userbanlistentry.getUser());
            }
        }

        super.clear();
    }
}
