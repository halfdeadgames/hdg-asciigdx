package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import com.github.halfdeadgames.asciigdx.workbench.fontdata.CodePoints;

/** Reads the hex code points and "first..last" ranges used by the Unicode data files. */
final class HexCodePoints {

    static final int INVALID = -1;

    private static final String RANGE_SEPARATOR = "..";

    private HexCodePoints() {
    }

    /** Returns the code point written as 1 to 8 hex digits, or {@link #INVALID}. */
    static int read(String hex) {
        int value = CodePoints.readHexDigits(hex);
        return Character.isValidCodePoint(value) ? value : INVALID;
    }

    /**
     * Returns {first, last} for "HEX" or "HEX..HEX", or null if the text is not a valid ascending
     * range.
     */
    static int[] readRange(String text) {
        int separator = text.indexOf(RANGE_SEPARATOR);
        if (separator < 0) {
            int single = read(text);
            return single == INVALID ? null : new int[]{single, single};
        }
        int first = read(text.substring(0, separator));
        int last = read(text.substring(separator + RANGE_SEPARATOR.length()));
        if (first == INVALID || last == INVALID || first > last) {
            return null;
        }
        return new int[]{first, last};
    }

    static boolean isRange(String text) {
        return text.contains(RANGE_SEPARATOR);
    }
}
