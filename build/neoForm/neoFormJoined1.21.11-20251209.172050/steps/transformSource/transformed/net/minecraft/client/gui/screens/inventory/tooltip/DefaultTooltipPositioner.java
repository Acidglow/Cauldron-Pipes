package net.minecraft.client.gui.screens.inventory.tooltip;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.joml.Vector2ic;

@OnlyIn(Dist.CLIENT)
public class DefaultTooltipPositioner implements ClientTooltipPositioner {
    public static final ClientTooltipPositioner INSTANCE = new DefaultTooltipPositioner();

    private DefaultTooltipPositioner() {
    }

    @Override
    public Vector2ic positionTooltip(int p_281867_, int p_282915_, int p_283108_, int p_282881_, int p_283243_, int p_282104_) {
        Vector2i vector2i = new Vector2i(p_283108_, p_282881_).add(12, -12);
        this.positionTooltip(p_281867_, p_282915_, vector2i, p_283243_, p_282104_);
        return vector2i;
    }

    private void positionTooltip(int screenWidth, int screenHeight, Vector2i tooltipPos, int tooltipWidth, int tooltipHeight) {
        if (tooltipPos.x + tooltipWidth > screenWidth) {
            tooltipPos.x = Math.max(tooltipPos.x - 24 - tooltipWidth, 4);
        }

        int i = tooltipHeight + 3;
        if (tooltipPos.y + i > screenHeight) {
            tooltipPos.y = screenHeight - i;
        }
    }
}
