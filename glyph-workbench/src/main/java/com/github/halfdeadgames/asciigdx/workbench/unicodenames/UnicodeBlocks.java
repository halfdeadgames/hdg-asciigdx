package com.github.halfdeadgames.asciigdx.workbench.unicodenames;

/** Unicode blocks as sorted, non-overlapping ranges; the lookup allocates nothing. */
final class UnicodeBlocks {

    private final int[] firsts;
    private final int[] lasts;
    private final String[] names;

    UnicodeBlocks(int[] firsts, int[] lasts, String[] names) {
        this.firsts = firsts;
        this.lasts = lasts;
        this.names = names;
    }

    /** Returns the block name, or null if the code point is in no listed block. */
    String blockOf(int codePoint) {
        int index = CodePointRanges.indexOfRangeContaining(firsts, lasts, codePoint);
        return index < 0 ? null : names[index];
    }
}
