package com.mojang.realmsclient.gui.screens.configuration;

import com.google.common.collect.ImmutableList;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.client.RealmsClient;
import com.mojang.realmsclient.dto.Ops;
import com.mojang.realmsclient.dto.PlayerInfo;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.exception.RealmsServiceException;
import com.mojang.realmsclient.gui.screens.RealmsConfirmScreen;
import com.mojang.realmsclient.util.RealmsUtil;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.FocusableTextWidget;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
class RealmsPlayersTab extends GridLayoutTab implements RealmsConfigurationTab {
    static final Logger LOGGER = LogUtils.getLogger();
    static final Component TITLE = Component.translatable("mco.configure.world.players.title");
    static final Component QUESTION_TITLE = Component.translatable("mco.question");
    private static final int PADDING = 8;
    final RealmsConfigureWorldScreen configurationScreen;
    final Minecraft minecraft;
    final Font font;
    RealmsServer serverData;
    final RealmsPlayersTab.InvitedObjectSelectionList invitedList;

    RealmsPlayersTab(RealmsConfigureWorldScreen configurationScreen, Minecraft minecraft, RealmsServer serverData) {
        super(TITLE);
        this.configurationScreen = configurationScreen;
        this.minecraft = minecraft;
        this.font = configurationScreen.getFont();
        this.serverData = serverData;
        GridLayout.RowHelper gridlayout$rowhelper = this.layout.spacing(8).createRowHelper(1);
        this.invitedList = gridlayout$rowhelper.addChild(
            new RealmsPlayersTab.InvitedObjectSelectionList(configurationScreen.width, this.calculateListHeight()),
            LayoutSettings.defaults().alignVerticallyTop().alignHorizontallyCenter()
        );
        gridlayout$rowhelper.addChild(
            Button.builder(
                    Component.translatable("mco.configure.world.buttons.invite"),
                    p_419740_ -> minecraft.setScreen(new RealmsInviteScreen(configurationScreen, serverData))
                )
                .build(),
            LayoutSettings.defaults().alignVerticallyBottom().alignHorizontallyCenter()
        );
        this.updateData(serverData);
    }

    public int calculateListHeight() {
        return this.configurationScreen.getContentHeight() - 20 - 16;
    }

    @Override
    public void doLayout(ScreenRectangle p_419767_) {
        this.invitedList.updateSizeAndPosition(this.configurationScreen.width, this.calculateListHeight(), this.configurationScreen.layout.getHeaderHeight());
        super.doLayout(p_419767_);
    }

    @Override
    public void updateData(RealmsServer p_419463_) {
        this.serverData = p_419463_;
        this.invitedList.updateList(p_419463_);
    }

    @OnlyIn(Dist.CLIENT)
    abstract static class Entry extends ContainerObjectSelectionList.Entry<RealmsPlayersTab.Entry> {
    }

    @OnlyIn(Dist.CLIENT)
    class HeaderEntry extends RealmsPlayersTab.Entry {
        private String cachedNumberOfInvites = "";
        private final FocusableTextWidget invitedWidget;

        public HeaderEntry() {
            Component component = Component.translatable("mco.configure.world.invited.number", "").withStyle(ChatFormatting.UNDERLINE);
            this.invitedWidget = FocusableTextWidget.builder(component, RealmsPlayersTab.this.font)
                .alwaysShowBorder(false)
                .backgroundFill(FocusableTextWidget.BackgroundFill.ON_FOCUS)
                .build();
        }

        @Override
        public void renderContent(GuiGraphics p_440278_, int p_440194_, int p_440165_, boolean p_440275_, float p_439884_) {
            String s = RealmsPlayersTab.this.serverData.players != null ? Integer.toString(RealmsPlayersTab.this.serverData.players.size()) : "0";
            if (!s.equals(this.cachedNumberOfInvites)) {
                this.cachedNumberOfInvites = s;
                Component component = Component.translatable("mco.configure.world.invited.number", s).withStyle(ChatFormatting.UNDERLINE);
                this.invitedWidget.setMessage(component);
            }

            this.invitedWidget
                .setPosition(
                    RealmsPlayersTab.this.invitedList.getRowLeft() + RealmsPlayersTab.this.invitedList.getRowWidth() / 2 - this.invitedWidget.getWidth() / 2,
                    this.getY() + this.getHeight() / 2 - this.invitedWidget.getHeight() / 2
                );
            this.invitedWidget.render(p_440278_, p_440194_, p_440165_, p_439884_);
        }

