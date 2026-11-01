package com.github.halfdeadgames.asciigdx.workbench;

/**
 * Decides which column the context area shows: the hovered one, else the last one the user chose,
 * else the first candidate. Column 0 is the original.
 */
final class ViewedColumn {

    private static final int NONE = -1;

    private int chosen = NONE;

    void choose(int column) {
        chosen = column;
    }

    void reset() {
        chosen = NONE;
    }

    /** Always a valid column index for {@code count} columns, or 0 when there are none. */
    int resolve(int hovered, int count) {
        int last = count - 1;
        if (last < 0) {
            return 0;
        }
        if (hovered >= 0) {
            return Math.min(hovered, last);
        }
        return Math.min(chosen >= 0 ? chosen : 1, last);
    }
}
