package com.github.halfdeadgames.asciigdx.shape;

/**
 * A one-cell-thick horizontal line. Its center is the middle of the line. It has no fill mode, and
 * it needs no aspect because all of its cells share one row.
 *
 * <p>Coordinates are cells: columns grow to the right and rows grow downward (y-down). Cells are
 * visited from left to right, as guaranteed by {@link CellShape}.
 * {@link #forEachCell(ShapeCellVisitor)} allocates nothing.
 *
 * <p>Instances are mutable and not thread-safe. A setter takes effect on the next
 * {@code forEachCell} call, and the bounds getters reflect it immediately. Changing the shape from
 * inside its own {@code forEachCell} visitor gives undefined results. If a setter throws, the shape
 * is unchanged.
 */
public final class HorizontalLineShape implements CellShape {

    private int left;
    private int row;
    private int length;

    /**
     * Creates a horizontal line.
     *
     * @param left   leftmost column, in cells; may be negative
     * @param row    row of the line, in cells; may be negative
     * @param length length in cells, at least 1
     * @throws IllegalArgumentException if {@code length} is below 1, or if the last column would
     *                                  reach {@link Integer#MAX_VALUE}
     */
    public HorizontalLineShape(int left, int row, int length) {
        ShapeMath.requireSpan(left, length, "length");
        this.left = left;
        this.row = row;
        this.length = length;
    }

    /**
     * Moves the line, keeping its length.
     *
     * @param left leftmost column, in cells; may be negative
     * @param row  row of the line, in cells; may be negative
     * @throws IllegalArgumentException if the last column would reach {@link Integer#MAX_VALUE};
     *                                  the shape is then unchanged
     */
    public void setPosition(int left, int row) {
        ShapeMath.requireSpan(left, length, "length");
        this.left = left;
        this.row = row;
    }

    /**
     * Changes the length, keeping the leftmost cell.
     *
     * @param length length in cells, at least 1
     * @throws IllegalArgumentException if {@code length} is below 1, or if the last column would
     *                                  reach {@link Integer#MAX_VALUE}; the shape is then unchanged
     */
    public void setLength(int length) {
        ShapeMath.requireSpan(left, length, "length");
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
        final float largestDistance = (length - 1) / 2f;
        final int right = getRight();
        for (int current = left; current <= right; current++) {
            final float distance = (float) Math.abs(current + 0.5 - (left + length / 2.0));
            visitor.visit(current, row, distance,
                    ShapeMath.distanceFraction(distance, largestDistance));
        }
    }

    @Override
    public int getLeft() {
        return left;
    }

    @Override
    public int getTop() {
        return row;
    }

    @Override
    public int getRight() {
        return left + length - 1;
    }

    @Override
    public int getBottom() {
        return row;
    }
}
