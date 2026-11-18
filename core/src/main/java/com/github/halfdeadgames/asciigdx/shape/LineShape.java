package com.github.halfdeadgames.asciigdx.shape;

import java.util.Objects;

/**
 * A one-cell-wide line between any two cells, at any angle. Both endpoint cells are always
 * included, and a line whose endpoints are the same cell has exactly that one cell. Its center is
 * the midpoint between the centers of the two endpoint cells. How the cells between the endpoints
 * are chosen is set by the {@link LineStyle}, which is fixed when the line is created. See
 * {@link LineStyle} for the cell count and tie rule of each style.
 *
 * <p>Coordinates are cells: columns grow to the right and rows grow downward (y-down). The cell set
 * does not depend on which endpoint is named first. Cells are visited from the top row to the
 * bottom row and from left to right within a row, as guaranteed by {@link CellShape}; every row of
 * the line holds one run of neighboring cells. {@link #forEachCell(ShapeCellVisitor)} allocates
 * nothing.
 *
 * <p>Distances follow {@link ShapeCellVisitor}: they are measured in column widths from the line's
 * center, with row differences multiplied by the cell aspect. The distance fraction is relative
 * to the distance of the endpoint cells and clamped to 1, so it is 1 at the endpoint cells, and a
 * cell slightly off the true line can also report 1. It is 0 for every cell when the line has a
 * single cell.
 *
 * <p>Instances are mutable and not thread-safe. {@link #setEndpoints} takes effect on the next
 * {@code forEachCell} call, and the bounds getters reflect it immediately. Changing the shape from
 * inside its own {@code forEachCell} visitor gives undefined results. If a setter throws, the shape
 * is unchanged.
 *
 * <p>Create instances with {@link #forSquareCells(int, int, int, int, LineStyle)} or
 * {@link #forCellAspect(int, int, int, int, LineStyle, float)}.
 *
 * <p>Example:
 * <pre>{@code
 * LineShape line = LineShape.forSquareCells(0, 0, 9, 3, LineStyle.NO_DIAGONAL_STEPS);
 * line.forEachCell((column, row, distance, distanceFraction) ->
 *         grid.set(column, row, '#', foreground, background));
 * }</pre>
 */
public final class LineShape implements CellShape {

    private final LineStyle style;
    private final float rowHeightPerColumnWidth;
    private int startColumn;
    private int startRow;
    private int endColumn;
    private int endRow;

    private LineShape(int startColumn, int startRow, int endColumn, int endRow, LineStyle style,
            float rowHeightPerColumnWidth) {
        this.style = Objects.requireNonNull(style, "style");
        this.rowHeightPerColumnWidth = ShapeMath.requireValidAspect(rowHeightPerColumnWidth);
        requireEndpoints(startColumn, startRow, endColumn, endRow);
        this.startColumn = startColumn;
        this.startRow = startRow;
        this.endColumn = endColumn;
        this.endRow = endRow;
    }

    /**
     * Creates a line for square cells (an aspect of 1). The aspect only affects reported
     * distances, never which cells are visited; for tall cells, use
     * {@link #forCellAspect(int, int, int, int, LineStyle, float)} so distances match the on-screen
     * spacing. The endpoints may be equal (a one-cell line) and may be given in either order.
     *
     * @param startColumn column of the first endpoint, in cells; may be negative
     * @param startRow    row of the first endpoint, in cells; may be negative
     * @param endColumn   column of the second endpoint, in cells; may be negative
     * @param endRow      row of the second endpoint, in cells; may be negative
     * @param style       how cells between the endpoints are chosen; must not be null
     * @return the new line
     * @throws NullPointerException     if {@code style} is null
     * @throws IllegalArgumentException if any coordinate equals {@link Integer#MAX_VALUE}
     */
    public static LineShape forSquareCells(int startColumn, int startRow, int endColumn,
            int endRow, LineStyle style) {
        return new LineShape(startColumn, startRow, endColumn, endRow, style, 1f);
    }

