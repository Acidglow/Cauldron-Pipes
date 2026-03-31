package net.minecraft.client.multiplayer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.dialog.DialogConnectionAccess;
import net.minecraft.client.gui.screens.dialog.DialogScreen;
import net.minecraft.client.gui.screens.dialog.DialogScreens;
import net.minecraft.client.gui.screens.dialog.WaitingForResponseScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.telemetry.WorldSessionTelemetryManager;
import net.minecraft.core.Holder;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundClearDialogPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundCustomReportDetailsPacket;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ClientboundServerLinksPacket;
import net.minecraft.network.protocol.common.ClientboundShowDialogPacket;
import net.minecraft.network.protocol.common.ClientboundStoreCookiePacket;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public abstract class ClientCommonPacketListenerImpl implements ClientCommonPacketListener {
    private static final Component GENERIC_DISCONNECT_MESSAGE = Component.translatable("disconnect.lost");
    private static final Logger LOGGER = LogUtils.getLogger();
    protected final Minecraft minecraft;
    protected final Connection connection;
    protected final @Nullable ServerData serverData;
    protected @Nullable String serverBrand;
    protected final WorldSessionTelemetryManager telemetryManager;
    protected final @Nullable Screen postDisconnectScreen;
    protected boolean isTransferring;
    private final List<ClientCommonPacketListenerImpl.DeferredPacket> deferredPackets = new ArrayList<>();
    protected final Map<Identifier, byte[]> serverCookies;
    protected Map<String, String> customReportDetails;
    private ServerLinks serverLinks;
    protected final Map<UUID, PlayerInfo> seenPlayers;
    protected boolean seenInsecureChatWarning;
    /**
     * Holds the current connection type, based on the types of payloads that have been received so far.
     */
    protected net.neoforged.neoforge.network.connection.ConnectionType connectionType = net.neoforged.neoforge.network.connection.ConnectionType.OTHER;

    protected ClientCommonPacketListenerImpl(Minecraft minecraft, Connection connection, CommonListenerCookie commonListenerCookie) {
        this.minecraft = minecraft;
        this.connection = connection;
        this.serverData = commonListenerCookie.serverData();
        this.serverBrand = commonListenerCookie.serverBrand();
        this.telemetryManager = commonListenerCookie.telemetryManager();
        this.postDisconnectScreen = commonListenerCookie.postDisconnectScreen();
        this.serverCookies = commonListenerCookie.serverCookies();
        this.customReportDetails = commonListenerCookie.customReportDetails();
        this.serverLinks = commonListenerCookie.serverLinks();
        this.seenPlayers = new HashMap<>(commonListenerCookie.seenPlayers());
        this.seenInsecureChatWarning = commonListenerCookie.seenInsecureChatWarning();
        // Neo: Set the connection type based on the cookie from the previous phase.
        this.connectionType = commonListenerCookie.connectionType();
    }

    public ServerLinks serverLinks() {
        return this.serverLinks;
    }

    @Override
    public void onPacketError(Packet p_341624_, Exception p_341639_) {
        LOGGER.error("Failed to handle packet {}, disconnecting", p_341624_, p_341639_);
        Optional<Path> optional = this.storeDisconnectionReport(p_341624_, p_341639_);
        Optional<URI> optional1 = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT).map(ServerLinks.Entry::link);
        this.connection.disconnect(new DisconnectionDetails(Component.translatable("disconnect.packetError"), optional, optional1));
    }

    @Override
    public DisconnectionDetails createDisconnectionInfo(Component p_350683_, Throwable p_350813_) {
        Optional<Path> optional = this.storeDisconnectionReport(null, p_350813_);
        Optional<URI> optional1 = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT).map(ServerLinks.Entry::link);
        return new DisconnectionDetails(p_350683_, optional, optional1);
    }

    private Optional<Path> storeDisconnectionReport(@Nullable Packet packet, Throwable error) {
        CrashReport crashreport = CrashReport.forThrowable(error, "Packet handling error");
        PacketUtils.fillCrashReport(crashreport, this, packet);
        Path path = this.minecraft.gameDirectory.toPath().resolve("debug");
        Path path1 = path.resolve("disconnect-" + Util.getFilenameFormattedDateTime() + "-client.txt");
        Optional<ServerLinks.Entry> optional = this.serverLinks.findKnownType(ServerLinks.KnownLinkType.BUG_REPORT);
        List<String> list = optional.<List<String>>map(p_351668_ -> List.of("Server bug reporting link: " + p_351668_.link())).orElse(List.of());
        return crashreport.saveToFile(path1, ReportType.NETWORK_PROTOCOL_ERROR, list) ? Optional.of(path1) : Optional.empty();
    }

    @Override
    public boolean shouldHandleMessage(Packet<?> p_341905_) {
        return ClientCommonPacketListener.super.shouldHandleMessage(p_341905_)
            ? true
            : this.isTransferring && (p_341905_ instanceof ClientboundStoreCookiePacket || p_341905_ instanceof ClientboundTransferPacket);
    }

    @Override
    public void handleKeepAlive(ClientboundKeepAlivePacket p_295361_) {
        this.sendWhen(new ServerboundKeepAlivePacket(p_295361_.getId()), () -> !RenderSystem.isFrozenAtPollEvents(), Duration.ofMinutes(1L));
    }

    @Override
    public void handlePing(ClientboundPingPacket p_295594_) {
        PacketUtils.ensureRunningOnSameThread(p_295594_, this, this.minecraft.packetProcessor());
        this.send(new ServerboundPongPacket(p_295594_.getId()));
    }

    @Override
    public void handleCustomPayload(ClientboundCustomPayloadPacket p_295727_) {
        // Neo: Unconditionally handle register/unregister payloads.
        if (p_295727_.payload() instanceof net.neoforged.neoforge.network.payload.MinecraftRegisterPayload minecraftRegisterPayload) {
            net.neoforged.neoforge.network.registration.NetworkRegistry.onMinecraftRegister(this.getConnection(), minecraftRegisterPayload.newChannels());
            return;
        }

        if (p_295727_.payload() instanceof net.neoforged.neoforge.network.payload.MinecraftUnregisterPayload minecraftUnregisterPayload) {
            net.neoforged.neoforge.network.registration.NetworkRegistry.onMinecraftUnregister(this.getConnection(), minecraftUnregisterPayload.forgottenChannels());
            return;
        }

        if (p_295727_.payload() instanceof net.neoforged.neoforge.network.payload.CommonVersionPayload commonVersionPayload) {
            net.neoforged.neoforge.network.registration.NetworkRegistry.checkCommonVersion(this, commonVersionPayload);
            return;
        }

        if (p_295727_.payload() instanceof net.neoforged.neoforge.network.payload.CommonRegisterPayload commonRegisterPayload) {
            net.neoforged.neoforge.network.registration.NetworkRegistry.onCommonRegister(this, commonRegisterPayload);
            return;
        }

        // Neo: Handle modded payloads. Vanilla payloads do not get sent to the modded handling pass. Additional payloads cannot be registered in the minecraft domain.
        if (net.neoforged.neoforge.network.registration.NetworkRegistry.isModdedPayload(p_295727_.payload())) {
            net.neoforged.neoforge.client.network.registration.ClientNetworkRegistry.handleModdedPayload(this, p_295727_);
            return;
        }

        CustomPacketPayload custompacketpayload = p_295727_.payload();
        if (!(custompacketpayload instanceof DiscardedPayload)) {
            PacketUtils.ensureRunningOnSameThread(p_295727_, this, this.minecraft.packetProcessor());
            if (custompacketpayload instanceof BrandPayload brandpayload) {
                this.serverBrand = brandpayload.brand();
                this.telemetryManager.onServerBrandReceived(brandpayload.brand());
            } else {
                this.handleCustomPayload(custompacketpayload);
            }
        }
    }

    protected abstract void handleCustomPayload(CustomPacketPayload payload);

    @Override
    public void handleResourcePackPush(ClientboundResourcePackPushPacket p_314606_) {
        PacketUtils.ensureRunningOnSameThread(p_314606_, this, this.minecraft.packetProcessor());
        UUID uuid = p_314606_.id();
        URL url = parseResourcePackUrl(p_314606_.url());
        if (url == null) {
            this.connection.send(new ServerboundResourcePackPacket(uuid, ServerboundResourcePackPacket.Action.INVALID_URL));
        } else {
            String s = p_314606_.hash();
            boolean flag = p_314606_.required();
            ServerData.ServerPackStatus serverdata$serverpackstatus = this.serverData != null
                ? this.serverData.getResourcePackStatus()
                : ServerData.ServerPackStatus.PROMPT;
            if (serverdata$serverpackstatus != ServerData.ServerPackStatus.PROMPT
                && (!flag || serverdata$serverpackstatus != ServerData.ServerPackStatus.DISABLED)) {
                this.minecraft.getDownloadedPackSource().pushPack(uuid, url, s);
            } else {
                this.minecraft.setScreen(this.addOrUpdatePackPrompt(uuid, url, s, flag, p_314606_.prompt().orElse(null)));
            }
        }
    }

    @Override
    public void handleResourcePackPop(ClientboundResourcePackPopPacket p_314537_) {
        PacketUtils.ensureRunningOnSameThread(p_314537_, this, this.minecraft.packetProcessor());
        p_314537_.id()
            .ifPresentOrElse(p_314401_ -> this.minecraft.getDownloadedPackSource().popPack(p_314401_), () -> this.minecraft.getDownloadedPackSource().popAll());
    }

    static Component preparePackPrompt(Component line1, @Nullable Component line2) {
        return (Component)(line2 == null ? line1 : Component.translatable("multiplayer.texturePrompt.serverPrompt", line1, line2));
    }

    private static @Nullable URL parseResourcePackUrl(String p_url) {
        try {
            URL url = new URL(p_url);
            String s = url.getProtocol();
            return !"http".equals(s) && !"https".equals(s) ? null : url;
        } catch (MalformedURLException malformedurlexception) {
            return null;
        }
    }

    @Override
    public void handleRequestCookie(ClientboundCookieRequestPacket p_320212_) {
        PacketUtils.ensureRunningOnSameThread(p_320212_, this, this.minecraft.packetProcessor());
        this.connection.send(new ServerboundCookieResponsePacket(p_320212_.key(), this.serverCookies.get(p_320212_.key())));
    }

    @Override
    public void handleStoreCookie(ClientboundStoreCookiePacket p_320008_) {
        PacketUtils.ensureRunningOnSameThread(p_320008_, this, this.minecraft.packetProcessor());
        this.serverCookies.put(p_320008_.key(), p_320008_.payload());
    }

    @Override
    public void handleCustomReportDetails(ClientboundCustomReportDetailsPacket p_350638_) {
        PacketUtils.ensureRunningOnSameThread(p_350638_, this, this.minecraft.packetProcessor());
        this.customReportDetails = p_350638_.details();
    }

    @Override
    public void handleServerLinks(ClientboundServerLinksPacket p_350990_) {
        PacketUtils.ensureRunningOnSameThread(p_350990_, this, this.minecraft.packetProcessor());
        List<ServerLinks.UntrustedEntry> list = p_350990_.links();
        Builder<ServerLinks.Entry> builder = ImmutableList.builderWithExpectedSize(list.size());

        for (ServerLinks.UntrustedEntry serverlinks$untrustedentry : list) {
            try {
                URI uri = Util.parseAndValidateUntrustedUri(serverlinks$untrustedentry.link());
                builder.add(new ServerLinks.Entry(serverlinks$untrustedentry.type(), uri));
            } catch (Exception exception) {
                LOGGER.warn("Received invalid link for type {}:{}", serverlinks$untrustedentry.type(), serverlinks$untrustedentry.link(), exception);
            }
        }

        this.serverLinks = new ServerLinks(builder.build());
    }

    @Override
    public void handleShowDialog(ClientboundShowDialogPacket p_425904_) {
        PacketUtils.ensureRunningOnSameThread(p_425904_, this, this.minecraft.packetProcessor());
        this.showDialog(p_425904_.dialog(), this.minecraft.screen);
    }

    protected abstract DialogConnectionAccess createDialogAccess();

    public void showDialog(Holder<Dialog> dialog, @Nullable Screen previousScreen) {
        this.showDialog(dialog, this.createDialogAccess(), previousScreen);
    }

    protected void showDialog(Holder<Dialog> dialog, DialogConnectionAccess connectionAccess, @Nullable Screen previousScreen) {
        if (previousScreen instanceof DialogScreen.WarningScreen dialogscreen$warningscreen) {
            Screen screen2 = dialogscreen$warningscreen.returnScreen();
            Screen screen3 = screen2 instanceof DialogScreen<?> dialogscreen1 ? dialogscreen1.previousScreen() : screen2;
            DialogScreen<?> dialogscreen2 = DialogScreens.createFromData(dialog.value(), screen3, connectionAccess);
            if (dialogscreen2 != null) {
                dialogscreen$warningscreen.updateReturnScreen(dialogscreen2);
            } else {
                LOGGER.warn("Failed to show dialog for data {}", dialog);
            }
        } else {
            Screen screen;
            if (previousScreen instanceof DialogScreen<?> dialogscreen) {
                screen = dialogscreen.previousScreen();
            } else if (previousScreen instanceof WaitingForResponseScreen waitingforresponsescreen) {
                screen = waitingforresponsescreen.previousScreen();
            } else {
                screen = previousScreen;
            }

            Screen screen1 = DialogScreens.createFromData(dialog.value(), screen, connectionAccess);
            if (screen1 != null) {
                this.minecraft.setScreen(screen1);
            } else {
                LOGGER.warn("Failed to show dialog for data {}", dialog);
            }
        }
    }

    @Override
    public void handleClearDialog(ClientboundClearDialogPacket p_426184_) {
        PacketUtils.ensureRunningOnSameThread(p_426184_, this, this.minecraft.packetProcessor());
        this.clearDialog();
    }

    public void clearDialog() {
        if (this.minecraft.screen instanceof DialogScreen.WarningScreen dialogscreen$warningscreen) {
            if (dialogscreen$warningscreen.returnScreen() instanceof DialogScreen<?> dialogscreen1) {
                dialogscreen$warningscreen.updateReturnScreen(dialogscreen1.previousScreen());
            }
        } else if (this.minecraft.screen instanceof DialogScreen<?> dialogscreen) {
            this.minecraft.setScreen(dialogscreen.previousScreen());
        }
    }

    @Override
    public void handleTransfer(ClientboundTransferPacket p_320739_) {
        this.isTransferring = true;
        PacketUtils.ensureRunningOnSameThread(p_320739_, this, this.minecraft.packetProcessor());
        if (this.serverData == null) {
            throw new IllegalStateException("Cannot transfer to server from singleplayer");
        } else {
            this.connection.disconnect(Component.translatable("disconnect.transfer"));
            this.connection.setReadOnly();
            this.connection.handleDisconnection();
            ServerAddress serveraddress = new ServerAddress(p_320739_.host(), p_320739_.port());
            ConnectScreen.startConnecting(
                Objects.requireNonNullElseGet(this.postDisconnectScreen, TitleScreen::new),
                this.minecraft,
                serveraddress,
                this.serverData,
                false,
                new TransferState(this.serverCookies, this.seenPlayers, this.seenInsecureChatWarning)
            );
        }
    }

    @Override
    public void handleDisconnect(ClientboundDisconnectPacket p_296159_) {
        this.connection.disconnect(p_296159_.reason());
    }

    protected void sendDeferredPackets() {
        Iterator<ClientCommonPacketListenerImpl.DeferredPacket> iterator = this.deferredPackets.iterator();

        while (iterator.hasNext()) {
            ClientCommonPacketListenerImpl.DeferredPacket clientcommonpacketlistenerimpl$deferredpacket = iterator.next();
            if (clientcommonpacketlistenerimpl$deferredpacket.sendCondition().getAsBoolean()) {
                this.send(clientcommonpacketlistenerimpl$deferredpacket.packet);
                iterator.remove();
            } else if (clientcommonpacketlistenerimpl$deferredpacket.expirationTime() <= Util.getMillis()) {
                iterator.remove();
            }
        }
    }

    public void send(Packet<?> packet) {
        // Neo: Validate modded payloads before sending.
        net.neoforged.neoforge.network.registration.NetworkRegistry.checkPacket(packet, this);
        this.connection.send(packet);
    }

    @Override
    public void onDisconnect(DisconnectionDetails p_350760_) {
        this.telemetryManager.onDisconnect();
        this.minecraft.disconnect(this.createDisconnectScreen(p_350760_), this.isTransferring);
        LOGGER.warn("Client disconnected with reason: {}", p_350760_.reason().getString());
    }

    @Override
    public void fillListenerSpecificCrashDetails(CrashReport p_350364_, CrashReportCategory p_315011_) {
        p_315011_.setDetail("Is Local", () -> String.valueOf(this.connection.isMemoryConnection()));
        p_315011_.setDetail("Server type", () -> this.serverData != null ? this.serverData.type().toString() : "<none>");
        p_315011_.setDetail("Server brand", () -> this.serverBrand);
        if (!this.customReportDetails.isEmpty()) {
            CrashReportCategory crashreportcategory = p_350364_.addCategory("Custom Server Details");
            this.customReportDetails.forEach(crashreportcategory::setDetail);
        }
    }

    protected Screen createDisconnectScreen(DisconnectionDetails details) {
        Screen screen = Objects.requireNonNullElseGet(
            this.postDisconnectScreen, () -> (Screen)(this.serverData != null ? new JoinMultiplayerScreen(new TitleScreen()) : new TitleScreen())
        );
        return this.serverData != null && this.serverData.isRealm()
            ? new DisconnectedScreen(screen, GENERIC_DISCONNECT_MESSAGE, details, CommonComponents.GUI_BACK)
            : new DisconnectedScreen(screen, GENERIC_DISCONNECT_MESSAGE, details);
    }

    public @Nullable String serverBrand() {
        return this.serverBrand;
    }

    private void sendWhen(Packet<? extends ServerboundPacketListener> packet, BooleanSupplier sendCondition, Duration expirationTime) {
        if (sendCondition.getAsBoolean()) {
            this.send(packet);
        } else {
            this.deferredPackets.add(new ClientCommonPacketListenerImpl.DeferredPacket(packet, sendCondition, Util.getMillis() + expirationTime.toMillis()));
        }
    }

    private Screen addOrUpdatePackPrompt(UUID id, URL url, String hash, boolean required, @Nullable Component prompt) {
        Screen screen = this.minecraft.screen;
        return screen instanceof ClientCommonPacketListenerImpl.PackConfirmScreen clientcommonpacketlistenerimpl$packconfirmscreen
            ? clientcommonpacketlistenerimpl$packconfirmscreen.update(this.minecraft, id, url, hash, required, prompt)
            : new ClientCommonPacketListenerImpl.PackConfirmScreen(
                this.minecraft,
                screen,
                List.of(new ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest(id, url, hash)),
                required,
                prompt
            );
    }

    @OnlyIn(Dist.CLIENT)
    protected abstract class CommonDialogAccess implements DialogConnectionAccess {
        @Override
        public void disconnect(Component p_451055_) {
            ClientCommonPacketListenerImpl.this.connection.disconnect(p_451055_);
            ClientCommonPacketListenerImpl.this.connection.handleDisconnection();
        }

        @Override
        public void openDialog(Holder<Dialog> p_437297_, @Nullable Screen p_437186_) {
            ClientCommonPacketListenerImpl.this.showDialog(p_437297_, this, p_437186_);
        }

        @Override
        public void sendCustomAction(Identifier p_467437_, Optional<Tag> p_437224_) {
            ClientCommonPacketListenerImpl.this.send(new ServerboundCustomClickActionPacket(p_467437_, p_437224_));
        }

        @Override
        public ServerLinks serverLinks() {
            return ClientCommonPacketListenerImpl.this.serverLinks();
        }
    }

    @OnlyIn(Dist.CLIENT)
    record DeferredPacket(Packet<? extends ServerboundPacketListener> packet, BooleanSupplier sendCondition, long expirationTime) {
    }

    @OnlyIn(Dist.CLIENT)
    class PackConfirmScreen extends ConfirmScreen {
        private final List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> requests;
        private final @Nullable Screen parentScreen;

        PackConfirmScreen(
            Minecraft minecraft,
            @Nullable Screen parentScreen,
            List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> requests,
            boolean required,
            @Nullable Component prompt
        ) {
            super(
                p_315005_ -> {
                    minecraft.setScreen(parentScreen);
                    DownloadedPackSource downloadedpacksource = minecraft.getDownloadedPackSource();
                    if (p_315005_) {
                        if (ClientCommonPacketListenerImpl.this.serverData != null) {
                            ClientCommonPacketListenerImpl.this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
                        }

                        downloadedpacksource.allowServerPacks();
                    } else {
                        downloadedpacksource.rejectServerPacks();
                        if (required) {
                            ClientCommonPacketListenerImpl.this.connection.disconnect(Component.translatable("multiplayer.requiredTexturePrompt.disconnect"));
                        } else if (ClientCommonPacketListenerImpl.this.serverData != null) {
                            ClientCommonPacketListenerImpl.this.serverData.setResourcePackStatus(ServerData.ServerPackStatus.DISABLED);
                        }
                    }

                    for (ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest : requests) {
                        downloadedpacksource.pushPack(
                            clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest.id,
                            clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest.url,
                            clientcommonpacketlistenerimpl$packconfirmscreen$pendingrequest.hash
                        );
                    }

                    if (ClientCommonPacketListenerImpl.this.serverData != null) {
                        ServerList.saveSingleServer(ClientCommonPacketListenerImpl.this.serverData);
                    }
                },
                required ? Component.translatable("multiplayer.requiredTexturePrompt.line1") : Component.translatable("multiplayer.texturePrompt.line1"),
                ClientCommonPacketListenerImpl.preparePackPrompt(
                    required
                        ? Component.translatable("multiplayer.requiredTexturePrompt.line2").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD)
                        : Component.translatable("multiplayer.texturePrompt.line2"),
                    prompt
                ),
                required ? CommonComponents.GUI_PROCEED : CommonComponents.GUI_YES,
                required ? CommonComponents.GUI_DISCONNECT : CommonComponents.GUI_NO
            );
            this.requests = requests;
            this.parentScreen = parentScreen;
        }

        public ClientCommonPacketListenerImpl.PackConfirmScreen update(
            Minecraft minecraft, UUID id, URL url, String hash, boolean required, @Nullable Component prompt
        ) {
            List<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest> list = ImmutableList.<ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest>builderWithExpectedSize(
                    this.requests.size() + 1
                )
                .addAll(this.requests)
                .add(new ClientCommonPacketListenerImpl.PackConfirmScreen.PendingRequest(id, url, hash))
                .build();
            return ClientCommonPacketListenerImpl.this.new PackConfirmScreen(minecraft, this.parentScreen, list, required, prompt);
        }

        @OnlyIn(Dist.CLIENT)
        record PendingRequest(UUID id, URL url, String hash) {
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public net.minecraft.network.PacketProcessor getPacketProcessor() {
        return this.minecraft.packetProcessor();
    }
}
