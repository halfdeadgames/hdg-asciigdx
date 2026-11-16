package com.github.halfdeadgames.asciigdx.fit;

import com.badlogic.gdx.utils.viewport.ScalingViewport;

/**
 * Result of fitting a glyph grid into a window: a whole-number pixel scale, the number of columns
 * and rows that fit at that scale, and the leftover window pixels on each axis.
 *
 * <p>All window sizes and leftovers are in pixels. Column and row counts are in cells. Instances
 * are immutable and safe to share. Create one with
 * {@link #calculate(int, int, int, int, int, int)} whenever the window size changes.
 *
 * <p>For a non-empty window (width and height both above 0), the scale is the largest whole number
 * that still leaves room for the requested minimum columns and rows, capped at an optional maximum
 * scale, and never less than 1. A lower cap leaves room for more columns and rows. If even scale 1
 * cannot hold the minimum, the grid is smaller than the minimum and {@link #isMinimumMet()} is
 * false.
 *
 * <p>For an empty window (width or height of 0 or less), the scale, columns, rows, and leftovers
 * are all 0. A window that is non-empty but smaller than one cell has scale 1 and 0 columns or
 * rows. In both cases {@link #isEmpty()} is true. A {@code TerminalGrid} cannot have zero size, so
 * check {@link #isEmpty()} before building a grid from {@link #getColumns()} and
 * {@link #getRows()}.
 */
public final class GridFit {

    private final int windowWidth;
    private final int windowHeight;
    private final int cellWidth;
    private final int cellHeight;
    private final int minColumns;
    private final int minRows;
    private final int scale;
    private final int columns;
    private final int rows;
    private final int leftoverX;
    private final int leftoverY;

    private GridFit(int windowWidth, int windowHeight, int cellWidth, int cellHeight,
            int minColumns, int minRows, int scale, int columns, int rows,
            int leftoverX, int leftoverY) {
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.minColumns = minColumns;
        this.minRows = minRows;
        this.scale = scale;
        this.columns = columns;
        this.rows = rows;
        this.leftoverX = leftoverX;
        this.leftoverY = leftoverY;
    }

    /**
     * Fits cells of the given size into the window with no scale limit. Equivalent to
     * {@link #calculate(int, int, int, int, int, int, int)} with a maximum scale of
     * {@link Integer#MAX_VALUE}. Call it on resize, not per frame.
     *
     * @param windowWidth window width in pixels; 0 or less gives an empty result
     * @param windowHeight window height in pixels; 0 or less gives an empty result
     * @param cellWidth unscaled cell width in pixels; must be above 0
     * @param cellHeight unscaled cell height in pixels; must be above 0
     * @param minColumns minimum columns to leave room for, in cells; must be above 0
     * @param minRows minimum rows to leave room for, in cells; must be above 0
     * @return the fit; never null
     * @throws IllegalArgumentException if a cell size or minimum is zero or negative
     */
    public static GridFit calculate(int windowWidth, int windowHeight, int cellWidth,
            int cellHeight,
            int minColumns, int minRows) {
        return calculate(windowWidth, windowHeight, cellWidth, cellHeight,
                minColumns, minRows, Integer.MAX_VALUE);
    }

    /**
     * Fits cells of the given size into the window. For a non-empty window, the scale is the
     * largest whole number that still fits the minimum columns and rows, capped at
     * {@code maxScale}, and never below 1. A lower cap leaves room for more columns and rows. If
     * the minimum does not fit even at scale 1, the grid is smaller than the minimum and
     * {@link #isMinimumMet()} is false.
     *
     * <p>If the window width or height is 0 or less, the result has scale 0, 0 columns, 0 rows,
     * and 0 leftovers. Call this method on resize, not per frame.
     *
     * @param windowWidth window width in pixels; 0 or less gives an empty result
     * @param windowHeight window height in pixels; 0 or less gives an empty result
     * @param cellWidth unscaled cell width in pixels; must be above 0
     * @param cellHeight unscaled cell height in pixels; must be above 0
     * @param minColumns minimum columns to leave room for, in cells; must be above 0
     * @param minRows minimum rows to leave room for, in cells; must be above 0
     * @param maxScale largest whole-number scale allowed; must be above 0
     * @return the fit; never null
     * @throws IllegalArgumentException if a cell size, minimum, or maxScale is zero or negative
     */
    public static GridFit calculate(int windowWidth, int windowHeight, int cellWidth,
            int cellHeight,
            int minColumns, int minRows, int maxScale) {
        if (cellWidth <= 0 || cellHeight <= 0 || minColumns <= 0 || minRows <= 0 || maxScale <= 0) {
            throw new IllegalArgumentException(
                    "Cell size, minimums, and max scale must be positive: cellWidth="
                            + cellWidth + ", cellHeight=" + cellHeight
                            + ", minColumns=" + minColumns + ", minRows=" + minRows
                            + ", maxScale=" + maxScale);
        }
        if (windowWidth <= 0 || windowHeight <= 0) {
            return new GridFit(windowWidth, windowHeight, cellWidth, cellHeight,
                    minColumns, minRows, 0, 0, 0, 0, 0);
        }
        long scaleByWidth = windowWidth / ((long) cellWidth * minColumns);
        long scaleByHeight = windowHeight / ((long) cellHeight * minRows);
        long fittingScale = Math.min(Math.min(scaleByWidth, scaleByHeight), maxScale);
        int scale = (int) Math.max(1L, fittingScale);
        long scaledCellWidth = (long) cellWidth * scale;
        long scaledCellHeight = (long) cellHeight * scale;
        int columns = (int) (windowWidth / scaledCellWidth);
        int rows = (int) (windowHeight / scaledCellHeight);
        int leftoverX = (int) (windowWidth - columns * scaledCellWidth);
        int leftoverY = (int) (windowHeight - rows * scaledCellHeight);
        return new GridFit(windowWidth, windowHeight, cellWidth, cellHeight,
                minColumns, minRows, scale, columns, rows, leftoverX, leftoverY);
    }

