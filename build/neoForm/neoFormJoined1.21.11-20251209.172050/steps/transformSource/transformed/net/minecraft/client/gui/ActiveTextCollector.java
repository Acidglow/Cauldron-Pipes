package net.minecraft.client.gui;

import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.gui.font.ActiveArea;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.state.GuiTextRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface ActiveTextCollector {
    double PERIOD_PER_SCROLLED_PIXEL = 0.5;
    double MIN_SCROLL_PERIOD = 3.0;

    ActiveTextCollector.Parameters defaultParameters();

    void defaultParameters(ActiveTextCollector.Parameters defaultParameters);

    default void accept(int x, int y, FormattedCharSequence text) {
        this.accept(TextAlignment.LEFT, x, y, this.defaultParameters(), text);
    }

    default void accept(int x, int y, Component text) {
        this.accept(TextAlignment.LEFT, x, y, this.defaultParameters(), text.getVisualOrderText());
    }

    default void accept(TextAlignment textAlignment, int x, int y, ActiveTextCollector.Parameters parameters, Component text) {
        this.accept(textAlignment, x, y, parameters, text.getVisualOrderText());
    }

    void accept(TextAlignment textAlignment, int x, int y, ActiveTextCollector.Parameters parameters, FormattedCharSequence text);

    default void accept(TextAlignment textAlignment, int x, int y, Component text) {
        this.accept(textAlignment, x, y, text.getVisualOrderText());
    }

    default void accept(TextAlignment textAlignment, int x, int y, FormattedCharSequence text) {
        this.accept(textAlignment, x, y, this.defaultParameters(), text);
    }

    void acceptScrolling(
        Component text, int center, int minX, int maxX, int minY, int maxY, ActiveTextCollector.Parameters parameters
    );

    default void acceptScrolling(Component text, int center, int minX, int maxX, int minY, int maxY) {
        this.acceptScrolling(text, center, minX, maxX, minY, maxY, this.defaultParameters());
    }

    default void acceptScrollingWithDefaultCenter(Component text, int minX, int maxX, int minY, int maxY) {
        this.acceptScrolling(text, (minX + maxX) / 2, minX, maxX, minY, maxY);
    }

    default void defaultScrollingHelper(
        Component text,
        int center,
        int minX,
        int maxX,
        int minY,
        int maxY,
        int width,
        int height,
        ActiveTextCollector.Parameters parameters
    ) {
        int i = (minY + maxY - height) / 2 + 1;
        int j = maxX - minX;
        if (width > j) {
            int k = width - j;
            double d0 = Util.getMillis() / 1000.0;
            double d1 = Math.max(k * 0.5, 3.0);
            double d2 = Math.sin((Math.PI / 2) * Math.cos((Math.PI * 2) * d0 / d1)) / 2.0 + 0.5;
            double d3 = Mth.lerp(d2, 0.0, (double)k);
            ActiveTextCollector.Parameters activetextcollector$parameters = parameters.withScissor(minX, maxX, minY, maxY);
            this.accept(TextAlignment.LEFT, minX - (int)d3, i, activetextcollector$parameters, text.getVisualOrderText());
        } else {
            int l = Mth.clamp(center, minX + width / 2, maxX - width / 2);
            this.accept(TextAlignment.CENTER, l, i, text);
        }
    }

    static void findElementUnderCursor(GuiTextRenderState renderState, float mouseX, float mouseY, final Consumer<Style> styleScanner) {
        ScreenRectangle screenrectangle = renderState.bounds();
        if (screenrectangle != null && screenrectangle.containsPoint((int)mouseX, (int)mouseY)) {
            Vector2fc vector2fc = renderState.pose.invert(new Matrix3x2f()).transformPosition(new Vector2f(mouseX, mouseY));
            final float f = vector2fc.x();
            final float f1 = vector2fc.y();
            renderState.ensurePrepared()
                .visit(
                    new Font.GlyphVisitor() {
                        @Override
                        public void acceptGlyph(TextRenderable.Styled p_458185_) {
                            this.acceptActiveArea(p_458185_);
                        }

                        @Override
                        public void acceptEmptyArea(EmptyArea p_457803_) {
                            this.acceptActiveArea(p_457803_);
                        }

                        private void acceptActiveArea(ActiveArea activeArea) {
                            if (ActiveTextCollector.isPointInRectangle(
                                f, f1, activeArea.activeLeft(), activeArea.activeTop(), activeArea.activeRight(), activeArea.activeBottom()
                            )) {
                                styleScanner.accept(activeArea.style());
                            }
                        }
                    }
                );
        }
    }

    static boolean isPointInRectangle(float mouseX, float mouseY, float minX, float minY, float maxX, float maxY) {
        return mouseX >= minX && mouseX < maxX && mouseY >= minY && mouseY < maxY;
    }

    @OnlyIn(Dist.CLIENT)
    public static class ClickableStyleFinder implements ActiveTextCollector {
        private static final ActiveTextCollector.Parameters INITIAL = new ActiveTextCollector.Parameters(new Matrix3x2f());
        private final Font font;
        private final int testX;
        private final int testY;
        private ActiveTextCollector.Parameters defaultParameters = INITIAL;
        private boolean includeInsertions;
        private @Nullable Style result;
        private final Consumer<Style> styleScanner = p_477720_ -> {
            if (p_477720_.getClickEvent() != null || this.includeInsertions && p_477720_.getInsertion() != null) {
                this.result = p_477720_;
            }
        };

        public ClickableStyleFinder(Font font, int testX, int testY) {
            this.font = font;
            this.testX = testX;
            this.testY = testY;
        }

        @Override
        public ActiveTextCollector.Parameters defaultParameters() {
            return this.defaultParameters;
        }

        @Override
        public void defaultParameters(ActiveTextCollector.Parameters p_457712_) {
            this.defaultParameters = p_457712_;
        }

        @Override
        public void accept(TextAlignment p_457904_, int p_458286_, int p_457947_, ActiveTextCollector.Parameters p_457608_, FormattedCharSequence p_457770_) {
            int i = p_457904_.calculateLeft(p_458286_, this.font, p_457770_);
            GuiTextRenderState guitextrenderstate = new GuiTextRenderState(
                this.font, p_457770_, p_457608_.pose(), i, p_457947_, ARGB.white(p_457608_.opacity()), 0, true, true, p_457608_.scissor()
            );
            ActiveTextCollector.findElementUnderCursor(guitextrenderstate, this.testX, this.testY, this.styleScanner);
        }

        @Override
        public void acceptScrolling(
            Component p_458278_, int p_457953_, int p_457705_, int p_458095_, int p_457790_, int p_457858_, ActiveTextCollector.Parameters p_458162_
        ) {
            int i = this.font.width(p_458278_);
            int j = 9;
            this.defaultScrollingHelper(p_458278_, p_457953_, p_457705_, p_458095_, p_457790_, p_457858_, i, j, p_458162_);
        }

        public ActiveTextCollector.ClickableStyleFinder includeInsertions(boolean includeInsertions) {
            this.includeInsertions = includeInsertions;
            return this;
        }

        public @Nullable Style result() {
            return this.result;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Parameters(Matrix3x2fc pose, float opacity, @Nullable ScreenRectangle scissor) {
        public Parameters(Matrix3x2fc p_457626_) {
            this(p_457626_, 1.0F, null);
        }

        public ActiveTextCollector.Parameters withPose(Matrix3x2fc pose) {
            return new ActiveTextCollector.Parameters(pose, this.opacity, this.scissor);
        }

        public ActiveTextCollector.Parameters withScale(float scale) {
            return this.withPose(this.pose.scale(scale, scale, new Matrix3x2f()));
        }

        public ActiveTextCollector.Parameters withOpacity(float opacity) {
            return this.opacity == opacity ? this : new ActiveTextCollector.Parameters(this.pose, opacity, this.scissor);
        }

        public ActiveTextCollector.Parameters withScissor(ScreenRectangle scissor) {
            return scissor.equals(this.scissor) ? this : new ActiveTextCollector.Parameters(this.pose, this.opacity, scissor);
        }

        public ActiveTextCollector.Parameters withScissor(int minX, int maxX, int minY, int maxY) {
            ScreenRectangle screenrectangle = new ScreenRectangle(minX, minY, maxX - minX, maxY - minY)
                .transformAxisAligned(this.pose);
            if (this.scissor != null) {
                screenrectangle = Objects.requireNonNullElse(this.scissor.intersection(screenrectangle), ScreenRectangle.empty());
            }

            return this.withScissor(screenrectangle);
        }
    }
}
