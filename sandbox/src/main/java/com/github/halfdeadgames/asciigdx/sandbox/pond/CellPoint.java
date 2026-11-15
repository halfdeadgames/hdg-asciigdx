package com.github.halfdeadgames.asciigdx.sandbox.pond;

/** Reusable output holder for a map position, so per-frame queries allocate nothing. */
public final class CellPoint {

    private int column;
    private int row;

    public int getColumn() {
        return column;
    }

    public int getRow() {
        return row;
    }

    public void set(int column, int row) {
        this.column = column;
        this.row = row;
    }
}
