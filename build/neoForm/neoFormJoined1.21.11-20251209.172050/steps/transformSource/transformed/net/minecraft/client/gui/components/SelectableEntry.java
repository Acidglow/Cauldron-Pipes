package net.minecraft.client.gui.components;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SelectableEntry {
    default boolean mouseOverIcon(int x, int y, int size) {
        return x >= 0 && x < size && y >= 0 && y < size;
    }

    default boolean mouseOverLeftHalf(int x, int y, int size) {
        return x >= 0 && x < size / 2 && y >= 0 && y < size;
    }

    default boolean mouseOverRightHalf(int x, int y, int size) {
        return x >= size / 2 && x < size && y >= 0 && y < size;
    }

    default boolean mouseOverTopRightQuarter(int x, int y, int size) {
        return x >= size / 2 && x < size && y >= 0 && y < size / 2;
    }

    default boolean mouseOverBottomRightQuarter(int x, int y, int size) {
        return x >= size / 2 && x < size && y >= size / 2 && y < size;
    }

    default boolean mouseOverTopLeftQuarter(int x, int y, int size) {
        return x >= 0 && x < size / 2 && y >= 0 && y < size / 2;
    }

    default boolean mouseOverBottomLeftQuarter(int x, int y, int size) {
        return x >= 0 && x < size / 2 && y >= size / 2 && y < size;
    }
}
