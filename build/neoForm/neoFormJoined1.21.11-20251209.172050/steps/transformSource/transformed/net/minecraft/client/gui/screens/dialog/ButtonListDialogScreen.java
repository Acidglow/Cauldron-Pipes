package net.minecraft.client.gui.screens.dialog;

import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.ButtonListDialog;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public abstract class ButtonListDialogScreen<T extends ButtonListDialog> extends DialogScreen<T> {
    public static final int FOOTER_MARGIN = 5;

    public ButtonListDialogScreen(@Nullable Screen previousScreen, T dialog, DialogConnectionAccess connectionAccess) {
        super(previousScreen, dialog, connectionAccess);
    }

    protected void populateBodyElements(LinearLayout p_425776_, DialogControlSet p_428248_, T p_428517_, DialogConnectionAccess p_427299_) {
        super.populateBodyElements(p_425776_, p_428248_, p_428517_, p_427299_);
        List<Button> list = this.createListActions(p_428517_, p_427299_).map(p_428060_ -> p_428248_.createActionButton(p_428060_).build()).toList();
        p_425776_.addChild(packControlsIntoColumns(list, p_428517_.columns()));
    }

    protected abstract Stream<ActionButton> createListActions(T dialog, DialogConnectionAccess connectionAccess);

    protected void updateHeaderAndFooter(HeaderAndFooterLayout p_426252_, DialogControlSet p_428234_, T p_426188_, DialogConnectionAccess p_427363_) {
        super.updateHeaderAndFooter(p_426252_, p_428234_, p_426188_, p_427363_);
        p_426188_.exitAction()
            .ifPresentOrElse(p_428057_ -> p_426252_.addToFooter(p_428234_.createActionButton(p_428057_).build()), () -> p_426252_.setFooterHeight(5));
    }
}
