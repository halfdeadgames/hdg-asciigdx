package com.github.halfdeadgames.asciigdx.workbench;

/** Moves a cursor over a grid of cells, skipping blank ones. Cells are numbered row by row. */
public final class CursorNavigator {

    private CursorNavigator() {
    }

    /**
     * Moves one step in a direction, jumping over blank cells. Moving past an edge wraps to the
     * other end of the same row (horizontal) or column (vertical).
     *
     * @param blank one flag per cell, {@code columns * rows} long
     * @param dx horizontal direction, -1, 0, or 1
     * @param dy vertical direction, -1, 0, or 1
     * @return the next non-blank cell in that direction, or {@code index} if the whole line is blank
     */
    public static int move(boolean[] blank, int columns, int rows, int index, int dx, int dy) {
        int column = index % columns;
        int row = index / columns;
        int steps = dx != 0 ? columns - 1 : rows - 1;
        for (int step = 0; step < steps; step++) {
            column = Math.floorMod(column + dx, columns);
            row = Math.floorMod(row + dy, rows);
            int candidate = row * columns + column;
            if (!blank[candidate]) {
                return candidate;
            }
        }
        return index;
    }

    /** Returns the first non-blank cell in reading order, or -1 if every cell is blank. */
    public static int firstNonBlank(boolean[] blank) {
        for (int i = 0; i < blank.length; i++) {
            if (!blank[i]) {
                return i;
            }
        }
        return -1;
    }
}
