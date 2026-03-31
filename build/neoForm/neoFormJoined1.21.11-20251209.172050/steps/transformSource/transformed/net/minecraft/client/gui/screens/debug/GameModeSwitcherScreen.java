package net.minecraft.client.gui.screens.debug;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.GameNarrator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ServerboundChangeGameModePacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.commands.GameModeCommand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameModeSwitcherScreen extends Screen {
    static final Identifier SLOT_SPRITE = Identifier.withDefaultNamespace("gamemode_switcher/slot");
    static final Identifier SELECTION_SPRITE = Identifier.withDefaultNamespace("gamemode_switcher/selection");
    private static final Identifier GAMEMODE_SWITCHER_LOCATION = Identifier.withDefaultNamespace("textures/gui/container/gamemode_switcher.png");
    private static final int SPRITE_SHEET_WIDTH = 128;
    private static final int SPRITE_SHEET_HEIGHT = 128;
    private static final int SLOT_AREA = 26;
    private static final int SLOT_PADDING = 5;
    private static final int SLOT_AREA_PADDED = 31;
    private static final int HELP_TIPS_OFFSET_Y = 5;
    private static final int ALL_SLOTS_WIDTH = GameModeSwitcherScreen.GameModeIcon.values().length * 31 - 5;
    private final GameModeSwitcherScreen.GameModeIcon previousHovered;
    private GameModeSwitcherScreen.GameModeIcon currentlyHovered;
    private int firstMouseX;
    private int firstMouseY;
    private boolean setFirstMousePos;
    private final List<GameModeSwitcherScreen.GameModeSlot> slots = Lists.newArrayList();

    public GameModeSwitcherScreen() {
        super(GameNarrator.NO_TITLE);
        this.previousHovered = GameModeSwitcherScreen.GameModeIcon.getFromGameType(this.getDefaultSelected());
        this.currentlyHovered = this.previousHovered;
    }

    private GameType getDefaultSelected() {
        MultiPlayerGameMode multiplayergamemode = Minecraft.getInstance().gameMode;
        GameType gametype = multiplayergamemode.getPreviousPlayerMode();
        if (gametype != null) {
            return gametype;
        } else {
            return multiplayergamemode.getPlayerMode() == GameType.CREATIVE ? GameType.SURVIVAL : GameType.CREATIVE;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.slots.clear();
        this.currentlyHovered = this.previousHovered;

        for (int i = 0; i < GameModeSwitcherScreen.GameModeIcon.VALUES.length; i++) {
            GameModeSwitcherScreen.GameModeIcon gamemodeswitcherscreen$gamemodeicon = GameModeSwitcherScreen.GameModeIcon.VALUES[i];
            this.slots
                .add(
                    new GameModeSwitcherScreen.GameModeSlot(
                        gamemodeswitcherscreen$gamemodeicon, this.width / 2 - ALL_SLOTS_WIDTH / 2 + i * 31, this.height / 2 - 31
                    )
                );
        }
    }

    @Override
    public void render(GuiGraphics p_281834_, int p_283223_, int p_282178_, float p_281339_) {
        p_281834_.drawCenteredString(this.font, this.currentlyHovered.name, this.width / 2, this.height / 2 - 31 - 20, -1);
        MutableComponent mutablecomponent = Component.translatable(
            "debug.gamemodes.select_next", this.minecraft.options.keyDebugSwitchGameMode.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.AQUA)
        );
        p_281834_.drawCenteredString(this.font, mutablecomponent, this.width / 2, this.height / 2 + 5, -1);
        if (!this.setFirstMousePos) {
            this.firstMouseX = p_283223_;
            this.firstMouseY = p_282178_;
            this.setFirstMousePos = true;
        }

        boolean flag = this.firstMouseX == p_283223_ && this.firstMouseY == p_282178_;

        for (GameModeSwitcherScreen.GameModeSlot gamemodeswitcherscreen$gamemodeslot : this.slots) {
            gamemodeswitcherscreen$gamemodeslot.render(p_281834_, p_283223_, p_282178_, p_281339_);
            gamemodeswitcherscreen$gamemodeslot.setSelected(this.currentlyHovered == gamemodeswitcherscreen$gamemodeslot.icon);
            if (!flag && gamemodeswitcherscreen$gamemodeslot.isHoveredOrFocused()) {
                this.currentlyHovered = gamemodeswitcherscreen$gamemodeslot.icon;
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics p_294233_, int p_295829_, int p_296393_, float p_294567_) {
        int i = this.width / 2 - 62;
        int j = this.height / 2 - 31 - 27;
        p_294233_.blit(RenderPipelines.GUI_TEXTURED, GAMEMODE_SWITCHER_LOCATION, i, j, 0.0F, 0.0F, 125, 75, 128, 128);
    }

    private void switchToHoveredGameMode() {
        switchToHoveredGameMode(this.minecraft, this.currentlyHovered);
    }

    private static void switchToHoveredGameMode(Minecraft minecraft, GameModeSwitcherScreen.GameModeIcon gameModeIcon) {
        if (minecraft.canSwitchGameMode()) {
            GameModeSwitcherScreen.GameModeIcon gamemodeswitcherscreen$gamemodeicon = GameModeSwitcherScreen.GameModeIcon.getFromGameType(
                minecraft.gameMode.getPlayerMode()
            );
            if (gameModeIcon != gamemodeswitcherscreen$gamemodeicon && GameModeCommand.PERMISSION_CHECK.check(minecraft.player.permissions())) {
                minecraft.player.connection.send(new ServerboundChangeGameModePacket(gameModeIcon.mode));
            }
        }
    }

    @Override
    public boolean keyPressed(KeyEvent p_445656_) {
        if (this.minecraft.options.keyDebugSwitchGameMode.matches(p_445656_)) {
            this.setFirstMousePos = false;
            this.currentlyHovered = this.currentlyHovered.getNext();
            return true;
        } else {
            return super.keyPressed(p_445656_);
        }
    }

    @Override
    public boolean keyReleased(KeyEvent p_455463_) {
        if (this.minecraft.options.keyDebugModifier.matches(p_455463_)) {
            this.switchToHoveredGameMode();
            this.minecraft.setScreen(null);
            return true;
        } else {
            return super.keyReleased(p_455463_);
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent p_470683_) {
        if (this.minecraft.options.keyDebugModifier.matchesMouse(p_470683_)) {
            this.switchToHoveredGameMode();
            this.minecraft.setScreen(null);
            return true;
        } else {
            return super.mouseReleased(p_470683_);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    static enum GameModeIcon {
        CREATIVE(Component.translatable("gameMode.creative"), GameType.CREATIVE, new ItemStack(Blocks.GRASS_BLOCK)),
        SURVIVAL(Component.translatable("gameMode.survival"), GameType.SURVIVAL, new ItemStack(Items.IRON_SWORD)),
        ADVENTURE(Component.translatable("gameMode.adventure"), GameType.ADVENTURE, new ItemStack(Items.MAP)),
        SPECTATOR(Component.translatable("gameMode.spectator"), GameType.SPECTATOR, new ItemStack(Items.ENDER_EYE));

        static final GameModeSwitcherScreen.GameModeIcon[] VALUES = values();
        private static final int ICON_AREA = 16;
        private static final int ICON_TOP_LEFT = 5;
        final Component name;
        final GameType mode;
        private final ItemStack renderStack;

        private GameModeIcon(Component name, GameType mode, ItemStack renderStack) {
            this.name = name;
            this.mode = mode;
            this.renderStack = renderStack;
        }

        void drawIcon(GuiGraphics guiGraphics, int x, int y) {
            guiGraphics.renderItem(this.renderStack, x, y);
        }

        GameModeSwitcherScreen.GameModeIcon getNext() {
            return switch (this) {
                case CREATIVE -> SURVIVAL;
                case SURVIVAL -> ADVENTURE;
                case ADVENTURE -> SPECTATOR;
                case SPECTATOR -> CREATIVE;
            };
        }

        static GameModeSwitcherScreen.GameModeIcon getFromGameType(GameType gameType) {
            return switch (gameType) {
                case SPECTATOR -> SPECTATOR;
                case SURVIVAL -> SURVIVAL;
                case CREATIVE -> CREATIVE;
                case ADVENTURE -> ADVENTURE;
            };
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class GameModeSlot extends AbstractWidget {
        final GameModeSwitcherScreen.GameModeIcon icon;
        private boolean isSelected;

        public GameModeSlot(GameModeSwitcherScreen.GameModeIcon icon, int x, int y) {
            super(x, y, 26, 26, icon.name);
            this.icon = icon;
        }

        @Override
        public void renderWidget(GuiGraphics p_281380_, int p_283094_, int p_283558_, float p_282631_) {
            this.drawSlot(p_281380_);
            if (this.isSelected) {
                this.drawSelection(p_281380_);
            }

            this.icon.drawIcon(p_281380_, this.getX() + 5, this.getY() + 5);
        }

        @Override
        public void updateWidgetNarration(NarrationElementOutput p_259120_) {
            this.defaultButtonNarrationText(p_259120_);
        }

        @Override
        public boolean isHoveredOrFocused() {
            return super.isHoveredOrFocused() || this.isSelected;
        }

        public void setSelected(boolean isSelected) {
            this.isSelected = isSelected;
        }

        private void drawSlot(GuiGraphics guiGraphics) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, GameModeSwitcherScreen.SLOT_SPRITE, this.getX(), this.getY(), 26, 26);
        }

        private void drawSelection(GuiGraphics guiGraphics) {
            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, GameModeSwitcherScreen.SELECTION_SPRITE, this.getX(), this.getY(), 26, 26);
        }
    }
}
