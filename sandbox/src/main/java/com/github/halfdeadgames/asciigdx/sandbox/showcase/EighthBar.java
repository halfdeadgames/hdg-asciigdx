package com.github.halfdeadgames.asciigdx.sandbox.showcase;

/** Picks the glyph for each cell of a bar that fills left to right in eighths of a cell. */
public final class EighthBar {

    private static final int EIGHTHS_PER_CELL = 8;
    private static final int FULL_BLOCK = 0x2588;
    private static final int EMPTY = ' ';
    /** Index is the number of filled eighths, 0 to 7. */
    private static final int[] PARTIAL_BLOCKS = {EMPTY, 0x258F, 0x258E, 0x258D, 0x258C, 0x258B, 0x258A, 0x2589};

    private EighthBar() {
    }

    /**
     * @param cellIndex cell to look up, from 0 to {@code cellCount - 1}
     * @param cellCount number of cells in the bar, at least 1
     * @param fraction filled share of the bar; clamped to 0..1
     * @return the code point to draw in that cell
     * @throws IllegalArgumentException if {@code cellCount} is not positive or {@code cellIndex} is out of range
     */
    public static int codePointAt(int cellIndex, int cellCount, float fraction) {
        if (cellCount <= 0) {
            throw new IllegalArgumentException("cellCount must be > 0: " + cellCount);
        }
        if (cellIndex < 0 || cellIndex >= cellCount) {
            throw new IllegalArgumentException("cellIndex out of range 0.." + (cellCount - 1) + ": " + cellIndex);
        }
        int eighthsInCell = totalEighths(cellCount, fraction) - cellIndex * EIGHTHS_PER_CELL;
        if (eighthsInCell <= 0) {
            return EMPTY;
        }
        if (eighthsInCell >= EIGHTHS_PER_CELL) {
            return FULL_BLOCK;
        }
        return PARTIAL_BLOCKS[eighthsInCell];
    }

    private static int totalEighths(int cellCount, float fraction) {
        float clamped = fraction > 0f ? Math.min(fraction, 1f) : 0f;
        return Math.round(clamped * cellCount * EIGHTHS_PER_CELL);
    }
}