    /**
     * Creates a line for cells of any aspect. The aspect only affects reported distances, never
     * which cells are visited. The endpoints may be equal (a one-cell line) and may be given in
     * either order.
     *
     * @param startColumn             column of the first endpoint, in cells; may be negative
     * @param startRow                row of the first endpoint, in cells; may be negative
     * @param endColumn               column of the second endpoint, in cells; may be negative
     * @param endRow                  row of the second endpoint, in cells; may be negative
     * @param style                   how cells between the endpoints are chosen; must not be null
     * @param rowHeightPerColumnWidth cell height divided by cell width; finite and greater than 0
     *                                (1 for square cells, 2 for tall 1:2 cells)
     * @return the new line
     * @throws NullPointerException     if {@code style} is null
     * @throws IllegalArgumentException if {@code rowHeightPerColumnWidth} is not finite or not
     *                                  greater than 0, or if any coordinate equals
     *                                  {@link Integer#MAX_VALUE}
     */
    public static LineShape forCellAspect(int startColumn, int startRow, int endColumn, int endRow,
            LineStyle style, float rowHeightPerColumnWidth) {
        return new LineShape(startColumn, startRow, endColumn, endRow, style,
                rowHeightPerColumnWidth);
    }

    /**
     * Moves both endpoints, keeping the style and the aspect.
     *
     * @param startColumn column of the first endpoint, in cells; may be negative
     * @param startRow    row of the first endpoint, in cells; may be negative
     * @param endColumn   column of the second endpoint, in cells; may be negative
     * @param endRow      row of the second endpoint, in cells; may be negative
     * @throws IllegalArgumentException if any coordinate equals {@link Integer#MAX_VALUE}; the
     *                                  shape is then unchanged
     */
    public void setEndpoints(int startColumn, int startRow, int endColumn, int endRow) {
        requireEndpoints(startColumn, startRow, endColumn, endRow);
        this.startColumn = startColumn;
        this.startRow = startRow;
        this.endColumn = endColumn;
        this.endRow = endRow;
    }

    /**
     * Returns the column of the first endpoint.
     *
     * @return the column in cells, possibly negative
     */
    public int getStartColumn() {
        return startColumn;
    }

    /**
     * Returns the row of the first endpoint.
     *
     * @return the row in cells, possibly negative
     */
    public int getStartRow() {
        return startRow;
    }

    /**
     * Returns the column of the second endpoint.
     *
     * @return the column in cells, possibly negative
     */
    public int getEndColumn() {
        return endColumn;
    }

    /**
     * Returns the row of the second endpoint.
     *
     * @return the row in cells, possibly negative
     */
    public int getEndRow() {
        return endRow;
    }

    /**
     * Returns the style chosen at creation.
     *
     * @return the style, never null
     */
    public LineStyle getStyle() {
        return style;
    }

    @Override
    public void forEachCell(ShapeCellVisitor visitor) {
        final boolean startIsTop = startRow < endRow
                || (startRow == endRow && startColumn <= endColumn);
        final int topColumn = startIsTop ? startColumn : endColumn;
        final int topRow = startIsTop ? startRow : endRow;
        final int bottomColumn = startIsTop ? endColumn : startColumn;
        final int bottomRow = startIsTop ? endRow : startRow;
        final long columnSpan = (long) bottomColumn - topColumn;
        final long rowSpan = (long) bottomRow - topRow;
        final float largestDistance = ShapeMath.distance(
                columnSpan / 2.0, rowSpan / 2.0 * rowHeightPerColumnWidth);
        if (style == LineStyle.DIAGONAL_STEPS) {
            walkDiagonalSteps(visitor, topColumn, topRow, columnSpan, rowSpan, largestDistance);
        } else {
            walkNoDiagonalSteps(visitor, topColumn, topRow, columnSpan, rowSpan, largestDistance);
        }
    }

