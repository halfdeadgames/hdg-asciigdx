package com.github.halfdeadgames.asciigdx.shape;

import java.util.Objects;

/**
 * A rectangle of cells, filled or outlined. Its center is the geometric center of the rectangle.
 *
 * <p>Coordinates are cells: columns grow to the right and rows grow downward (y-down). Cells are visited row
 * by row from top to bottom and left to right within a row, as guaranteed by {@link CellShape}.
 * {@link #forEachCell(ShapeCellVisitor)} allocates nothing.
 *
 * <p>Instances are mutable and not thread-safe. A setter takes effect on the next
 * {@code forEachCell} call, and the bounds getters reflect it immediately. Changing the shape from inside
 * its own {@code forEachCell} visitor gives undefined results. If a setter throws, the shape is unchanged.
 *
 * <p>Create instances with {@link #forSquareCells(int, int, int, int, ShapeFill)} or
 * {@link #forCellAspect(int, int, int, int, ShapeFill, float)}.
 */
public final class RectangleShape implements CellShape {

    private final float rowHeightPerColumnWidth;
    private final ShapeFill fill;
    private int left;
    private int top;
    private int width;
    private int height;
    private float largestDistance;

    private RectangleShape(int left, int top, int width, int height, ShapeFill fill,
                           float rowHeightPerColumnWidth) {
        this.fill = Objects.requireNonNull(fill, "fill");
        this.rowHeightPerColumnWidth = ShapeMath.requireValidAspect(rowHeightPerColumnWidth);
        ShapeMath.requireSpan(left, width, "width");
        ShapeMath.requireSpan(top, height, "height");
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        refreshLargestDistance();
    }

    /**
     * Creates a rectangle for square cells (an aspect of 1). The aspect only affects reported distances,
     * never which cells are visited; for tall cells, use
     * {@link #forCellAspect(int, int, int, int, ShapeFill, float)} so distances match the on-screen spacing.
     *
     * @param left   leftmost column, in cells; may be negative
     * @param top    top row, in cells; may be negative
     * @param width  width in cells, at least 1
     * @param height height in cells, at least 1
     * @param fill   filled or outline; must not be {@code null}
     * @return a new rectangle
     * @throws NullPointerException     if {@code fill} is {@code null}
     * @throws IllegalArgumentException if {@code width} or {@code height} is below 1, or if the rectangle's
     *                                  last column or row would reach {@link Integer#MAX_VALUE}
     */
    public static RectangleShape forSquareCells(int left, int top, int width, int height, ShapeFill fill) {
        return new RectangleShape(left, top, width, height, fill, 1f);
    }

    /**
     * Creates a rectangle for cells of any shape. The aspect only affects reported distances, never which
     * cells are visited.
     *
     * @param left                    leftmost column, in cells; may be negative
     * @param top                     top row, in cells; may be negative
     * @param width                   width in cells, at least 1
     * @param height                  height in cells, at least 1
     * @param fill                    filled or outline; must not be {@code null}
     * @param rowHeightPerColumnWidth cell height divided by cell width (2 for tall 1:2 cells); finite and
     *                                above 0
     * @return a new rectangle
     * @throws NullPointerException     if {@code fill} is {@code null}
     * @throws IllegalArgumentException if {@code rowHeightPerColumnWidth} is not finite or is not above 0,
     *                                  if {@code width} or {@code height} is below 1, or if the rectangle's
     *                                  last column or row would reach {@link Integer#MAX_VALUE}
     */
    public static RectangleShape forCellAspect(int left, int top, int width, int height, ShapeFill fill,
                                               float rowHeightPerColumnWidth) {
        return new RectangleShape(left, top, width, height, fill, rowHeightPerColumnWidth);
    }

    /**
     * Moves the rectangle, keeping its size.
     *
     * @param left leftmost column, in cells; may be negative
     * @param top  top row, in cells; may be negative
     * @throws IllegalArgumentException if the rectangle's last column or row would reach
     *                                  {@link Integer#MAX_VALUE}; the shape is then unchanged
     */
    public void setPosition(int left, int top) {
        ShapeMath.requireSpan(left, width, "width");
        ShapeMath.requireSpan(top, height, "height");
        this.left = left;
        this.top = top;
    }

    /**
     * Resizes the rectangle, keeping its top-left cell.
     *
     * @param width  width in cells, at least 1
     * @param height height in cells, at least 1
     * @throws IllegalArgumentException if {@code width} or {@code height} is below 1, or if the rectangle's
     *                                  last column or row would reach {@link Integer#MAX_VALUE}; the shape
     *                                  is then unchanged
     */
    public void setSize(int width, int height) {
        ShapeMath.requireSpan(left, width, "width");
        ShapeMath.requireSpan(top, height, "height");
        this.width = width;
        this.height = height;
        refreshLargestDistance();
    }

    /**
     * Returns the width.
     *
     * @return the width in cells, at least 1
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height.
     *
     * @return the height in cells, at least 1
     */
    public int getHeight() {
        return height;
    }

    /**
     * Returns the fill mode.
     *
     * @return whether the rectangle is filled or outlined; never {@code null}
     */
    public ShapeFill getFill() {
        return fill;
    }

    @Override
    public void forEachCell(ShapeCellVisitor visitor) {
        final int right = getRight();
        final int bottom = getBottom();
        for (int row = top; row <= bottom; row++) {
            if (isHollowRow(row)) {
                visitCell(visitor, left, row);
                if (right != left) {
                    visitCell(visitor, right, row);
                }
                continue;
            }
            for (int column = left; column <= right; column++) {
                visitCell(visitor, column, row);
            }
        }
    }

    @Override
    public int getLeft() {
        return left;
    }

    @Override
    public int getTop() {
        return top;
    }

    @Override
    public int getRight() {
        return left + width - 1;
    }

    @Override
    public int getBottom() {
        return top + height - 1;
    }

    private boolean isHollowRow(int row) {
        return fill == ShapeFill.OUTLINE && row != top && row != getBottom();
    }

    private void visitCell(ShapeCellVisitor visitor, int column, int row) {
        final float distance = distanceToCenter(column, row);
        visitor.visit(column, row, distance, ShapeMath.distanceFraction(distance, largestDistance));
    }

    private float distanceToCenter(int column, int row) {
        final double columnDelta = column + 0.5 - (left + width / 2.0);
        final double rowDelta = row + 0.5 - (top + height / 2.0);
        return ShapeMath.distance(columnDelta, rowDelta * rowHeightPerColumnWidth);
    }

    // The corner cells are the farthest from the center and belong to both fill modes.
    private void refreshLargestDistance() {
        largestDistance = distanceToCenter(left, top);
    }
}
