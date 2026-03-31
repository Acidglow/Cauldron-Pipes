package net.minecraft.client.gui.screens.inventory.tooltip;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientActivePlayersTooltip implements ClientTooltipComponent {
    private static final int SKIN_SIZE = 10;
    private static final int PADDING = 2;
    private final List<PlayerSkinRenderCache.RenderInfo> activePlayers;

    public ClientActivePlayersTooltip(ClientActivePlayersTooltip.ActivePlayersTooltip tooltip) {
        this.activePlayers = tooltip.profiles();
    }

    @Override
    public int getHeight(Font p_360698_) {
        return this.activePlayers.size() * 12 + 2;
    }

    private static String getName(PlayerSkinRenderCache.RenderInfo renderInfo) {
        return renderInfo.gameProfile().name();
    }

    @Override
    public int getWidth(Font p_351017_) {
        int i = 0;

        for (PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo : this.activePlayers) {
            int j = p_351017_.width(getName(playerskinrendercache$renderinfo));
            if (j > i) {
                i = j;
            }
        }

        return i + 10 + 6;
    }

    @Override
    public void renderImage(Font p_350808_, int p_350702_, int p_350999_, int p_368644_, int p_368594_, GuiGraphics p_350342_) {
        for (int i = 0; i < this.activePlayers.size(); i++) {
            PlayerSkinRenderCache.RenderInfo playerskinrendercache$renderinfo = this.activePlayers.get(i);
            int j = p_350999_ + 2 + i * 12;
            PlayerFaceRenderer.draw(p_350342_, playerskinrendercache$renderinfo.playerSkin(), p_350702_ + 2, j, 10);
            p_350342_.drawString(p_350808_, getName(playerskinrendercache$renderinfo), p_350702_ + 10 + 4, j + 2, -1);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record ActivePlayersTooltip(List<PlayerSkinRenderCache.RenderInfo> profiles) implements TooltipComponent {
    }
}
