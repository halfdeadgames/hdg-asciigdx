package com.github.halfdeadgames.asciigdx.workbench;

/** Broad family a glyph belongs to, so preview text can group glyphs that are seen together. */
enum GlyphClass {
    LETTER("LETTERS"),
    DIGIT("DIGITS"),
    PUNCTUATION("PUNCTUATION MARKS"),
    BOX_AND_BLOCK("BOX AND BLOCK GLYPHS"),
    SYMBOL("SYMBOLS"),
    /** Every visible glyph; never returned by {@link #of(int)}. */
    ANY("GLYPHS");

    private static final int BOX_AND_BLOCK_FIRST = 0x2500;
    private static final int BOX_AND_BLOCK_LAST = 0x259F;

    private final String pluralLabel;

    GlyphClass(String pluralLabel) {
        this.pluralLabel = pluralLabel;
    }

    String pluralLabel() {
        return pluralLabel;
    }

    /** Returns the class of a visible code point, or null for whitespace, control, and invalid code points. */
    static GlyphClass of(int codePoint) {
        if (!isVisible(codePoint)) {
            return null;
        }
        if (Character.isLetter(codePoint)) {
            return LETTER;
        }
        if (codePoint >= BOX_AND_BLOCK_FIRST && codePoint <= BOX_AND_BLOCK_LAST) {
            return BOX_AND_BLOCK;
        }
        return switch (Character.getType(codePoint)) {
            case Character.DECIMAL_DIGIT_NUMBER, Character.LETTER_NUMBER, Character.OTHER_NUMBER -> DIGIT;
            case Character.CONNECTOR_PUNCTUATION, Character.DASH_PUNCTUATION, Character.START_PUNCTUATION,
                 Character.END_PUNCTUATION, Character.INITIAL_QUOTE_PUNCTUATION,
                 Character.FINAL_QUOTE_PUNCTUATION, Character.OTHER_PUNCTUATION -> PUNCTUATION;
            default -> SYMBOL;
        };
    }

    boolean contains(int codePoint) {
        GlyphClass actual = of(codePoint);
        return actual != null && (this == ANY || this == actual);
    }

    private static boolean isVisible(int codePoint) {
        return Character.isValidCodePoint(codePoint)
            && !Character.isWhitespace(codePoint)
            && !Character.isSpaceChar(codePoint)
            && !Character.isISOControl(codePoint);
    }
}
