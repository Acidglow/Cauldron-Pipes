package net.minecraft.server.players;

import com.google.gson.JsonObject;
import java.io.File;
import java.net.SocketAddress;
import net.minecraft.server.notifications.NotificationService;
import org.jspecify.annotations.Nullable;

public class IpBanList extends StoredUserList<String, IpBanListEntry> {
    public IpBanList(File p_11036_, NotificationService p_442638_) {
        super(p_11036_, p_442638_);
    }

    @Override
    protected StoredUserEntry<String> createEntry(JsonObject entryData) {
        return new IpBanListEntry(entryData);
    }

    public boolean isBanned(SocketAddress address) {
        String s = this.getIpFromAddress(address);
        return this.contains(s);
    }

    public boolean isBanned(String address) {
        return this.contains(address);
    }

    public @Nullable IpBanListEntry get(SocketAddress address) {
        String s = this.getIpFromAddress(address);
        return this.get(s);
    }

    private String getIpFromAddress(SocketAddress address) {
        String s = address.toString();
        if (s.contains("/")) {
            s = s.substring(s.indexOf(47) + 1);
        }

        if (s.contains(":")) {
            s = s.substring(0, s.indexOf(58));
        }

        return s;
    }

    public boolean add(IpBanListEntry p_443240_) {
        if (super.add(p_443240_)) {
            if (p_443240_.getUser() != null) {
                this.notificationService.ipBanned(p_443240_);
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean remove(String p_443137_) {
        if (super.remove(p_443137_)) {
            this.notificationService.ipUnbanned(p_443137_);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        for (IpBanListEntry ipbanlistentry : this.getEntries()) {
            if (ipbanlistentry.getUser() != null) {
                this.notificationService.ipUnbanned(ipbanlistentry.getUser());
            }
        }

        super.clear();
    }
}
