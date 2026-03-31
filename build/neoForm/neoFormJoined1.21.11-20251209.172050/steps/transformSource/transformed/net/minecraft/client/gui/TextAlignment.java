package net.minecraft.client.gui;

import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum TextAlignment {
    LEFT {
        @Override
        public int calculateLeft(int p_457890_, int p_457585_) {
            return p_457890_;
        }

        @Override
        public int calculateLeft(int p_457910_, Font p_458176_, FormattedCharSequence p_457694_) {
            return p_457910_;
        }
    },
    CENTER {
        @Override
        public int calculateLeft(int p_457759_, int p_457620_) {
            return p_457759_ - p_457620_ / 2;
        }
    },
    RIGHT {
        @Override
        public int calculateLeft(int p_458245_, int p_457756_) {
            return p_458245_ - p_457756_;
        }
    };

    public abstract int calculateLeft(int x, int width);

    public int calculateLeft(int x, Font font, FormattedCharSequence text) {
        return this.calculateLeft(x, font.width(text));
    }
}
