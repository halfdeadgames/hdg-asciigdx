package com.github.halfdeadgames.asciigdx.workbench.preview;

import com.github.halfdeadgames.asciigdx.workbench.unicodenames.CategoryTypes;
import com.github.halfdeadgames.asciigdx.workbench.unicodenames.UnicodeFacts;

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
        return of(codePoint, UnicodeFacts.bundled());
    }

    /** Same as {@link #of(int)}, with the Unicode facts given by the caller. */
    static GlyphClass of(int codePoint, UnicodeFacts facts) {
        if (!Character.isValidCodePoint(codePoint)) {
            return null;
        }
        int type = facts.type(codePoint);
        if (!isVisible(codePoint, type)) {
            return null;
        }
        if (CategoryTypes.isLetter(type)) {
            return LETTER;
        }
        if (codePoint >= BOX_AND_BLOCK_FIRST && codePoint <= BOX_AND_BLOCK_LAST) {
            return BOX_AND_BLOCK;
        }
        return switch (type) {
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

    private static boolean isVisible(int codePoint, int type) {
        return !Character.isWhitespace(codePoint)
            && !CategoryTypes.isSeparator(type)
            && !Character.isISOControl(codePoint);
    }
}
