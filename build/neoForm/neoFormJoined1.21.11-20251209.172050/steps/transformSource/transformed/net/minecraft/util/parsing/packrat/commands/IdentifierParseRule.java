package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.resources.Identifier;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Rule;
import org.jspecify.annotations.Nullable;

public class IdentifierParseRule implements Rule<StringReader, Identifier> {
    public static final Rule<StringReader, Identifier> INSTANCE = new IdentifierParseRule();

    private IdentifierParseRule() {
    }

    public @Nullable Identifier parse(ParseState<StringReader> p_467860_) {
        p_467860_.input().skipWhitespace();

        try {
            return Identifier.readNonEmpty(p_467860_.input());
        } catch (CommandSyntaxException commandsyntaxexception) {
            return null;
        }
    }
}
