package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;
import org.jspecify.annotations.Nullable;

public class UnquotedStringParseRule implements Rule<StringReader, String> {
    private final int minSize;
    private final DelayedException<CommandSyntaxException> error;

    public UnquotedStringParseRule(int minSize, DelayedException<CommandSyntaxException> error) {
        this.minSize = minSize;
        this.error = error;
    }

    public @Nullable String parse(ParseState<StringReader> p_410705_) {
        p_410705_.input().skipWhitespace();
        int i = p_410705_.mark();
        String s = p_410705_.input().readUnquotedString();
        if (s.length() < this.minSize) {
            p_410705_.errorCollector().store(i, this.error);
            return null;
        } else {
            return s;
        }
    }
}
