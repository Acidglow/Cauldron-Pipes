package com.mojang.realmsclient.gui.screens;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.PendingInvite;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.RealmsDataFetcher;
import com.mojang.realmsclient.util.RealmsUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.realms.RealmsScreen;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsPendingInvitesScreen extends RealmsScreen {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final Component NO_PENDING_INVITES_TEXT = Component.translatable("mco.invites.nopending");
    private final Screen lastScreen;
    private final CompletableFuture<List<PendingInvite>> pendingInvites = CompletableFuture.supplyAsync(() -> {
        try {
            return RealmsClient.getOrCreate().pendingInvites().pendingInvites();
        } catch (RealmsServiceException realmsserviceexception) {
            LOGGER.error("Couldn't list invites", (Throwable)realmsserviceexception);
            return List.of();
        }
    }, Util.ioPool());
    final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    RealmsPendingInvitesScreen.@Nullable PendingInvitationSelectionList pendingInvitationSelectionList;

    public RealmsPendingInvitesScreen(Screen lastScreen, Component title) {
        super(title);
        this.lastScreen = lastScreen;
    }

    @Override
    public void init() {
        RealmsMainScreen.refreshPendingInvites();
        this.layout.addTitleHeader(this.title, this.font);
        this.pendingInvitationSelectionList = this.layout.addToContents(new RealmsPendingInvitesScreen.PendingInvitationSelectionList(this.minecraft));
        this.pendingInvites.thenAcceptAsync(p_414875_ -> {
            List<RealmsPendingInvitesScreen.Entry> list = p_414875_.stream().map(p_293579_ -> new RealmsPendingInvitesScreen.Entry(p_293579_)).toList();
            this.pendingInvitationSelectionList.replaceEntries(list);
            if (list.isEmpty()) {
                this.minecraft.getNarrator().saySystemQueued(NO_PENDING_INVITES_TEXT);
            }
        }, this.screenExecutor);
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, p_293580_ -> this.onClose()).width(200).build());
        this.layout.visitWidgets(p_438700_ -> {
            AbstractWidget abstractwidget = this.addRenderableWidget(p_438700_);
        });
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.pendingInvitationSelectionList != null) {
            this.pendingInvitationSelectionList.updateSize(this.width, this.layout);
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.lastScreen);
    }

    @Override
    public void render(GuiGraphics p_282787_, int p_88900_, int p_88901_, float p_88902_) {
        super.render(p_282787_, p_88900_, p_88901_, p_88902_);
        if (this.pendingInvites.isDone() && this.pendingInvitationSelectionList.hasPendingInvites()) {
            p_282787_.drawCenteredString(this.font, NO_PENDING_INVITES_TEXT, this.width / 2, this.height / 2 - 20, -1);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class Entry extends ContainerObjectSelectionList.Entry<RealmsPendingInvitesScreen.Entry> {
        private static final Component ACCEPT_INVITE = Component.translatable("mco.invites.button.accept");
        private static final Component REJECT_INVITE = Component.translatable("mco.invites.button.reject");
        private static final WidgetSprites ACCEPT_SPRITE = new WidgetSprites(
            Identifier.withDefaultNamespace("pending_invite/accept"), Identifier.withDefaultNamespace("pending_invite/accept_highlighted")
        );
        private static final WidgetSprites REJECT_SPRITE = new WidgetSprites(
            Identifier.withDefaultNamespace("pending_invite/reject"), Identifier.withDefaultNamespace("pending_invite/reject_highlighted")
        );
        private static final int SPRITE_TEXTURE_SIZE = 18;
        private static final int SPRITE_SIZE = 21;
        private static final int TEXT_LEFT = 38;
        private final PendingInvite pendingInvite;
        private final List<AbstractWidget> children = new ArrayList<>();
        private final SpriteIconButton acceptButton;
        private final SpriteIconButton rejectButton;
        private final StringWidget realmName;
        private final StringWidget realmOwnerName;
        private final StringWidget inviteDate;

        Entry(PendingInvite pendingInvite) {
            this.pendingInvite = pendingInvite;
            int i = RealmsPendingInvitesScreen.this.pendingInvitationSelectionList.getRowWidth() - 32 - 32 - 42;
            this.realmName = new StringWidget(Component.literal(pendingInvite.realmName()), RealmsPendingInvitesScreen.this.font).setMaxWidth(i);
            this.realmOwnerName = new StringWidget(Component.literal(pendingInvite.realmOwnerName()).withColor(-6250336), RealmsPendingInvitesScreen.this.font)
                .setMaxWidth(i);
            this.inviteDate = new StringWidget(
                    ComponentUtils.mergeStyles(RealmsUtil.convertToAgePresentationFromInstant(pendingInvite.date()), Style.EMPTY.withColor(-6250336)),
                    RealmsPendingInvitesScreen.this.font
                )
                .setMaxWidth(i);
            Button.CreateNarration button$createnarration = this.getCreateNarration(pendingInvite);
            this.acceptButton = SpriteIconButton.builder(ACCEPT_INVITE, p_440643_ -> this.handleInvitation(true), false)
                .sprite(ACCEPT_SPRITE, 18, 18)
                .size(21, 21)
                .narration(button$createnarration)
                .withTootip()
                .build();
            this.rejectButton = SpriteIconButton.builder(REJECT_INVITE, p_440243_ -> this.handleInvitation(false), false)
                .sprite(REJECT_SPRITE, 18, 18)
                .size(21, 21)
                .narration(button$createnarration)
                .withTootip()
                .build();
            this.children.addAll(List.of(this.acceptButton, this.rejectButton));
        }

        private Button.CreateNarration getCreateNarration(PendingInvite invite) {
            return p_458795_ -> {
                MutableComponent mutablecomponent = CommonComponents.joinForNarration(
                    p_458795_.get(),
                    Component.literal(invite.realmName()),
                    Component.literal(invite.realmOwnerName()),
                    RealmsUtil.convertToAgePresentationFromInstant(invite.date())
                );
                return Component.translatable("narrator.select", mutablecomponent);
            };
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        @Override
        public void renderContent(GuiGraphics p_439597_, int p_439407_, int p_439905_, boolean p_439814_, float p_440283_) {
            int i = this.getContentX();
            int j = this.getContentY();
            int k = i + 38;
            RealmsUtil.renderPlayerFace(p_439597_, i, j, 32, this.pendingInvite.realmOwnerUuid());
            this.realmName.setPosition(k, j + 1);
            this.realmName.renderWidget(p_439597_, p_439407_, p_439905_, i);
            this.realmOwnerName.setPosition(k, j + 12);
            this.realmOwnerName.renderWidget(p_439597_, p_439407_, p_439905_, i);
            this.inviteDate.setPosition(k, j + 24);
            this.inviteDate.renderWidget(p_439597_, p_439407_, p_439905_, i);
            int l = j + this.getContentHeight() / 2 - 10;
            this.acceptButton.setPosition(i + this.getContentWidth() - 16 - 42, l);
            this.acceptButton.render(p_439597_, p_439407_, p_439905_, p_440283_);
            this.rejectButton.setPosition(i + this.getContentWidth() - 8 - 21, l);
            this.rejectButton.render(p_439597_, p_439407_, p_439905_, p_440283_);
        }

        private void handleInvitation(boolean accept) {
            String s = this.pendingInvite.invitationId();
            CompletableFuture.<Boolean>supplyAsync(() -> {
                try {
                    RealmsClient realmsclient = RealmsClient.getOrCreate();
                    if (accept) {
                        realmsclient.acceptInvitation(s);
                    } else {
                        realmsclient.rejectInvitation(s);
                    }

                    return true;
                } catch (RealmsServiceException realmsserviceexception) {
                    RealmsPendingInvitesScreen.LOGGER.error("Couldn't handle invite", (Throwable)realmsserviceexception);
                    return false;
                }
            }, Util.ioPool()).thenAcceptAsync(p_440127_ -> {
                if (p_440127_) {
                    RealmsPendingInvitesScreen.this.pendingInvitationSelectionList.removeInvitation(this);
                    RealmsDataFetcher realmsdatafetcher = RealmsPendingInvitesScreen.this.minecraft.realmsDataFetcher();
                    if (accept) {
                        realmsdatafetcher.serverListUpdateTask.reset();
                    }

                    realmsdatafetcher.pendingInvitesTask.reset();
                }
            }, RealmsPendingInvitesScreen.this.screenExecutor);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class PendingInvitationSelectionList extends ContainerObjectSelectionList<RealmsPendingInvitesScreen.Entry> {
        public static final int ITEM_HEIGHT = 36;

        public PendingInvitationSelectionList(Minecraft minecraft) {
            super(
                minecraft,
                RealmsPendingInvitesScreen.this.width,
                RealmsPendingInvitesScreen.this.layout.getContentHeight(),
                RealmsPendingInvitesScreen.this.layout.getHeaderHeight(),
                36
            );
        }

        @Override
        public int getRowWidth() {
            return 280;
        }

        public boolean hasPendingInvites() {
            return this.getItemCount() == 0;
        }

        public void removeInvitation(RealmsPendingInvitesScreen.Entry entry) {
            this.removeEntry(entry);
        }
    }
}
