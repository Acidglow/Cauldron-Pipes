package net.minecraft.client.gui.spectator.categories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.SpectatorMenuCategory;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TeleportToTeamMenuCategory implements SpectatorMenuCategory, SpectatorMenuItem {
    private static final Identifier TELEPORT_TO_TEAM_SPRITE = Identifier.withDefaultNamespace("spectator/teleport_to_team");
    private static final Component TELEPORT_TEXT = Component.translatable("spectatorMenu.team_teleport");
    private static final Component TELEPORT_PROMPT = Component.translatable("spectatorMenu.team_teleport.prompt");
    private final List<SpectatorMenuItem> items;

    public TeleportToTeamMenuCategory() {
        Minecraft minecraft = Minecraft.getInstance();
        this.items = createTeamEntries(minecraft, minecraft.level.getScoreboard());
    }

    private static List<SpectatorMenuItem> createTeamEntries(Minecraft minecraft, Scoreboard scoreboard) {
        return scoreboard.getPlayerTeams()
            .stream()
            .flatMap(p_260025_ -> TeleportToTeamMenuCategory.TeamSelectionItem.create(minecraft, p_260025_).stream())
            .toList();
    }

    @Override
    public List<SpectatorMenuItem> getItems() {
        return this.items;
    }

    @Override
    public Component getPrompt() {
        return TELEPORT_PROMPT;
    }

    @Override
    public void selectItem(SpectatorMenu menu) {
        menu.selectCategory(this);
    }

    @Override
    public Component getName() {
        return TELEPORT_TEXT;
    }

    @Override
    public void renderIcon(GuiGraphics p_282933_, float p_283568_, float p_365205_) {
        p_282933_.blitSprite(
            RenderPipelines.GUI_TEXTURED, TELEPORT_TO_TEAM_SPRITE, 0, 0, 16, 16, ARGB.colorFromFloat(p_365205_, p_283568_, p_283568_, p_283568_)
        );
    }

    @Override
    public boolean isEnabled() {
        return !this.items.isEmpty();
    }

    @OnlyIn(Dist.CLIENT)
    static class TeamSelectionItem implements SpectatorMenuItem {
        private final PlayerTeam team;
        private final Supplier<PlayerSkin> iconSkin;
        private final List<PlayerInfo> players;

        private TeamSelectionItem(PlayerTeam team, List<PlayerInfo> players, Supplier<PlayerSkin> iconSkin) {
            this.team = team;
            this.players = players;
            this.iconSkin = iconSkin;
        }

        public static Optional<SpectatorMenuItem> create(Minecraft minecraft, PlayerTeam team) {
            List<PlayerInfo> list = new ArrayList<>();

            for (String s : team.getPlayers()) {
                PlayerInfo playerinfo = minecraft.getConnection().getPlayerInfo(s);
                if (playerinfo != null && playerinfo.getGameMode() != GameType.SPECTATOR) {
                    list.add(playerinfo);
                }
            }

            if (list.isEmpty()) {
                return Optional.empty();
            } else {
                PlayerInfo playerinfo1 = list.get(RandomSource.create().nextInt(list.size()));
                return Optional.of(new TeleportToTeamMenuCategory.TeamSelectionItem(team, list, playerinfo1::getSkin));
            }
        }

        @Override
        public void selectItem(SpectatorMenu menu) {
            menu.selectCategory(new TeleportToPlayerMenuCategory(this.players));
        }

        @Override
        public Component getName() {
            return this.team.getDisplayName();
        }

        @Override
        public void renderIcon(GuiGraphics p_283215_, float p_282946_, float p_360612_) {
            Integer integer = this.team.getColor().getColor();
            if (integer != null) {
                float f = (integer >> 16 & 0xFF) / 255.0F;
                float f1 = (integer >> 8 & 0xFF) / 255.0F;
                float f2 = (integer & 0xFF) / 255.0F;
                p_283215_.fill(1, 1, 15, 15, ARGB.colorFromFloat(p_360612_, f * p_282946_, f1 * p_282946_, f2 * p_282946_));
            }

            PlayerFaceRenderer.draw(p_283215_, this.iconSkin.get(), 2, 2, 12, ARGB.colorFromFloat(p_360612_, p_282946_, p_282946_, p_282946_));
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}
