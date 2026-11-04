package com.github.halfdeadgames.asciigdx.workbench;

/** Text formatting of a code point for messages and titles: {@code U+0041 LATIN CAPITAL LETTER A}. */
final class GlyphLabel {

    private static final int MIN_HEX_DIGITS = 4;
    private static final int MAX_HEX_DIGITS = 6;

    private GlyphLabel() {
    }

    static void appendCodeAndName(StringBuilder out, int codePoint) {
        out.append("U+");
        appendHex(out, codePoint);
        out.append(' ').append(GlyphName.fullName(codePoint));
    }

    static void appendHex(StringBuilder out, int value) {
        int digits = MIN_HEX_DIGITS;
        while (digits < MAX_HEX_DIGITS && (value >>> (digits * 4)) != 0) {
            digits++;
        }
        for (int shift = (digits - 1) * 4; shift >= 0; shift -= 4) {
            out.append(Character.toUpperCase(Character.forDigit((value >>> shift) & 0xF, 16)));
        }
    }
}
