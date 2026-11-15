package com.github.halfdeadgames.asciigdx.sandbox.pond;

/** One of the four lily positions inside a game cell; each maps to one cell of the lily layer. */
public enum LilySlot {
    NORTH_WEST(0, 0),
    NORTH_EAST(1, 0),
    SOUTH_WEST(0, 1),
    SOUTH_EAST(1, 1);

    private final int columnOffset;
    private final int rowOffset;

    LilySlot(int columnOffset, int rowOffset) {
        this.columnOffset = columnOffset;
        this.rowOffset = rowOffset;
    }

    /** Returns 0 for the west column of the slot grid and 1 for the east column. */
    public int columnOffset() {
        return columnOffset;
    }

    /** Returns 0 for the north row of the slot grid and 1 for the south row. */
    public int rowOffset() {
        return rowOffset;
    }
}
