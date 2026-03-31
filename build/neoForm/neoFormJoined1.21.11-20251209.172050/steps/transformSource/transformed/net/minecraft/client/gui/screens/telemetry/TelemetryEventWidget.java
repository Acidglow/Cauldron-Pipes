package net.minecraft.client.gui.screens.telemetry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractTextAreaWidget;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.Layout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.layouts.SpacerElement;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.telemetry.TelemetryEventType;
import net.minecraft.client.telemetry.TelemetryProperty;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TelemetryEventWidget extends AbstractTextAreaWidget {
    private static final int HEADER_HORIZONTAL_PADDING = 32;
    private static final String TELEMETRY_REQUIRED_TRANSLATION_KEY = "telemetry.event.required";
    private static final String TELEMETRY_OPTIONAL_TRANSLATION_KEY = "telemetry.event.optional";
    private static final String TELEMETRY_OPTIONAL_DISABLED_TRANSLATION_KEY = "telemetry.event.optional.disabled";
    private static final Component PROPERTY_TITLE = Component.translatable("telemetry_info.property_title").withStyle(ChatFormatting.UNDERLINE);
    private final Font font;
    private TelemetryEventWidget.Content content;
    private @Nullable DoubleConsumer onScrolledListener;

    public TelemetryEventWidget(int x, int y, int width, int height, Font font) {
        super(x, y, width, height, Component.empty());
        this.font = font;
        this.content = this.buildContent(Minecraft.getInstance().telemetryOptInExtra());
    }

    public void onOptInChanged(boolean optIn) {
        this.content = this.buildContent(optIn);
        this.refreshScrollAmount();
    }

    public void updateLayout() {
        this.content = this.buildContent(Minecraft.getInstance().telemetryOptInExtra());
        this.refreshScrollAmount();
    }

    private TelemetryEventWidget.Content buildContent(boolean optIn) {
        TelemetryEventWidget.ContentBuilder telemetryeventwidget$contentbuilder = new TelemetryEventWidget.ContentBuilder(this.containerWidth());
        List<TelemetryEventType> list = new ArrayList<>(TelemetryEventType.values());
        list.sort(Comparator.comparing(TelemetryEventType::isOptIn));

        for (int i = 0; i < list.size(); i++) {
            TelemetryEventType telemetryeventtype = list.get(i);
            boolean flag = telemetryeventtype.isOptIn() && !optIn;
            this.addEventType(telemetryeventwidget$contentbuilder, telemetryeventtype, flag);
            if (i < list.size() - 1) {
                telemetryeventwidget$contentbuilder.addSpacer(9);
            }
        }

        return telemetryeventwidget$contentbuilder.build();
    }

    public void setOnScrolledListener(@Nullable DoubleConsumer onScrolledListener) {
        this.onScrolledListener = onScrolledListener;
    }

    @Override
    public void setScrollAmount(double p_261736_) {
        super.setScrollAmount(p_261736_);
        if (this.onScrolledListener != null) {
            this.onScrolledListener.accept(this.scrollAmount());
        }
    }

    @Override
    protected int getInnerHeight() {
        return this.content.container().getHeight();
    }

    @Override
    protected double scrollRate() {
        return 9.0;
    }

    @Override
    protected void renderContents(GuiGraphics p_283081_, int p_283426_, int p_282414_, float p_283358_) {
        int i = this.getInnerTop();
        int j = this.getInnerLeft();
        p_283081_.pose().pushMatrix();
        p_283081_.pose().translate(j, i);
        this.content.container().visitWidgets(p_280896_ -> p_280896_.render(p_283081_, p_283426_, p_282414_, p_283358_));
        p_283081_.pose().popMatrix();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_261538_) {
        p_261538_.add(NarratedElementType.TITLE, this.content.narration());
    }

    private Component grayOutIfDisabled(Component component, boolean disabled) {
        return (Component)(disabled ? component.copy().withStyle(ChatFormatting.GRAY) : component);
    }

    private void addEventType(TelemetryEventWidget.ContentBuilder contentBuilder, TelemetryEventType eventType, boolean disabled) {
        String s = eventType.isOptIn() ? (disabled ? "telemetry.event.optional.disabled" : "telemetry.event.optional") : "telemetry.event.required";
        contentBuilder.addHeader(this.font, this.grayOutIfDisabled(Component.translatable(s, eventType.title()), disabled));
        contentBuilder.addHeader(this.font, eventType.description().withStyle(ChatFormatting.GRAY));
        contentBuilder.addSpacer(9 / 2);
        contentBuilder.addLine(this.font, this.grayOutIfDisabled(PROPERTY_TITLE, disabled), 2);
        this.addEventTypeProperties(eventType, contentBuilder, disabled);
    }

    private void addEventTypeProperties(TelemetryEventType eventType, TelemetryEventWidget.ContentBuilder contentBuilder, boolean disabled) {
        for (TelemetryProperty<?> telemetryproperty : eventType.properties()) {
            contentBuilder.addLine(this.font, this.grayOutIfDisabled(telemetryproperty.title(), disabled));
        }
    }

    private int containerWidth() {
        return this.width - this.totalInnerPadding();
    }

    @OnlyIn(Dist.CLIENT)
    record Content(Layout container, Component narration) {
    }

    @OnlyIn(Dist.CLIENT)
    static class ContentBuilder {
        private final int width;
        private final LinearLayout layout;
        private final MutableComponent narration = Component.empty();

        public ContentBuilder(int width) {
            this.width = width;
            this.layout = LinearLayout.vertical();
            this.layout.defaultCellSetting().alignHorizontallyLeft();
            this.layout.addChild(SpacerElement.width(width));
        }

        public void addLine(Font font, Component message) {
            this.addLine(font, message, 0);
        }

        public void addLine(Font font, Component message, int padding) {
            this.layout.addChild(new MultiLineTextWidget(message, font).setMaxWidth(this.width), p_295098_ -> p_295098_.paddingBottom(padding));
            this.narration.append(message).append("\n");
        }

        public void addHeader(Font font, Component message) {
            this.layout
                .addChild(
                    new MultiLineTextWidget(message, font).setMaxWidth(this.width - 64).setCentered(true),
                    p_296036_ -> p_296036_.alignHorizontallyCenter().paddingHorizontal(32)
                );
            this.narration.append(message).append("\n");
        }

        public void addSpacer(int height) {
            this.layout.addChild(SpacerElement.height(height));
        }

        public TelemetryEventWidget.Content build() {
            this.layout.arrangeElements();
            return new TelemetryEventWidget.Content(this.layout, this.narration);
        }
    }
}
