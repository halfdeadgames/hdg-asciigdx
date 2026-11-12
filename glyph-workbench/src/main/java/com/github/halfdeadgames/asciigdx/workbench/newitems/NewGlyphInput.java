package com.github.halfdeadgames.asciigdx.workbench.newitems;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CodePoints;
import com.github.halfdeadgames.asciigdx.workbench.fontdata.SheetOrder;

/** Turns text typed into the "new glyph" prompt into a code point. */
final class NewGlyphInput {

    private static final int PREFIX_LENGTH = 2;

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
     * Reads a hex code point such as {@code 41} or {@code U+0041}. Every input is hex, so a lone {@code A}
     * means U+000A.
     *
     * @param typed the raw text from the prompt
     * @return the code point, or an error message
     */
    static Result parse(String typed) {
        var stripped = typed == null ? "" : typed.strip();
        if (stripped.isEmpty()) {
            return failure("Type a hex code like 41 or U+0041");
        }
        boolean hasPrefix = stripped.startsWith("U+") || stripped.startsWith("u+");
        return parseHex(stripped, hasPrefix);
    }

    private static Result parseHex(String stripped, boolean hasPrefix) {
        if (hasPrefix && stripped.length() == PREFIX_LENGTH) {
            return failure("Add hex digits after U+");
        }
        int code = hasPrefix ? CodePoints.readUPlus(stripped) : CodePoints.readHexDigits(stripped);
        if (code == CodePoints.NOT_A_CODE) {
            return failure("Use 1 to 8 hex digits, like 41 or U+0041");
        }
        return check(code);
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

    private static Result failure(String message) {
        return new Result(-1, message);
    }
}
