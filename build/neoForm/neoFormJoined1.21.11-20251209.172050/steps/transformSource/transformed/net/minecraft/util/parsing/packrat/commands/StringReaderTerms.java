package net.minecraft.util.parsing.packrat.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import it.unimi.dsi.fastutil.chars.CharList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.util.parsing.packrat.Control;
import net.minecraft.util.parsing.packrat.DelayedException;
import net.minecraft.util.parsing.packrat.ParseState;
import net.minecraft.util.parsing.packrat.Scope;
import net.minecraft.util.parsing.packrat.SuggestionSupplier;
import net.minecraft.util.parsing.packrat.Term;

public interface StringReaderTerms {
    static Term<StringReader> word(String value) {
        return new StringReaderTerms.TerminalWord(value);
    }

    static Term<StringReader> character(final char value) {
        return new StringReaderTerms.TerminalCharacters(CharList.of(value)) {
            @Override
            protected boolean isAccepted(char p_410825_) {
                return value == p_410825_;
            }
        };
    }

    static Term<StringReader> characters(final char value1, final char value2) {
        return new StringReaderTerms.TerminalCharacters(CharList.of(value1, value2)) {
            @Override
            protected boolean isAccepted(char p_410435_) {
                return p_410435_ == value1 || p_410435_ == value2;
            }
        };
    }

    static StringReader createReader(String input, int cursor) {
        StringReader stringreader = new StringReader(input);
        stringreader.setCursor(cursor);
        return stringreader;
    }

    public abstract static class TerminalCharacters implements Term<StringReader> {
        private final DelayedException<CommandSyntaxException> error;
        private final SuggestionSupplier<StringReader> suggestions;

        public TerminalCharacters(CharList characters) {
            String s = characters.intStream().mapToObj(Character::toString).collect(Collectors.joining("|"));
            this.error = DelayedException.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(), s);
            this.suggestions = p_410379_ -> characters.intStream().mapToObj(Character::toString);
        }

        @Override
        public boolean parse(ParseState<StringReader> p_409975_, Scope p_409815_, Control p_410378_) {
            p_409975_.input().skipWhitespace();
            int i = p_409975_.mark();
            if (p_409975_.input().canRead() && this.isAccepted(p_409975_.input().read())) {
                return true;
            } else {
                p_409975_.errorCollector().store(i, this.suggestions, this.error);
                return false;
            }
        }

        protected abstract boolean isAccepted(char c);
    }

    public static final class TerminalWord implements Term<StringReader> {
        private final String value;
        private final DelayedException<CommandSyntaxException> error;
        private final SuggestionSupplier<StringReader> suggestions;

        public TerminalWord(String value) {
            this.value = value;
            this.error = DelayedException.create(CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect(), value);
            this.suggestions = p_409184_ -> Stream.of(value);
        }

        @Override
        public boolean parse(ParseState<StringReader> p_335419_, Scope p_335724_, Control p_335868_) {
            p_335419_.input().skipWhitespace();
            int i = p_335419_.mark();
            String s = p_335419_.input().readUnquotedString();
            if (!s.equals(this.value)) {
                p_335419_.errorCollector().store(i, this.suggestions, this.error);
                return false;
            } else {
                return true;
            }
        }

        @Override
        public String toString() {
            return "terminal[" + this.value + "]";
        }
    }
}
