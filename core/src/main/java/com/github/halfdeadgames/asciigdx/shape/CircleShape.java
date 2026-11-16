package com.github.halfdeadgames.asciigdx.shape;

import java.util.Objects;

/**
 * A disc of cells, filled or outlined. A cell is inside when the aspect-corrected distance from its center
 * to the circle's center is at most the radius. If no cell center is inside, the shape is the single cell
 * containing the circle's center, so it is never empty. A single-cell shape always reports a distance
 * fraction of 0.
 *
 * <p>The outline consists of the inside cells that have at least one of their four side neighbors outside,
 * which gives a closed ring one cell thick.
 *
 * <p>Unlike the other shapes, the aspect (cell height divided by cell width) is required and has no
 * default, because it changes the circle's shape: tall cells give a circle spanning fewer rows.
 *
 * <p>Tip: a whole-number radius leaves a single cell sticking out at each of the four axis points of the
 * outline. A radius about 0.4 below a whole number (for example, 6.6 instead of 7) gives a rounder outline.
 *
 * <p>The center plus the reach (the radius in columns, the radius divided by the aspect in rows) must stay
 * within {@link #COORDINATE_LIMIT} cells of the origin; circles that reach farther are rejected.
 *
 * <p>Coordinates are cells: columns grow to the right and rows grow downward (y-down). Cells are visited row
 * by row from top to bottom and left to right within a row, as guaranteed by {@link CellShape}.
 * {@link #forEachCell(ShapeCellVisitor)} allocates nothing.
 *
 * <p>Instances are mutable and not thread-safe. A setter takes effect on the next
 * {@code forEachCell} call, and the bounds getters reflect it immediately. Changing the shape from inside
 * its own {@code forEachCell} visitor gives undefined results. If a setter throws, the shape is unchanged.
 */
public final class CircleShape implements CellShape {

    /**
     * Largest absolute cell coordinate (center plus reach) a circle may span: 2^24, the largest magnitude up
     * to which a float represents every integer exactly.
     */
    public static final int COORDINATE_LIMIT = 1 << 24;

    private final float rowHeightPerColumnWidth;
    private final ShapeFill fill;
    private float centerX;
    private float centerY;
    private float radius;

    private boolean usesCenterFallback;
    private int fallbackColumn;
    private int fallbackRow;
    private int left;
    private int top;
    private int right;
    private int bottom;
    private float largestDistance;

    /**
     * Creates a circle. The aspect has no default because it changes which cells make up the circle; read
     * it from the font's cell size (cell height divided by cell width).
     *
     * @param centerX                 center in column units; 10.5f is the middle of column 10
     * @param centerY                 center in row units; 10.5f is the middle of row 10
     * @param radius                  radius in column widths; finite and above 0
     * @param fill                    filled or outline; must not be {@code null}
     * @param rowHeightPerColumnWidth cell height divided by cell width (2 for tall 1:2 cells); finite and
     *                                above 0
     * @throws NullPointerException     if {@code fill} is {@code null}
     * @throws IllegalArgumentException if {@code rowHeightPerColumnWidth} or {@code radius} is not finite or
     *                                  is not above 0, if {@code centerX} or {@code centerY} is not finite,
     *                                  or if the circle's reach would go beyond {@link #COORDINATE_LIMIT}
     */
    public CircleShape(float centerX, float centerY, float radius, ShapeFill fill,
                       float rowHeightPerColumnWidth) {
        this.fill = Objects.requireNonNull(fill, "fill");
        this.rowHeightPerColumnWidth = ShapeMath.requireValidAspect(rowHeightPerColumnWidth);
        requireReach(centerX, centerY, requireValidRadius(radius));
        this.centerX = centerX;
        this.centerY = centerY;
        this.radius = radius;
        recompute();
    }

    /**
     * Moves the circle, keeping its radius.
     *
     * @param centerX center in column units; finite
     * @param centerY center in row units; finite
     * @throws IllegalArgumentException if {@code centerX} or {@code centerY} is not finite, or if the
     *                                  circle's reach would go beyond {@link #COORDINATE_LIMIT}; the shape
     *                                  is then unchanged
     */
    public void setCenter(float centerX, float centerY) {
        requireReach(centerX, centerY, radius);
        this.centerX = centerX;
        this.centerY = centerY;
        recompute();
    }

    /**
     * Changes the radius, keeping the center.
     *
     * @param radius radius in column widths; finite and above 0
     * @throws IllegalArgumentException if {@code radius} is not finite or is not above 0, or if the
     *                                  circle's reach would go beyond {@link #COORDINATE_LIMIT}; the shape
     *                                  is then unchanged
     */
    public void setRadius(float radius) {
        requireReach(centerX, centerY, requireValidRadius(radius));
        this.radius = radius;
        recompute();
    }

