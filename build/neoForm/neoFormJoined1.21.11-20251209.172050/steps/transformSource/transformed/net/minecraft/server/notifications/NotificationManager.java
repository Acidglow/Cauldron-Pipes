package net.minecraft.server.notifications;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.ServerOpListEntry;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.world.level.gamerules.GameRule;

public class NotificationManager implements NotificationService {
    private final List<NotificationService> notificationServices = Lists.newArrayList();

    public void registerService(NotificationService service) {
        this.notificationServices.add(service);
    }

    @Override
    public void playerJoined(ServerPlayer p_442825_) {
        this.notificationServices.forEach(p_443382_ -> p_443382_.playerJoined(p_442825_));
    }

    @Override
    public void playerLeft(ServerPlayer p_442626_) {
        this.notificationServices.forEach(p_443011_ -> p_443011_.playerLeft(p_442626_));
    }

    @Override
    public void serverStarted() {
        this.notificationServices.forEach(NotificationService::serverStarted);
    }

    @Override
    public void serverShuttingDown() {
        this.notificationServices.forEach(NotificationService::serverShuttingDown);
    }

    @Override
    public void serverSaveStarted() {
        this.notificationServices.forEach(NotificationService::serverSaveStarted);
    }

    @Override
    public void serverSaveCompleted() {
        this.notificationServices.forEach(NotificationService::serverSaveCompleted);
    }

    @Override
    public void serverActivityOccured() {
        this.notificationServices.forEach(NotificationService::serverActivityOccured);
    }

    @Override
    public void playerOped(ServerOpListEntry p_442940_) {
        this.notificationServices.forEach(p_443522_ -> p_443522_.playerOped(p_442940_));
    }

    @Override
    public void playerDeoped(ServerOpListEntry p_443315_) {
        this.notificationServices.forEach(p_442984_ -> p_442984_.playerDeoped(p_443315_));
    }

    @Override
    public void playerAddedToAllowlist(NameAndId p_442558_) {
        this.notificationServices.forEach(p_442575_ -> p_442575_.playerAddedToAllowlist(p_442558_));
    }

    @Override
    public void playerRemovedFromAllowlist(NameAndId p_443493_) {
        this.notificationServices.forEach(p_443062_ -> p_443062_.playerRemovedFromAllowlist(p_443493_));
    }

    @Override
    public void ipBanned(IpBanListEntry p_442915_) {
        this.notificationServices.forEach(p_443574_ -> p_443574_.ipBanned(p_442915_));
    }

    @Override
    public void ipUnbanned(String p_443008_) {
        this.notificationServices.forEach(p_443156_ -> p_443156_.ipUnbanned(p_443008_));
    }

    @Override
    public void playerBanned(UserBanListEntry p_442723_) {
        this.notificationServices.forEach(p_442822_ -> p_442822_.playerBanned(p_442723_));
    }

    @Override
    public void playerUnbanned(NameAndId p_442721_) {
        this.notificationServices.forEach(p_442676_ -> p_442676_.playerUnbanned(p_442721_));
    }

    @Override
    public <T> void onGameRuleChanged(GameRule<T> p_461031_, T p_460980_) {
        this.notificationServices.forEach(p_460347_ -> p_460347_.onGameRuleChanged(p_461031_, p_460980_));
    }

    @Override
    public void statusHeartbeat() {
        this.notificationServices.forEach(NotificationService::statusHeartbeat);
    }
}
