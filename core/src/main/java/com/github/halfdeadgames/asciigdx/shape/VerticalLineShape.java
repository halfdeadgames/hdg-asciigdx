package com.github.halfdeadgames.asciigdx.shape;

/** A one-cell-thick vertical line. Its center is the middle of the line. It has no fill mode. */
public final class VerticalLineShape implements CellShape {

    private final float rowHeightPerColumnWidth;
    private int column;
    private int row;
    private int length;

    /**
     * Creates a vertical line with an aspect of 1, for square cells. The aspect only affects reported
     * distances, never which cells are visited; for tall cells, use the other constructor so distances match
     * the on-screen spacing.
     *
     * @param column column of the line
     * @param row    top row
     * @param length length in cells, at least 1
     */
    public VerticalLineShape(int column, int row, int length) {
        this(column, row, length, 1f);
    }

    /**
     * Creates a vertical line. The aspect only affects reported distances, never which cells are visited.
     *
     * @param column                  column of the line
     * @param row                     top row
     * @param length                  length in cells, at least 1
     * @param rowHeightPerColumnWidth cell height divided by cell width; finite and above 0
     */
    public VerticalLineShape(int column, int row, int length, float rowHeightPerColumnWidth) {
        this.rowHeightPerColumnWidth = ShapeMath.requireValidAspect(rowHeightPerColumnWidth);
        ShapeMath.requireSpan(row, length, "length");
        this.column = column;
        this.row = row;
        this.length = length;
    }

    /**
     * Moves the line, keeping its length.
     *
     * @param newColumn column of the line
     * @param newRow    top row
     */
    public void setPosition(int newColumn, int newRow) {
        ShapeMath.requireSpan(newRow, length, "length");
        column = newColumn;
        row = newRow;
    }

    /**
     * Changes the length, keeping the top cell.
     *
     * @param newLength length in cells, at least 1
     */
    public void setLength(int newLength) {
        ShapeMath.requireSpan(row, newLength, "length");
        length = newLength;
    }

    /** Returns the length in cells. */
    public int getLength() {
        return length;
    }

    @Override
    public void forEachCell(ShapeCellVisitor visitor) {
        final float largestDistance = (length - 1) / 2f * rowHeightPerColumnWidth;
        final int bottom = getBottom();
        for (int current = row; current <= bottom; current++) {
            final double rowDelta = Math.abs(current + 0.5 - (row + length / 2.0));
            final float distance = (float) (rowDelta * rowHeightPerColumnWidth);
            visitor.visit(column, current, distance, ShapeMath.distanceFraction(distance, largestDistance));
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
        return column;
    }

    @Override
    public int getBottom() {
        return row + length - 1;
    }
}