    /**
     * Returns the whole-number factor each cell pixel is enlarged by on screen.
     *
     * @return the scale: 0 for a window with width or height of 0 or less, otherwise 1 or more
     */
    public int getScale() {
        return scale;
    }

    /**
     * Returns how many columns fit in the window at this scale.
     *
     * @return the column count in cells; 0 if the window is empty or narrower than one scaled cell
     */
    public int getColumns() {
        return columns;
    }

    /**
     * Returns how many rows fit in the window at this scale.
     *
     * @return the row count in cells; 0 if the window is empty or shorter than one scaled cell
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns the unused window width: the pixels the grid does not cover horizontally, summed
     * over both sides. A game that centers the grid splits this value itself.
     *
     * @return the unused width in pixels; 0 or more, and less than one scaled cell width unless
     *     the window is empty
     */
    public int getLeftoverWidth() {
        return leftoverX;
    }

    /**
     * Returns the unused window height: the pixels the grid does not cover vertically, summed
     * over top and bottom. A game that centers the grid splits this value itself.
     *
     * @return the unused height in pixels; 0 or more, and less than one scaled cell height unless
     *     the window is empty
     */
    public int getLeftoverHeight() {
        return leftoverY;
    }

    /**
     * Returns the unscaled cell width this fit was calculated with.
     *
     * @return the cell width in pixels; above 0
     */
    public int getCellWidth() {
        return cellWidth;
    }

    /**
     * Returns the unscaled cell height this fit was calculated with.
     *
     * @return the cell height in pixels; above 0
     */
    public int getCellHeight() {
        return cellHeight;
    }

    /**
     * Returns the grid width in unscaled pixels ({@code columns * cellWidth}). This matches the
     * world width a libGDX viewport uses before scaling.
     *
     * @return the world width in pixels; 0 if there are no columns
     */
    public int getWorldWidth() {
        return columns * cellWidth;
    }

    /**
     * Returns the grid height in unscaled pixels ({@code rows * cellHeight}). This matches the
     * world height a libGDX viewport uses before scaling.
     *
     * @return the world height in pixels; 0 if there are no rows
     */
    public int getWorldHeight() {
        return rows * cellHeight;
    }

    /**
     * Tells whether the grid has at least the requested minimum columns and rows.
     *
     * @return true if columns and rows both reach their minimums; always false for an empty window
     */
    public boolean isMinimumMet() {
        return columns >= minColumns && rows >= minRows;
    }

    /**
     * Tells whether no column or no row fits, so no grid can be built from this fit.
     *
     * @return true if the column count or the row count is 0
     */
    public boolean isEmpty() {
        return columns == 0 || rows == 0;
    }

    /**
     * Configures the viewport to draw this grid at exactly this scale, then updates it. Replaces
     * the viewport's {@code Scaling} with an exact whole-number scaling, sets the viewport's world
     * size to {@link #getWorldWidth()} by {@link #getWorldHeight()}, and calls
     * {@code viewport.update(windowWidth, windowHeight, centerCamera)} with the window size this
     * fit was calculated for.
     *
     * <p>Does nothing when {@link #isEmpty()} is true. Creates one small object per call, so call
     * it on resize, not per frame. Call it on the GL/render thread, because the viewport update
     * changes the GL viewport.
     *
     * @param viewport the viewport to configure; must not be null
     * @param centerCamera true to center the viewport's camera on the world, false to leave the
     *     camera position unchanged
     * @throws NullPointerException if the viewport is null and this fit is not empty
     */
    public void applyTo(ScalingViewport viewport, boolean centerCamera) {
        if (isEmpty()) {
            return;
        }
        viewport.setScaling(new ExactScaling(scale));
        viewport.setWorldSize(getWorldWidth(), getWorldHeight());
        viewport.update(windowWidth, windowHeight, centerCamera);
    }
}
