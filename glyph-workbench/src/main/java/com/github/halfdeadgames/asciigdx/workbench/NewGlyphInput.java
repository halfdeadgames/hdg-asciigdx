package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.SheetOrder;

/** Turns text typed into the "new glyph" prompt into a code point. */
final class NewGlyphInput {

    private static final int MAX_HEX_DIGITS = 6;

    /** The outcome of parsing: a code point when valid, a user-facing message otherwise. */
    record Result(int codePoint, String error) {

        /** Returns true when the input named a usable code point. */
        boolean isValid() {
            return error == null;
        }
    }

    private NewGlyphInput() {
    }

    /**
     * Reads either a single character or a hex code point such as {@code 41} or {@code U+0041}.
     *
     * @param typed the raw text from the prompt
     * @return the code point, or an error message
     */
    static Result parse(String typed) {
        if (typed == null || typed.isEmpty()) {
            return failure("Type a character or a code point like U+0041");
        }
        if (typed.codePointCount(0, typed.length()) == 1) {
            return check(typed.codePointAt(0));
        }
        var stripped = typed.strip();
        if (stripped.isEmpty()) {
            return failure("Type a character or a code point like U+0041");
        }
        boolean hasPrefix = stripped.startsWith("U+") || stripped.startsWith("u+");
        if (!hasPrefix && stripped.codePointCount(0, stripped.length()) == 1) {
            return check(stripped.codePointAt(0));
        }
        return parseHex(stripped, hasPrefix);
    }

    private static Result parseHex(String stripped, boolean hasPrefix) {
        var digits = hasPrefix ? stripped.substring(2) : stripped;
        if (digits.isEmpty()) {
            return failure("Add hex digits after U+");
        }
        if (digits.length() > MAX_HEX_DIGITS || !isAsciiHex(digits)) {
            return failure("Use 1 to 6 hex digits, like 41 or U+0041");
        }
        return check(Integer.parseInt(digits, 16));
    }

    private static Result check(int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            return failure("Code points stop at U+10FFFF");
        }
        if (Character.getType(codePoint) == Character.SURROGATE) {
            return failure("Surrogates cannot be glyphs");
        }
        if (codePoint == SheetOrder.FILLER) {
            return failure("That code point is reserved for empty slots");
        }
        if (Character.getType(codePoint) == Character.CONTROL) {
            return failure("Control characters cannot be glyphs");
        }
        return new Result(codePoint, null);
    }

    private static boolean isAsciiHex(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean hex = (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private static Result failure(String message) {
        return new Result(-1, message);
    }
}
