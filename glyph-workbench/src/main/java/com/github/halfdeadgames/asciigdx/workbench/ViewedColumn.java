package com.github.halfdeadgames.asciigdx.workbench;

/**
 * Decides which column the context area shows: the hovered one, else the last one the user chose,
 * else the first candidate. Column 0 is the original.
 */
final class ViewedColumn {

    private static final int NONE = -1;

    private int chosen = NONE;
    private int preselected = NONE;

    void choose(int column) {
        chosen = column;
    }

    void reset() {
        chosen = NONE;
        preselected = NONE;
    }

    /** The column shown until the user hovers or chooses one; a negative value means the first candidate. */
    void setDefault(int column) {
        preselected = column;
    }

    /**
     * Column (1-based, after the original) of the saved pick if it is offered, else the hand version,
     * else the nearest version; -1 when none of them is offered.
     */
    static int defaultColumn(String[] picks, String savedPick, String handId, String nearestId) {
        String[] preference = {savedPick, handId, nearestId};
        for (String wanted : preference) {
            for (int i = 0; wanted != null && i < picks.length; i++) {
                if (picks[i].equals(wanted)) {
                    return i + 1;
                }
            }
        }
        return NONE;
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
        int fallback = preselected >= 0 ? preselected : 1;
        return Math.min(chosen >= 0 ? chosen : fallback, last);
    }
}