        int height(int height) {
            return height + this.invitedWidget.getPadding() * 2;
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(this.invitedWidget);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(this.invitedWidget);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class InvitedObjectSelectionList extends ContainerObjectSelectionList<RealmsPlayersTab.Entry> {
        private static final int PLAYER_ENTRY_HEIGHT = 36;

        public InvitedObjectSelectionList(int width, int height) {
            super(Minecraft.getInstance(), width, height, RealmsPlayersTab.this.configurationScreen.getHeaderHeight(), 36);
        }

        void updateList(RealmsServer server) {
            this.clearEntries();
            this.populateList(server);
        }

        private void populateList(RealmsServer server) {
            RealmsPlayersTab.HeaderEntry realmsplayerstab$headerentry = RealmsPlayersTab.this.new HeaderEntry();
            this.addEntry(realmsplayerstab$headerentry, realmsplayerstab$headerentry.height(9));

            for (RealmsPlayersTab.PlayerEntry realmsplayerstab$playerentry : server.players
                .stream()
                .map(p_439372_ -> RealmsPlayersTab.this.new PlayerEntry(p_439372_))
                .toList()) {
                this.addEntry(realmsplayerstab$playerentry);
            }
        }

        @Override
        protected void renderListBackground(GuiGraphics p_419974_) {
        }

        @Override
        protected void renderListSeparators(GuiGraphics p_419993_) {
        }

        @Override
        public int getRowWidth() {
            return 300;
        }
    }

    @OnlyIn(Dist.CLIENT)
    class PlayerEntry extends RealmsPlayersTab.Entry {
        protected static final int SKIN_FACE_SIZE = 32;
        private static final Component NORMAL_USER_TEXT = Component.translatable("mco.configure.world.invites.normal.tooltip");
        private static final Component OP_TEXT = Component.translatable("mco.configure.world.invites.ops.tooltip");
        private static final Component REMOVE_TEXT = Component.translatable("mco.configure.world.invites.remove.tooltip");
        private static final Identifier MAKE_OP_SPRITE = Identifier.withDefaultNamespace("player_list/make_operator");
        private static final Identifier REMOVE_OP_SPRITE = Identifier.withDefaultNamespace("player_list/remove_operator");
        private static final Identifier REMOVE_PLAYER_SPRITE = Identifier.withDefaultNamespace("player_list/remove_player");
        private static final int ICON_WIDTH = 8;
        private static final int ICON_HEIGHT = 7;
        private final PlayerInfo playerInfo;
        private final Button removeButton;
        private final Button makeOpButton;
        private final Button removeOpButton;

        public PlayerEntry(PlayerInfo playerInfo) {
            this.playerInfo = playerInfo;
            int i = RealmsPlayersTab.this.serverData.players.indexOf(this.playerInfo);
            this.makeOpButton = SpriteIconButton.builder(NORMAL_USER_TEXT, p_440611_ -> this.op(i), false)
                .sprite(MAKE_OP_SPRITE, 8, 7)
                .width(16 + RealmsPlayersTab.this.configurationScreen.getFont().width(NORMAL_USER_TEXT))
                .narration(
                    p_454109_ -> CommonComponents.joinForNarration(
                        Component.translatable("mco.invited.player.narration", playerInfo.name),
                        p_454109_.get(),
                        Component.translatable("narration.cycle_button.usage.focused", OP_TEXT)
                    )
                )
                .build();
            this.removeOpButton = SpriteIconButton.builder(OP_TEXT, p_439171_ -> this.deop(i), false)
                .sprite(REMOVE_OP_SPRITE, 8, 7)
                .width(16 + RealmsPlayersTab.this.configurationScreen.getFont().width(OP_TEXT))
                .narration(
                    p_454105_ -> CommonComponents.joinForNarration(
                        Component.translatable("mco.invited.player.narration", playerInfo.name),
                        p_454105_.get(),
                        Component.translatable("narration.cycle_button.usage.focused", NORMAL_USER_TEXT)
                    )
                )
                .build();
            this.removeButton = SpriteIconButton.builder(REMOVE_TEXT, p_439258_ -> this.uninvite(i), false)
                .sprite(REMOVE_PLAYER_SPRITE, 8, 7)
                .width(16 + RealmsPlayersTab.this.configurationScreen.getFont().width(REMOVE_TEXT))
                .narration(
                    p_454107_ -> CommonComponents.joinForNarration(Component.translatable("mco.invited.player.narration", playerInfo.name), p_454107_.get())
                )
                .build();
            this.updateOpButtons();
        }

        private void op(int index) {
            UUID uuid = RealmsPlayersTab.this.serverData.players.get(index).uuid;
            RealmsUtil.<Ops>supplyAsync(
                    p_439318_ -> p_439318_.op(RealmsPlayersTab.this.serverData.id, uuid),
                    p_439830_ -> RealmsPlayersTab.LOGGER.error("Couldn't op the user", (Throwable)p_439830_)
                )
                .thenAcceptAsync(p_439825_ -> {
                    this.updateOps(p_439825_);
                    this.updateOpButtons();
                    this.setFocused(this.removeOpButton);
                }, RealmsPlayersTab.this.minecraft);
        }

        private void deop(int index) {
            UUID uuid = RealmsPlayersTab.this.serverData.players.get(index).uuid;
            RealmsUtil.<Ops>supplyAsync(
                    p_440264_ -> p_440264_.deop(RealmsPlayersTab.this.serverData.id, uuid),
                    p_438905_ -> RealmsPlayersTab.LOGGER.error("Couldn't deop the user", (Throwable)p_438905_)
                )
                .thenAcceptAsync(p_439926_ -> {
                    this.updateOps(p_439926_);
                    this.updateOpButtons();
                    this.setFocused(this.makeOpButton);
                }, RealmsPlayersTab.this.minecraft);
        }

        private void uninvite(int index) {
            if (index >= 0 && index < RealmsPlayersTab.this.serverData.players.size()) {
                PlayerInfo playerinfo = RealmsPlayersTab.this.serverData.players.get(index);
                RealmsConfirmScreen realmsconfirmscreen = new RealmsConfirmScreen(
                    p_440157_ -> {
                        if (p_440157_) {
                            RealmsUtil.runAsync(
                                p_454111_ -> p_454111_.uninvite(RealmsPlayersTab.this.serverData.id, playerinfo.uuid),
                                p_439194_ -> RealmsPlayersTab.LOGGER.error("Couldn't uninvite user", (Throwable)p_439194_)
                            );
                            RealmsPlayersTab.this.serverData.players.remove(index);
                            RealmsPlayersTab.this.updateData(RealmsPlayersTab.this.serverData);
                        }

                        RealmsPlayersTab.this.minecraft.setScreen(RealmsPlayersTab.this.configurationScreen);
                    },
                    RealmsPlayersTab.QUESTION_TITLE,
                    Component.translatable("mco.configure.world.uninvite.player", playerinfo.name)
                );
                RealmsPlayersTab.this.minecraft.setScreen(realmsconfirmscreen);
            }
        }

        private void updateOps(Ops ops) {
            for (PlayerInfo playerinfo : RealmsPlayersTab.this.serverData.players) {
                playerinfo.operator = ops.ops().contains(playerinfo.name);
            }
        }

        private void updateOpButtons() {
            this.makeOpButton.visible = !this.playerInfo.operator;
            this.removeOpButton.visible = !this.makeOpButton.visible;
        }

        private Button activeOpButton() {
            return this.makeOpButton.visible ? this.makeOpButton : this.removeOpButton;
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return ImmutableList.of(this.activeOpButton(), this.removeButton);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(this.activeOpButton(), this.removeButton);
        }

        @Override
        public void renderContent(GuiGraphics p_439457_, int p_440311_, int p_440163_, boolean p_440552_, float p_440331_) {
            int i;
            if (!this.playerInfo.accepted) {
                i = -6250336;
            } else if (this.playerInfo.online) {
                i = -16711936;
            } else {
                i = -1;
            }

            int j = this.getContentYMiddle() - 16;
            RealmsUtil.renderPlayerFace(p_439457_, this.getContentX(), j, 32, this.playerInfo.uuid);
            int k = this.getContentYMiddle() - 9 / 2;
            p_439457_.drawString(RealmsPlayersTab.this.font, this.playerInfo.name, this.getContentX() + 8 + 32, k, i);
            int l = this.getContentYMiddle() - 10;
            int i1 = this.getContentRight() - this.removeButton.getWidth();
            this.removeButton.setPosition(i1, l);
            this.removeButton.render(p_439457_, p_440311_, p_440163_, p_440331_);
            int j1 = i1 - this.activeOpButton().getWidth() - 8;
            this.makeOpButton.setPosition(j1, l);
            this.makeOpButton.render(p_439457_, p_440311_, p_440163_, p_440331_);
            this.removeOpButton.setPosition(j1, l);
            this.removeOpButton.render(p_439457_, p_440311_, p_440163_, p_440331_);
        }
    }
}
