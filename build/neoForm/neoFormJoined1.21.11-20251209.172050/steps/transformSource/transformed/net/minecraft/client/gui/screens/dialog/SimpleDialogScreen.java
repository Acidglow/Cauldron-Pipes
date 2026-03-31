package net.minecraft.client.gui.screens.dialog;

import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.dialog.ActionButton;
import net.minecraft.server.dialog.SimpleDialog;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SimpleDialogScreen<T extends SimpleDialog> extends DialogScreen<T> {
    public SimpleDialogScreen(@Nullable Screen previousScreen, T dialog, DialogConnectionAccess connectionAccess) {
        super(previousScreen, dialog, connectionAccess);
    }

    protected void updateHeaderAndFooter(HeaderAndFooterLayout p_426189_, DialogControlSet p_428311_, T p_427513_, DialogConnectionAccess p_427393_) {
        super.updateHeaderAndFooter(p_426189_, p_428311_, p_427513_, p_427393_);
        LinearLayout linearlayout = LinearLayout.horizontal().spacing(8);

        for (ActionButton actionbutton : p_427513_.mainActions()) {
            linearlayout.addChild(p_428311_.createActionButton(actionbutton).build());
        }

        p_426189_.addToFooter(linearlayout);
    }
}
