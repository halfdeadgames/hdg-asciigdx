package com.github.halfdeadgames.asciigdx.skindesigner.panel;

import com.badlogic.gdx.utils.JsonValue;
import com.github.halfdeadgames.asciigdx.ui.SkinSchema;
import java.util.Locale;

/** Converts between hex digits typed in the panel and the {@code "#RRGGBB"} text of a skin file. */
final class HexColorText {
    private static final int RGB_DIGIT_COUNT = 6;
    private static final int RGBA_DIGIT_COUNT = 8;
    private static final char LITERAL_PREFIX = '#';

    private HexColorText() {
    }

    /** Returns whether the text is 6 or 8 hex digits, with an optional leading {@code #}. */
    static boolean isValid(String typedText) {
        String digits = withoutPrefix(typedText.trim());
        boolean validLength = digits.length() == RGB_DIGIT_COUNT
                || digits.length() == RGBA_DIGIT_COUNT;
        return validLength && hasOnlyHexDigits(digits);
    }

    /**
     * Returns the skin file text for typed hex digits, such as {@code "#FFD700"}.
     *
     * @throws IllegalArgumentException if the text is not 6 or 8 hex digits
     */
    static String toSkinText(String typedText) {
        if (!isValid(typedText)) {
            throw new IllegalArgumentException("A color is RRGGBB or RRGGBBAA, with the digits 0 to"
                    + " 9 and A to F");
        }
        return LITERAL_PREFIX + withoutPrefix(typedText.trim()).toUpperCase(Locale.ROOT);
    }

    /**
     * Returns the hex digits a color value stands for, looking up a palette name in the
     * {@code colors} section. Returns an empty text when the value is neither a literal nor a
     * known name.
     */
    static String digitsOf(JsonValue tree, String colorValue) {
        if (colorValue.startsWith(String.valueOf(LITERAL_PREFIX))) {
            return withoutPrefix(colorValue).toUpperCase(Locale.ROOT);
        }
        JsonValue palette = tree.get(SkinSchema.COLORS);
        JsonValue named = palette == null ? null : palette.get(colorValue);
        if (named == null || !named.isString()) {
            return "";
        }
        return withoutPrefix(named.asString()).toUpperCase(Locale.ROOT);
    }

    private static String withoutPrefix(String text) {
        if (!text.isEmpty() && text.charAt(0) == LITERAL_PREFIX) {
            return text.substring(1);
        }
        return text;
    }

    private static boolean hasOnlyHexDigits(String digits) {
        for (int i = 0; i < digits.length(); i++) {
            if (Character.digit(digits.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
