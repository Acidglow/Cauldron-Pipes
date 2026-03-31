package net.minecraft.client.gui.render.state.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record GuiSkinRenderState(
    PlayerModel playerModel,
    Identifier texture,
    float rotationX,
    float rotationY,
    float pivotY,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuiSkinRenderState(
        PlayerModel p_481584_,
        Identifier p_467911_,
        float p_421905_,
        float p_421891_,
        float p_421997_,
        int p_421720_,
        int p_422175_,
        int p_422472_,
        int p_421971_,
        float p_422493_,
        @Nullable ScreenRectangle p_421756_
    ) {
        this(
            p_481584_,
            p_467911_,
            p_421905_,
            p_421891_,
            p_421997_,
            p_421720_,
            p_422175_,
            p_422472_,
            p_421971_,
            p_422493_,
            p_421756_,
            PictureInPictureRenderState.getBounds(p_421720_, p_422175_, p_422472_, p_421971_, p_421756_)
        );
    }
}
