package com.github.halfdeadgames.asciigdx.workbench;

/**
 * Pixel layout of a grid of square cells separated by gaps, in window pixels with the origin at
 * the top-left and y pointing down. The panel starts with a gap, so it is
 * {@code columns * (cellSize + gap) + gap} pixels wide.
 */
public final class SheetGridLayout {

    private final int left;
    private final int top;
    private final int cellSize;
    private final int pitch;
    private final int gap;
    private final int columns;
    private final int rows;

    public SheetGridLayout(int left, int top, int cellSize, int gap, int columns, int rows) {
        this.left = left;
        this.top = top;
        this.cellSize = cellSize;
        this.gap = gap;
        this.pitch = cellSize + gap;
        this.columns = columns;
        this.rows = rows;
    }

    public int panelWidth() {
        return columns * pitch + gap;
    }

    public int panelHeight() {
        return rows * pitch + gap;
    }

    public int left() {
        return left;
    }

    public int top() {
        return top;
    }

    public int cellSize() {
        return cellSize;
    }

    public int cellLeft(int index) {
        return left + gap + (index % columns) * pitch;
    }

    public int cellTop(int index) {
        return top + gap + (index / columns) * pitch;
    }

    /** Top of a cell when the panel is scrolled so that {@code firstRow} is the first visible row. */
    public int cellTop(int index, int firstRow) {
        return top + gap + (index / columns - firstRow) * pitch;
    }

    /** Height of a panel that shows the given number of rows. */
    public int heightForRows(int visibleRows) {
        return visibleRows * pitch + gap;
    }

    /** Number of whole rows that fit in a height, at least 1. */
    public int rowsThatFit(int height) {
        return Math.max(1, (height - gap) / pitch);
    }

    /** Returns the cell under a pixel, or -1 if the pixel is in a gap or outside the panel. */
    public int cellAt(int pixelX, int pixelY) {
        return cellAt(pixelX, pixelY, 0, rows);
    }

    /**
     * Returns the cell under a pixel when the panel is scrolled to {@code firstRow} and shows
     * {@code visibleRows} rows, or -1 for gaps, hidden rows, and pixels outside the panel.
     */
    public int cellAt(int pixelX, int pixelY, int firstRow, int visibleRows) {
        int x = pixelX - left - gap;
        int y = pixelY - top - gap;
        if (x < 0 || y < 0) {
            return -1;
        }
        int column = x / pitch;
        int visibleRow = y / pitch;
        int row = visibleRow + firstRow;
        if (column >= columns || visibleRow >= visibleRows || row >= rows
            || x % pitch >= cellSize || y % pitch >= cellSize) {
            return -1;
        }
        return row * columns + column;
    }
}
