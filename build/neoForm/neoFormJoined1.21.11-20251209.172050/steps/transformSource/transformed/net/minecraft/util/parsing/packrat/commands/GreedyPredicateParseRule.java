package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;
import org.jspecify.annotations.Nullable;

public abstract class GreedyPredicateParseRule implements Rule<StringReader, String> {
    private final int minSize;
    private final int maxSize;
    private final DelayedException<CommandSyntaxException> error;

    public GreedyPredicateParseRule(int minSize, DelayedException<CommandSyntaxException> error) {
        this(minSize, Integer.MAX_VALUE, error);
    }

    public GreedyPredicateParseRule(int minSize, int maxSize, DelayedException<CommandSyntaxException> error) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.error = error;
    }

    public @Nullable String parse(ParseState<StringReader> p_410027_) {
        StringReader stringreader = p_410027_.input();
        String s = stringreader.getString();
        int i = stringreader.getCursor();
        int j = i;

        while (j < s.length() && this.isAccepted(s.charAt(j)) && j - i < this.maxSize) {
            j++;
        }

        int k = j - i;
        if (k < this.minSize) {
            p_410027_.errorCollector().store(p_410027_.mark(), this.error);
            return null;
        } else {
            stringreader.setCursor(j);
            return s.substring(i, j);
        }
    }

    protected abstract boolean isAccepted(char c);
}
