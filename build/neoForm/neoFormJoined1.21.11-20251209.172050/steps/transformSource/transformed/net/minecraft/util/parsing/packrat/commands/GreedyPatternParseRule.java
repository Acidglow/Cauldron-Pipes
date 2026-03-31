package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;

public final class GreedyPatternParseRule implements Rule<StringReader, String> {
    private final Pattern pattern;
    private final DelayedException<CommandSyntaxException> error;

    public GreedyPatternParseRule(Pattern pattern, DelayedException<CommandSyntaxException> error) {
        this.pattern = pattern;
        this.error = error;
    }

    public String parse(ParseState<StringReader> p_410475_) {
        StringReader stringreader = p_410475_.input();
        String s = stringreader.getString();
        Matcher matcher = this.pattern.matcher(s).region(stringreader.getCursor(), s.length());
        if (!matcher.lookingAt()) {
            p_410475_.errorCollector().store(p_410475_.mark(), this.error);
            return null;
        } else {
            stringreader.setCursor(matcher.end());
            return matcher.group(0);
        }
    }
}
