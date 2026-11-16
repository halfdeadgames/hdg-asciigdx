package com.github.halfdeadgames.asciigdx.shape;

/**
 * A one-cell-thick vertical line. Its center is the middle of the line. It has no fill mode.
 *
 * <p>Coordinates are cells: columns grow to the right and rows grow downward (y-down). Cells are visited
 * from the top row to the bottom row, as guaranteed by {@link CellShape}.
 * {@link #forEachCell(ShapeCellVisitor)} allocates nothing.
 *
 * <p>Instances are mutable and not thread-safe. A setter takes effect on the next
 * {@code forEachCell} call, and the bounds getters reflect it immediately. Changing the shape from inside
 * its own {@code forEachCell} visitor gives undefined results. If a setter throws, the shape is unchanged.
 *
 * <p>Create instances with {@link #forSquareCells(int, int, int)} or
 * {@link #forCellAspect(int, int, int, float)}.
 */
public final class VerticalLineShape implements CellShape {

    private final float rowHeightPerColumnWidth;
    private int column;
    private int top;
    private int length;

    private VerticalLineShape(int column, int top, int length, float rowHeightPerColumnWidth) {
        this.rowHeightPerColumnWidth = ShapeMath.requireValidAspect(rowHeightPerColumnWidth);
        ShapeMath.requireSpan(top, length, "length");
        this.column = column;
        this.top = top;
        this.length = length;
    }

    /**
     * Creates a vertical line for square cells (an aspect of 1). The aspect only affects reported
     * distances, never which cells are visited; for tall cells, use
     * {@link #forCellAspect(int, int, int, float)} so distances match the on-screen spacing.
     *
     * @param column column of the line, in cells; may be negative
     * @param top    top row, in cells; may be negative
     * @param length length in cells, at least 1
     * @return a new vertical line
     * @throws IllegalArgumentException if {@code length} is below 1, or if the last row would reach
     *                                  {@link Integer#MAX_VALUE}
     */
    public static VerticalLineShape forSquareCells(int column, int top, int length) {
        return new VerticalLineShape(column, top, length, 1f);
    }

    /**
     * Creates a vertical line for cells of any shape. The aspect only affects reported distances, never
     * which cells are visited.
     *
     * @param column                  column of the line, in cells; may be negative
     * @param top                     top row, in cells; may be negative
     * @param length                  length in cells, at least 1
     * @param rowHeightPerColumnWidth cell height divided by cell width (2 for tall 1:2 cells); finite and
     *                                above 0
     * @return a new vertical line
     * @throws IllegalArgumentException if {@code rowHeightPerColumnWidth} is not finite or is not above 0,
     *                                  if {@code length} is below 1, or if the last row would reach
     *                                  {@link Integer#MAX_VALUE}
     */
    public static VerticalLineShape forCellAspect(int column, int top, int length,
                                                  float rowHeightPerColumnWidth) {
        return new VerticalLineShape(column, top, length, rowHeightPerColumnWidth);
    }

    /**
     * Moves the line, keeping its length.
     *
     * @param column column of the line, in cells; may be negative
     * @param top    top row, in cells; may be negative
     * @throws IllegalArgumentException if the last row would reach {@link Integer#MAX_VALUE}; the shape is
     *                                  then unchanged
     */
    public void setPosition(int column, int top) {
        ShapeMath.requireSpan(top, length, "length");
        this.column = column;
        this.top = top;
    }

    /**
     * Changes the length, keeping the top cell.
     *
     * @param length length in cells, at least 1
     * @throws IllegalArgumentException if {@code length} is below 1, or if the last row would reach
     *                                  {@link Integer#MAX_VALUE}; the shape is then unchanged
     */
    public void setLength(int length) {
        ShapeMath.requireSpan(top, length, "length");
        this.length = length;
    }

    /**
     * Returns the length.
     *
     * @return the length in cells, at least 1
     */
    public int getLength() {
        return length;
    }

    @Override
    public void forEachCell(ShapeCellVisitor visitor) {
        final float largestDistance = (length - 1) / 2f * rowHeightPerColumnWidth;
        final int bottom = getBottom();
        for (int current = top; current <= bottom; current++) {
            final double rowDelta = Math.abs(current + 0.5 - (top + length / 2.0));
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
        return top;
    }

    @Override
    public int getRight() {
        return column;
    }

    @Override
    public int getBottom() {
        return top + length - 1;
    }
}
