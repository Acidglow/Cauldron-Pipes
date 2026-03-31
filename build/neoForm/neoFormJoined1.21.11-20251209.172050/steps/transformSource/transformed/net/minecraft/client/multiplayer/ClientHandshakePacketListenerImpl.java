package net.minecraft.client.multiplayer;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.ForcedUsernameChangeException;
import com.mojang.authlib.exceptions.InsufficientPrivilegesException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserBannedException;
import com.mojang.logging.LogUtils;
import java.math.BigInteger;
import java.security.PublicKey;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.configuration.ConfigurationProtocols;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginFinishedPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.ServerLinks;
import net.minecraft.util.Crypt;
import net.minecraft.util.Util;
import net.minecraft.world.flag.FeatureFlags;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientHandshakePacketListenerImpl implements ClientLoginPacketListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Minecraft minecraft;
    private final @Nullable ServerData serverData;
    private final @Nullable Screen parent;
    private final Consumer<Component> updateStatus;
    private final Connection connection;
    private final boolean newWorld;
    private final @Nullable Duration worldLoadDuration;
    private @Nullable String minigameName;
    private final LevelLoadTracker levelLoadTracker;
    private final Map<Identifier, byte[]> cookies;
    private final boolean wasTransferredTo;
    private final Map<UUID, PlayerInfo> seenPlayers;
    private final boolean seenInsecureChatWarning;
    private final AtomicReference<ClientHandshakePacketListenerImpl.State> state = new AtomicReference<>(ClientHandshakePacketListenerImpl.State.CONNECTING);

    public ClientHandshakePacketListenerImpl(
        Connection connection,
        Minecraft minecraft,
        @Nullable ServerData serverData,
        @Nullable Screen parent,
        boolean newWorld,
        @Nullable Duration worldLoadDuration,
        Consumer<Component> updateStatus,
        LevelLoadTracker levelLoadTracker,
        @Nullable TransferState transferState
    ) {
        this.connection = connection;
        this.minecraft = minecraft;
        this.serverData = serverData;
        this.parent = parent;
        this.updateStatus = updateStatus;
        this.newWorld = newWorld;
        this.worldLoadDuration = worldLoadDuration;
        this.levelLoadTracker = levelLoadTracker;
        this.cookies = transferState != null ? new HashMap<>(transferState.cookies()) : new HashMap<>();
        this.seenPlayers = transferState != null ? transferState.seenPlayers() : Map.of();
        this.seenInsecureChatWarning = transferState != null ? transferState.seenInsecureChatWarning() : false;
        this.wasTransferredTo = transferState != null;
    }

    private void switchState(ClientHandshakePacketListenerImpl.State state) {
        ClientHandshakePacketListenerImpl.State clienthandshakepacketlistenerimpl$state = this.state.updateAndGet(p_339288_ -> {
            if (!state.fromStates.contains(p_339288_)) {
                throw new IllegalStateException("Tried to switch to " + state + " from " + p_339288_ + ", but expected one of " + state.fromStates);
            } else {
                return state;
            }
        });
        this.updateStatus.accept(clienthandshakepacketlistenerimpl$state.message);
    }

    @Override
    public void handleHello(ClientboundHelloPacket packet) {
        this.switchState(ClientHandshakePacketListenerImpl.State.AUTHORIZING);

        Cipher cipher;
        Cipher cipher1;
        String s;
        ServerboundKeyPacket serverboundkeypacket;
        try {
            SecretKey secretkey = Crypt.generateSecretKey();
            PublicKey publickey = packet.getPublicKey();
            s = new BigInteger(Crypt.digestData(packet.getServerId(), publickey, secretkey)).toString(16);
            cipher = Crypt.getCipher(2, secretkey);
            cipher1 = Crypt.getCipher(1, secretkey);
            byte[] abyte = packet.getChallenge();
            serverboundkeypacket = new ServerboundKeyPacket(secretkey, publickey, abyte);
        } catch (Exception exception) {
            throw new IllegalStateException("Protocol error", exception);
        }

        if (packet.shouldAuthenticate()) {
            Util.ioPool().execute(() -> {
                Component component = this.authenticateServer(s);
                if (component != null) {
                    if (this.serverData == null || !this.serverData.isLan()) {
                        this.connection.disconnect(component);
                        return;
                    }

                    LOGGER.warn(component.getString());
                }

                this.setEncryption(serverboundkeypacket, cipher, cipher1);
            });
        } else {
            this.setEncryption(serverboundkeypacket, cipher, cipher1);
        }
    }

    private void setEncryption(ServerboundKeyPacket keyPacket, Cipher decryptingCypher, Cipher encryptingCypher) {
        this.switchState(ClientHandshakePacketListenerImpl.State.ENCRYPTING);
        this.connection.send(keyPacket, PacketSendListener.thenRun(() -> this.connection.setEncryptionKey(decryptingCypher, encryptingCypher)));
    }

    private @Nullable Component authenticateServer(String serverHash) {
        try {
            this.minecraft
                .services()
                .sessionService()
                .joinServer(this.minecraft.getUser().getProfileId(), this.minecraft.getUser().getAccessToken(), serverHash);
            return null;
        } catch (AuthenticationUnavailableException authenticationunavailableexception) {
            return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.serversUnavailable"));
        } catch (InvalidCredentialsException invalidcredentialsexception) {
            return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.invalidSession"));
        } catch (InsufficientPrivilegesException insufficientprivilegesexception) {
            return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.insufficientPrivileges"));
        } catch (ForcedUsernameChangeException | UserBannedException userbannedexception) {
            return Component.translatable("disconnect.loginFailedInfo", Component.translatable("disconnect.loginFailedInfo.userBanned"));
        } catch (AuthenticationException authenticationexception) {
            return Component.translatable("disconnect.loginFailedInfo", authenticationexception.getMessage());
        }
    }

    @Override
    public void handleLoginFinished(ClientboundLoginFinishedPacket p_374132_) {
        this.switchState(ClientHandshakePacketListenerImpl.State.JOINING);
        GameProfile gameprofile = p_374132_.gameProfile();
        this.connection
            .setupInboundProtocol(
                ConfigurationProtocols.CLIENTBOUND,
                new ClientConfigurationPacketListenerImpl(
                    this.minecraft,
                    this.connection,
                    new CommonListenerCookie(
                        this.levelLoadTracker,
                        gameprofile,
                        this.minecraft.getTelemetryManager().createWorldSessionManager(this.newWorld, this.worldLoadDuration, this.minigameName),
                        ClientRegistryLayer.createRegistryAccess().compositeAccess(),
                        FeatureFlags.DEFAULT_FLAGS,
                        null,
                        this.serverData,
                        this.parent,
                        this.cookies,
                        null,
                        Map.of(),
                        ServerLinks.EMPTY,
                        this.seenPlayers,
                        false
                    )
                )
            );
        this.connection.send(ServerboundLoginAcknowledgedPacket.INSTANCE);
        this.connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
        this.connection.send(new ServerboundCustomPayloadPacket(new BrandPayload(ClientBrandRetriever.getClientModName())));
        this.connection.send(new ServerboundClientInformationPacket(this.minecraft.options.buildPlayerInformation()));
    }

    @Override
    public void onDisconnect(DisconnectionDetails p_350923_) {
        Component component = this.wasTransferredTo ? CommonComponents.TRANSFER_CONNECT_FAILED : CommonComponents.CONNECT_FAILED;
        if (this.serverData != null && this.serverData.isRealm()) {
            this.minecraft.setScreen(new DisconnectedScreen(this.parent, component, p_350923_.reason(), CommonComponents.GUI_BACK));
        } else {
            this.minecraft.setScreen(new DisconnectedScreen(this.parent, component, p_350923_));
        }
    }

    @Override
    public boolean isAcceptingMessages() {
        return this.connection.isConnected();
    }

    @Override
    public void handleDisconnect(ClientboundLoginDisconnectPacket packet) {
        this.connection.disconnect(packet.reason());
    }

    @Override
    public void handleCompression(ClientboundLoginCompressionPacket packet) {
        if (!this.connection.isMemoryConnection()) {
            this.connection.setupCompression(packet.getCompressionThreshold(), false);
        }
    }

    @Override
    public void handleCustomQuery(ClientboundCustomQueryPacket packet) {
        this.updateStatus.accept(Component.translatable("connect.negotiating"));
        this.connection.send(new ServerboundCustomQueryAnswerPacket(packet.transactionId(), null));
    }

    public void setMinigameName(@Nullable String minigameName) {
        this.minigameName = minigameName;
    }

    @Override
    public void handleRequestCookie(ClientboundCookieRequestPacket p_319938_) {
        this.connection.send(new ServerboundCookieResponsePacket(p_319938_.key(), this.cookies.get(p_319938_.key())));
    }

    @Override
    public void fillListenerSpecificCrashDetails(CrashReport p_350555_, CrashReportCategory p_315015_) {
        p_315015_.setDetail("Server type", () -> this.serverData != null ? this.serverData.type().toString() : "<unknown>");
        p_315015_.setDetail("Login phase", () -> this.state.get().toString());
        p_315015_.setDetail("Is Local", () -> String.valueOf(this.connection.isMemoryConnection()));
    }

    @OnlyIn(Dist.CLIENT)
    static enum State {
        CONNECTING(Component.translatable("connect.connecting"), Set.of()),
        AUTHORIZING(Component.translatable("connect.authorizing"), Set.of(CONNECTING)),
        ENCRYPTING(Component.translatable("connect.encrypting"), Set.of(AUTHORIZING)),
        JOINING(Component.translatable("connect.joining"), Set.of(ENCRYPTING, CONNECTING));

        final Component message;
        final Set<ClientHandshakePacketListenerImpl.State> fromStates;

        private State(Component message, Set<ClientHandshakePacketListenerImpl.State> fromStates) {
            this.message = message;
            this.fromStates = fromStates;
        }
    }
}