    /**
     * Returns the center column.
     *
     * @return the center in column units
     */
    public float getCenterX() {
        return centerX;
    }

    /**
     * Returns the center row.
     *
     * @return the center in row units
     */
    public float getCenterY() {
        return centerY;
    }

    /**
     * Returns the radius.
     *
     * @return the radius in column widths
     */
    public float getRadius() {
        return radius;
    }

    /**
     * Returns the fill mode.
     *
     * @return whether the circle is filled or outlined; never {@code null}
     */
    public ShapeFill getFill() {
        return fill;
    }

    @Override
    public void forEachCell(ShapeCellVisitor visitor) {
        for (int row = top; row <= bottom; row++) {
            for (int column = left; column <= right; column++) {
                if (isVisited(column, row)) {
                    final float distance = distanceToCenter(column, row);
                    visitor.visit(column, row, distance, ShapeMath.distanceFraction(distance, largestDistance));
                }
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
        return right;
    }

    @Override
    public int getBottom() {
        return bottom;
    }

    private static float requireValidRadius(float radius) {
        if (!Float.isFinite(radius) || radius <= 0f) {
            throw new IllegalArgumentException("radius must be finite and > 0 but was " + radius);
        }
        return radius;
    }

    private void requireReach(float candidateCenterX, float candidateCenterY, float candidateRadius) {
        ShapeMath.requireFinite(candidateCenterX, "centerX");
        ShapeMath.requireFinite(candidateCenterY, "centerY");
        final double rowReach = candidateRadius / rowHeightPerColumnWidth;
        final boolean columnsInRange = Math.abs((double) candidateCenterX) + candidateRadius <= COORDINATE_LIMIT;
        final boolean rowsInRange = Math.abs((double) candidateCenterY) + rowReach <= COORDINATE_LIMIT;
        if (!columnsInRange || !rowsInRange) {
            throw new IllegalArgumentException("circle must stay within +/-" + COORDINATE_LIMIT
                    + " cells but had center (" + candidateCenterX + ", " + candidateCenterY
                    + ") and radius " + candidateRadius);
        }
    }

    private static int floorToInt(double value) {
        return (int) Math.floor(value);
    }

    private boolean isVisited(int column, int row) {
        if (!isMember(column, row)) {
            return false;
        }
        if (fill == ShapeFill.FILLED) {
            return true;
        }
        return !isMember(column - 1, row) || !isMember(column + 1, row)
                || !isMember(column, row - 1) || !isMember(column, row + 1);
    }

    private boolean isMember(int column, int row) {
        if (usesCenterFallback) {
            return column == fallbackColumn && row == fallbackRow;
        }
        final double columnDelta = column + 0.5 - centerX;
        final double rowDelta = (row + 0.5 - centerY) * rowHeightPerColumnWidth;
        return columnDelta * columnDelta + rowDelta * rowDelta <= (double) radius * radius;
    }

    private float distanceToCenter(int column, int row) {
        final double columnDelta = column + 0.5 - centerX;
        final double rowDelta = (row + 0.5 - centerY) * rowHeightPerColumnWidth;
        return ShapeMath.distance(columnDelta, rowDelta);
    }

    private void recompute() {
        usesCenterFallback = false;
        scanVisitedCells();
        if (left > right) {
            useCenterCellOnly();
        }
    }

    private void scanVisitedCells() {
        final int scanLeft = floorToInt(centerX - radius);
        final int scanRight = floorToInt(centerX + radius);
        final int scanTop = floorToInt(centerY - radius / rowHeightPerColumnWidth);
        final int scanBottom = floorToInt(centerY + radius / rowHeightPerColumnWidth);
        left = Integer.MAX_VALUE;
        top = Integer.MAX_VALUE;
        right = Integer.MIN_VALUE;
        bottom = Integer.MIN_VALUE;
        largestDistance = 0f;
        int visitedCount = 0;
        for (int row = scanTop; row <= scanBottom; row++) {
            for (int column = scanLeft; column <= scanRight; column++) {
                if (isVisited(column, row)) {
                    includeInBounds(column, row);
                    largestDistance = Math.max(largestDistance, distanceToCenter(column, row));
                    visitedCount++;
                }
            }
        }
        if (visitedCount == 1) {
            largestDistance = 0f;
        }
    }

    private void includeInBounds(int column, int row) {
        left = Math.min(left, column);
        top = Math.min(top, row);
        right = Math.max(right, column);
        bottom = Math.max(bottom, row);
    }

    private void useCenterCellOnly() {
        usesCenterFallback = true;
        fallbackColumn = floorToInt(centerX);
        fallbackRow = floorToInt(centerY);
        left = fallbackColumn;
        right = fallbackColumn;
        top = fallbackRow;
        bottom = fallbackRow;
        largestDistance = 0f;
    }
}
