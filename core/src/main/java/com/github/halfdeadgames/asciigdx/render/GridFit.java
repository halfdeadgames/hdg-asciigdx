package com.github.halfdeadgames.asciigdx.render;

import com.badlogic.gdx.utils.viewport.ScalingViewport;

/**
 * Result of fitting a glyph grid into a window: a whole-number pixel scale, the number of columns
 * and rows that fit at that scale, and the leftover screen pixels on each axis.
 *
 * <p>The scale is the largest one that still leaves room for the requested minimum columns and
 * rows, and never less than 1. A {@code TerminalGrid} cannot have zero size, so check
 * {@link #isEmpty()} before building a grid from {@link #getColumns()} and {@link #getRows()}.
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
     * Fits cells of the given size into the window.
     *
     * @throws IllegalArgumentException if a cell size or minimum is zero or negative
     */
    public static GridFit calculate(int windowWidth, int windowHeight, int cellWidth, int cellHeight,
                                    int minColumns, int minRows) {
        if (cellWidth <= 0 || cellHeight <= 0 || minColumns <= 0 || minRows <= 0) {
            throw new IllegalArgumentException("Cell size and minimums must be positive: cellWidth="
                    + cellWidth + ", cellHeight=" + cellHeight
                    + ", minColumns=" + minColumns + ", minRows=" + minRows);
        }
        if (windowWidth <= 0 || windowHeight <= 0) {
            return new GridFit(windowWidth, windowHeight, cellWidth, cellHeight,
                    minColumns, minRows, 0, 0, 0, 0, 0);
        }
        long scaleByWidth = windowWidth / ((long) cellWidth * minColumns);
        long scaleByHeight = windowHeight / ((long) cellHeight * minRows);
        int scale = (int) Math.max(1L, Math.min(scaleByWidth, scaleByHeight));
        long scaledCellWidth = (long) cellWidth * scale;
        long scaledCellHeight = (long) cellHeight * scale;
        int columns = (int) (windowWidth / scaledCellWidth);
        int rows = (int) (windowHeight / scaledCellHeight);
        int leftoverX = (int) (windowWidth - columns * scaledCellWidth);
        int leftoverY = (int) (windowHeight - rows * scaledCellHeight);
        return new GridFit(windowWidth, windowHeight, cellWidth, cellHeight,
                minColumns, minRows, scale, columns, rows, leftoverX, leftoverY);
    }

    public int getScale() {
        return scale;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    /** Unused window pixels to the side of the grid. */
    public int getLeftoverX() {
        return leftoverX;
    }

    /** Unused window pixels above or below the grid. */
    public int getLeftoverY() {
        return leftoverY;
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public int getCellHeight() {
        return cellHeight;
    }

    /** Grid width in unscaled cell pixels. */
    public int getWorldWidth() {
        return columns * cellWidth;
    }

    /** Grid height in unscaled cell pixels. */
    public int getWorldHeight() {
        return rows * cellHeight;
    }

    /** Whether the grid has at least the requested minimum columns and rows. */
    public boolean meetsMinimum() {
        return columns >= minColumns && rows >= minRows;
    }

    /** Whether no column or no row fits. */
    public boolean isEmpty() {
        return columns == 0 || rows == 0;
    }

    /**
     * Configures the viewport to use exactly this scale and world size, then updates it.
     * Does nothing if the result is empty. Call it on resize, not every frame.
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
