package com.github.halfdeadgames.asciigdx.shape;

import java.util.Objects;

/** A rectangle of cells, filled or outlined. Its center is the geometric center of the rectangle. */
public final class RectangleShape implements CellShape {

    private final float rowHeightPerColumnWidth;
    private final ShapeFill fill;
    private int left;
    private int top;
    private int width;
    private int height;
    private float largestDistance;

    /**
     * Creates a rectangle with an aspect of 1, for square cells. The aspect only affects reported distances,
     * never which cells are visited; for tall cells, use the other constructor so distances match the
     * on-screen spacing.
     *
     * @param left   leftmost column
     * @param top    top row
     * @param width  width in cells, at least 1
     * @param height height in cells, at least 1
     * @param fill   filled or outline
     */
    public RectangleShape(int left, int top, int width, int height, ShapeFill fill) {
        this(left, top, width, height, fill, 1f);
    }

    /**
     * Creates a rectangle. The aspect only affects reported distances, never which cells are visited.
     *
     * @param left                    leftmost column
     * @param top                     top row
     * @param width                   width in cells, at least 1
     * @param height                  height in cells, at least 1
     * @param fill                    filled or outline
     * @param rowHeightPerColumnWidth cell height divided by cell width; finite and above 0
     */
    public RectangleShape(int left, int top, int width, int height, ShapeFill fill,
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
     * Moves the rectangle, keeping its size.
     *
     * @param newLeft leftmost column
     * @param newTop  top row
     */
    public void setPosition(int newLeft, int newTop) {
        ShapeMath.requireSpan(newLeft, width, "width");
        ShapeMath.requireSpan(newTop, height, "height");
        left = newLeft;
        top = newTop;
    }

    /**
     * Resizes the rectangle, keeping its top-left cell.
     *
     * @param newWidth  width in cells, at least 1
     * @param newHeight height in cells, at least 1
     */
    public void setSize(int newWidth, int newHeight) {
        ShapeMath.requireSpan(left, newWidth, "width");
        ShapeMath.requireSpan(top, newHeight, "height");
        width = newWidth;
        height = newHeight;
        refreshLargestDistance();
    }

    /** Returns the width in cells. */
    public int getWidth() {
        return width;
    }

    /** Returns the height in cells. */
    public int getHeight() {
        return height;
    }

    /** Returns whether the rectangle is filled or outlined. */
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
