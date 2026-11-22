package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import java.util.Locale;

/** Converts between the {@code U+XXXX} text of the panel and the one-character text of a skin file. */
final class GlyphText {
    private static final String PREFIX = "U+";
    private static final int MAX_HEX_DIGITS = 6;

    private GlyphText() {
    }

    /**
     * Returns the one-code-point text for typed {@code U+XXXX} text. The prefix is optional and
     * not case sensitive.
     *
     * @throws IllegalArgumentException if the text is not 1 to 6 hex digits naming a valid code
     *         point
     */
    static String toSkinText(String typedText) {
        String trimmed = typedText.trim();
        String digits = trimmed.regionMatches(true, 0, PREFIX, 0, PREFIX.length())
                ? trimmed.substring(PREFIX.length()) : trimmed;
        if (digits.isEmpty() || digits.length() > MAX_HEX_DIGITS || !isHex(digits)) {
            throw new IllegalArgumentException("A glyph is written U+ and 1 to 6 hex digits, "
                    + "such as U+2588");
        }
        int codePoint = Integer.parseInt(digits, 16);
        if (!Character.isValidCodePoint(codePoint) || Character.isISOControl(codePoint)) {
            throw new IllegalArgumentException("U+" + digits.toUpperCase(Locale.ROOT)
                    + " is not a drawable character");
        }
        return new String(Character.toChars(codePoint));
    }

    /** Returns the {@code U+XXXX} text of the first code point of a glyph value. */
    static String toTypedText(String skinText) {
        if (skinText.isEmpty()) {
            return "";
        }
        return String.format(Locale.ROOT, "U+%04X", skinText.codePointAt(0));
    }

    private static boolean isHex(String digits) {
        for (int i = 0; i < digits.length(); i++) {
            if (Character.digit(digits.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