    /**
     * Bresenham walk: the longer axis advances every step, and the shorter axis advances whenever
     * its accumulated error exceeds half a step (a tie waits). The error is scaled by two so that
     * it stays an integer.
     */
    private void walkDiagonalSteps(ShapeCellVisitor visitor, int topColumn, int topRow,
            long columnSpan, long rowSpan, float largestDistance) {
        final long absoluteColumnSpan = Math.abs(columnSpan);
        final boolean columnIsLonger = absoluteColumnSpan >= rowSpan;
        final long longerSteps = Math.max(absoluteColumnSpan, rowSpan);
        final long shorterSteps = Math.min(absoluteColumnSpan, rowSpan);
        final int columnDirection = columnSpan < 0 ? -1 : 1;
        int column = topColumn;
        int row = topRow;
        int runMinColumn = column;
        int runMaxColumn = column;
        long error = longerSteps;
        for (long step = 0; step < longerSteps; step++) {
            error += 2 * shorterSteps;
            final boolean shorterAxisAdvances = error > 2 * longerSteps;
            if (shorterAxisAdvances) {
                error -= 2 * longerSteps;
            }
            final int previousRow = row;
            if (columnIsLonger) {
                column += columnDirection;
                row += shorterAxisAdvances ? 1 : 0;
            } else {
                row++;
                column += shorterAxisAdvances ? columnDirection : 0;
            }
            if (row != previousRow) {
                visitRun(visitor, previousRow, runMinColumn, runMaxColumn, largestDistance);
                runMinColumn = column;
                runMaxColumn = column;
            } else {
                runMinColumn = Math.min(runMinColumn, column);
                runMaxColumn = Math.max(runMaxColumn, column);
            }
        }
        visitRun(visitor, row, runMinColumn, runMaxColumn, largestDistance);
    }

    /**
     * Four-connected walk: each step moves one cell sideways or one cell down. The sign of
     * {@code error} says which side of the true line the next cell center lies on, and it is
     * updated incrementally so the products it stands for never overflow. A tie (the line passes
     * through a cell corner) waits on the shorter axis: a line at least as wide as it is tall moves
     * sideways first, and a steeper line moves down first.
     */
    private void walkNoDiagonalSteps(ShapeCellVisitor visitor, int topColumn, int topRow,
            long columnSpan, long rowSpan, float largestDistance) {
        final long absoluteColumnSpan = Math.abs(columnSpan);
        final int columnDirection = columnSpan < 0 ? -1 : 1;
        final boolean isSteep = rowSpan > absoluteColumnSpan;
        int column = topColumn;
        int row = topRow;
        int runMinColumn = column;
        int runMaxColumn = column;
        long sidewaysSteps = 0;
        long downSteps = 0;
        long error = rowSpan - absoluteColumnSpan;
        while (sidewaysSteps < absoluteColumnSpan || downSteps < rowSpan) {
            if (error < 0 || (error == 0 && !isSteep)) {
                column += columnDirection;
                sidewaysSteps++;
                error += 2 * rowSpan;
                runMinColumn = Math.min(runMinColumn, column);
                runMaxColumn = Math.max(runMaxColumn, column);
            } else {
                visitRun(visitor, row, runMinColumn, runMaxColumn, largestDistance);
                row++;
                downSteps++;
                error -= 2 * absoluteColumnSpan;
                runMinColumn = column;
                runMaxColumn = column;
            }
        }
        visitRun(visitor, row, runMinColumn, runMaxColumn, largestDistance);
    }

    private void visitRun(ShapeCellVisitor visitor, int row, int minColumn, int maxColumn,
            float largestDistance) {
        final double centerColumn = ((long) startColumn + endColumn + 1) / 2.0;
        final double centerRow = ((long) startRow + endRow + 1) / 2.0;
        final double rowDelta = (row + 0.5 - centerRow) * rowHeightPerColumnWidth;
        for (int column = minColumn; column <= maxColumn; column++) {
            final float distance = ShapeMath.distance(column + 0.5 - centerColumn, rowDelta);
            visitor.visit(column, row, distance,
                    ShapeMath.distanceFraction(distance, largestDistance));
        }
    }

    private static void requireEndpoints(int startColumn, int startRow, int endColumn,
            int endRow) {
        ShapeMath.requireBelowMaxValue(startColumn, "startColumn");
        ShapeMath.requireBelowMaxValue(startRow, "startRow");
        ShapeMath.requireBelowMaxValue(endColumn, "endColumn");
        ShapeMath.requireBelowMaxValue(endRow, "endRow");
    }

    @Override
    public int getLeft() {
        return Math.min(startColumn, endColumn);
    }

    @Override
    public int getTop() {
        return Math.min(startRow, endRow);
    }

    @Override
    public int getRight() {
        return Math.max(startColumn, endColumn);
    }

    @Override
    public int getBottom() {
        return Math.max(startRow, endRow);
    }
}
