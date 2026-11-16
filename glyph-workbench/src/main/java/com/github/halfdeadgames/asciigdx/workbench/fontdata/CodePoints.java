package com.github.halfdeadgames.asciigdx.workbench.fontdata;

/** Shared reading and checking of code points written as hex text. */
public final class CodePoints {

    /** Returned by the readers when the text is not in the expected form. */
    public static final int NOT_A_CODE = -1;

    /** Returned by the readers for well-formed text whose value is above U+10FFFF. */
    public static final int OUT_OF_UNICODE = Integer.MAX_VALUE;

    private static final int MAX_HEX_DIGITS = 8;
    private static final int PREFIX_LENGTH = 2;

    private CodePoints() {
    }

    /**
     * Reads a code written as {@code U+} or {@code u+} followed by 1 to 8 ASCII hex digits,
     * either case, and nothing else. A value above U+10FFFF comes back as {@link #OUT_OF_UNICODE},
     * so callers can report "out of Unicode" themselves with
     * {@link Character#isValidCodePoint(int)}.
     *
     * @param text the text to read, not null
     * @return the value, or {@link #NOT_A_CODE} if the text is not in that form
     */
    public static int readUPlus(String text) {
        if (text.length() <= PREFIX_LENGTH || (text.charAt(0) != 'U' && text.charAt(0) != 'u')
                || text.charAt(1) != '+') {
            return NOT_A_CODE;
        }
        return readHexDigits(text, PREFIX_LENGTH);
    }

    /**
     * Reads 1 to 8 ASCII hex digits, either case, and nothing else. Like
     * {@link #readUPlus(String)}, a value above U+10FFFF comes back as {@link #OUT_OF_UNICODE}.
     *
     * @param text the digits, not null
     * @return the value, or {@link #NOT_A_CODE} if the text is not 1 to 8 hex digits
     */
    public static int readHexDigits(String text) {
        return readHexDigits(text, 0);
    }

    /**
     * Tells whether a code point can be a glyph: a valid Unicode code point that is not a surrogate
     * and not {@link SheetOrder#FILLER}.
     *
     * @param codePoint the code point to check
     * @return true if it can be used as a glyph
     */
    public static boolean isUsableAsGlyph(int codePoint) {
        return Character.isValidCodePoint(codePoint)
                && Character.getType(codePoint) != Character.SURROGATE
                && codePoint != SheetOrder.FILLER;
    }

    /**
     * Writes a code point the way font files and messages name it, like {@code U+0041} or
     * {@code U+1F600}.
     *
     * @param codePoint the code point
     * @return {@code U+} and at least 4 uppercase hex digits
     */
    public static String label(int codePoint) {
        return String.format("U+%04X", codePoint);
    }

    private static int readHexDigits(String text, int start) {
        int digitCount = text.length() - start;
        if (digitCount < 1 || digitCount > MAX_HEX_DIGITS) {
            return NOT_A_CODE;
        }
        long value = 0;
        for (int i = start; i < text.length(); i++) {
            int digit = hexValue(text.charAt(i));
            if (digit < 0) {
                return NOT_A_CODE;
            }
            value = value * 16 + digit;
        }
        return value > Character.MAX_CODE_POINT ? OUT_OF_UNICODE : (int) value;
    }

    private static int hexValue(char c) {
        if (c >= '0' && c <= '9') {
            return c - '0';
        }
        if (c >= 'a' && c <= 'f') {
            return c - 'a' + 10;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 'A' + 10;
        }
        return -1;
    }
}
