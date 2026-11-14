package com.github.halfdeadgames.asciigdx.shape;

/**
 * A one-cell-thick horizontal line. Its center is the middle of the line. It has no fill mode, and it needs
 * no aspect because all of its cells share one row.
 */
public final class HorizontalLineShape implements CellShape {

    private int column;
    private int row;
    private int length;

    /**
     * Creates a horizontal line.
     *
     * @param column leftmost column
     * @param row    row of the line
     * @param length length in cells, at least 1
     */
    public HorizontalLineShape(int column, int row, int length) {
        ShapeMath.requireSpan(column, length, "length");
        this.column = column;
        this.row = row;
        this.length = length;
    }

    /**
     * Moves the line, keeping its length.
     *
     * @param newColumn leftmost column
     * @param newRow    row of the line
     */
    public void setPosition(int newColumn, int newRow) {
        ShapeMath.requireSpan(newColumn, length, "length");
        column = newColumn;
        row = newRow;
    }

    /**
     * Changes the length, keeping the leftmost cell.
     *
     * @param newLength length in cells, at least 1
     */
    public void setLength(int newLength) {
        ShapeMath.requireSpan(column, newLength, "length");
        length = newLength;
    }

    /** Returns the length in cells. */
    public int getLength() {
        return length;
    }

    @Override
    public void forEachCell(ShapeCellVisitor visitor) {
        final float largestDistance = (length - 1) / 2f;
        final int right = getRight();
        for (int current = column; current <= right; current++) {
            final float distance = (float) Math.abs(current + 0.5 - (column + length / 2.0));
            visitor.visit(current, row, distance, ShapeMath.distanceFraction(distance, largestDistance));
        }
    }

    @Override
    public int getLeft() {
        return column;
    }

    @Override
    public int getTop() {
        return row;
    }

    @Override
    public int getRight() {
        return column + length - 1;
    }

    @Override
    public int getBottom() {
        return row;
    }
}
