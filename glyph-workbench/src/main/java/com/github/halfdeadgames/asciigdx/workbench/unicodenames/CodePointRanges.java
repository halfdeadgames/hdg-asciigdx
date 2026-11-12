package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

import java.util.Arrays;

/** Lookup in sorted, non-overlapping ranges stored as parallel int arrays. */
final class CodePointRanges {

    private CodePointRanges() {
    }

    /** Returns the index of the range holding the code point, or -1. */
    static int indexOfRangeContaining(int[] firsts, int[] lasts, int codePoint) {
        int found = Arrays.binarySearch(firsts, codePoint);
        if (found >= 0) {
            return found;
        }
        int candidate = -found - 2;
        return candidate >= 0 && codePoint <= lasts[candidate] ? candidate : -1;
    }
}
