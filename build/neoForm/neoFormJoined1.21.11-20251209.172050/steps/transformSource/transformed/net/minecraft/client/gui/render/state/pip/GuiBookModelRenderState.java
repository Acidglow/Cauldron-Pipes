package net.minecraft.client.gui.render.state.pip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record GuiBookModelRenderState(
    BookModel bookModel,
    Identifier texture,
    float open,
    float flip,
    int x0,
    int y0,
    int x1,
    int y1,
    float scale,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements PictureInPictureRenderState {
    public GuiBookModelRenderState(
        BookModel p_480372_,
        Identifier p_469982_,
        float p_422477_,
        float p_422616_,
        int p_422448_,
        int p_421855_,
        int p_422080_,
        int p_422328_,
        float p_421772_,
        @Nullable ScreenRectangle p_421980_
    ) {
        this(
            p_480372_,
            p_469982_,
            p_422477_,
            p_422616_,
            p_422448_,
            p_421855_,
            p_422080_,
            p_422328_,
            p_421772_,
            p_421980_,
            PictureInPictureRenderState.getBounds(p_422448_, p_421855_, p_422080_, p_422328_, p_421980_)
        );
    }
}
