package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Optional;

public record StringTag(String value) implements PrimitiveTag {
    private static final int SELF_SIZE_IN_BYTES = 36;
    public static final TagType<StringTag> TYPE = new TagType.VariableSize<StringTag>() {
        public StringTag load(DataInput p_129320_, NbtAccounter p_129322_) throws IOException {
            return StringTag.valueOf(readAccounted(p_129320_, p_129322_));
        }

        @Override
        public StreamTagVisitor.ValueResult parse(DataInput p_197570_, StreamTagVisitor p_197571_, NbtAccounter p_302336_) throws IOException {
            return p_197571_.visit(readAccounted(p_197570_, p_302336_));
        }

        private static String readAccounted(DataInput input, NbtAccounter nbtAccounter) throws IOException {
            nbtAccounter.accountBytes(36L);
            String s = input.readUTF();
            nbtAccounter.readUTF(s);
            return s;
        }

        @Override
        public void skip(DataInput p_197568_, NbtAccounter p_302356_) throws IOException {
            StringTag.skipString(p_197568_);
        }

        @Override
        public String getName() {
            return "STRING";
        }

        @Override
        public String getPrettyName() {
            return "TAG_String";
        }
    };
    private static final StringTag EMPTY = new StringTag("");
    private static final char DOUBLE_QUOTE = '"';
    private static final char SINGLE_QUOTE = '\'';
    private static final char ESCAPE = '\\';
    private static final char NOT_SET = '\u0000';

    @Deprecated(forRemoval = true)
    public StringTag(String value) {
        this.value = value;
    }

    public static void skipString(DataInput input) throws IOException {
        input.skipBytes(input.readUnsignedShort());
    }

    public static StringTag valueOf(String data) {
        return data.isEmpty() ? EMPTY : new StringTag(data);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        output.writeUTF(this.value);
    }

    @Override
    public int sizeInBytes() {
        return 36 + 2 * this.value.length();
    }

    @Override
    public byte getId() {
        return 8;
    }

    @Override
    public TagType<StringTag> getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        StringTagVisitor stringtagvisitor = new StringTagVisitor();
        stringtagvisitor.visitString(this);
        return stringtagvisitor.build();
    }

    public StringTag copy() {
        return this;
    }

    @Override
    public Optional<String> asString() {
        return Optional.of(this.value);
    }

    @Override
    public void accept(TagVisitor p_178154_) {
        p_178154_.visitString(this);
    }

    public static String quoteAndEscape(String text) {
        StringBuilder stringbuilder = new StringBuilder();
        quoteAndEscape(text, stringbuilder);
        return stringbuilder.toString();
    }

    public static void quoteAndEscape(String text, StringBuilder stringBuilder) {
        int i = stringBuilder.length();
        stringBuilder.append(' ');
        char c0 = 0;

        for (int j = 0; j < text.length(); j++) {
            char c1 = text.charAt(j);
            if (c1 == '\\') {
                stringBuilder.append("\\\\");
            } else if (c1 != '"' && c1 != '\'') {
                String s = SnbtGrammar.escapeControlCharacters(c1);
                if (s != null) {
                    stringBuilder.append('\\');
                    stringBuilder.append(s);
                } else {
                    stringBuilder.append(c1);
                }
            } else {
                if (c0 == 0) {
                    c0 = (char)(c1 == '"' ? 39 : 34);
                }

                if (c0 == c1) {
                    stringBuilder.append('\\');
                }

                stringBuilder.append(c1);
            }
        }

        if (c0 == 0) {
            c0 = '"';
        }

        stringBuilder.setCharAt(i, c0);
        stringBuilder.append(c0);
    }

    public static String escapeWithoutQuotes(String input) {
        StringBuilder stringbuilder = new StringBuilder();
        escapeWithoutQuotes(input, stringbuilder);
        return stringbuilder.toString();
    }

    public static void escapeWithoutQuotes(String input, StringBuilder stringBuilder) {
        for (int i = 0; i < input.length(); i++) {
            char c0 = input.charAt(i);
            switch (c0) {
                case '"':
                case '\'':
                case '\\':
                    stringBuilder.append('\\');
                    stringBuilder.append(c0);
                    break;
                default:
                    String s = SnbtGrammar.escapeControlCharacters(c0);
                    if (s != null) {
                        stringBuilder.append('\\');
                        stringBuilder.append(s);
                    } else {
                        stringBuilder.append(c0);
                    }
            }
        }
    }

    @Override
    public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197566_) {
        return p_197566_.visit(this.value);
    }
}
