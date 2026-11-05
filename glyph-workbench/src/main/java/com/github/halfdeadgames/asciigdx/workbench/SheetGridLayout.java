package com.github.halfdeadgames.asciigdx.workbench;

import com.github.halfdeadgames.asciigdx.workbench.ui.GlyphSize;

/**
 * Pixel layout of a grid of equal rectangular cells separated by gaps, in window pixels with the
 * origin at the top-left and y pointing down. The panel starts with a gap, so it is
 * {@code columns * (cellWidth + gap) + gap} pixels wide and {@code rows * (cellHeight + gap) + gap}
 * pixels high.
 */
public final class SheetGridLayout {

    private final int left;
    private final int top;
    private final int cellWidth;
    private final int cellHeight;
    private final int pitchX;
    private final int pitchY;
    private final int gap;
    private final int columns;
    private final int visibleColumns;
    private final int rows;

    public SheetGridLayout(int left, int top, GlyphSize cellSize, int gap, int columns, int rows) {
        this(left, top, cellSize, gap, columns, columns, rows);
    }

    /**
     * Cells are numbered across all {@code columns}, but only the first {@code visibleColumns} take
     * room in the panel and answer {@link #cellAt}.
     */
    public SheetGridLayout(int left, int top, GlyphSize cellSize, int gap, int columns,
                           int visibleColumns, int rows) {
        this.visibleColumns = Math.max(1, Math.min(columns, visibleColumns));
        this.left = left;
        this.top = top;
        this.cellWidth = cellSize.width();
        this.cellHeight = cellSize.height();
        this.gap = gap;
        this.pitchX = cellWidth + gap;
        this.pitchY = cellHeight + gap;
        this.columns = columns;
        this.rows = rows;
    }

    public int panelWidth() {
        return visibleColumns * pitchX + gap;
    }

    public int panelHeight() {
        return rows * pitchY + gap;
    }

    public int left() {
        return left;
    }

    public int top() {
        return top;
    }

    public int cellWidth() {
        return cellWidth;
    }

    public int cellHeight() {
        return cellHeight;
    }

    public int cellLeft(int index) {
        return left + gap + (index % columns) * pitchX;
    }

    public int cellTop(int index) {
        return top + gap + (index / columns) * pitchY;
    }

    /** Top of a cell when the panel is scrolled so that {@code firstRow} is the first visible row. */
    public int cellTop(int index, int firstRow) {
        return top + gap + (index / columns - firstRow) * pitchY;
    }

    /** Height of a panel that shows the given number of rows. */
    public int heightForRows(int visibleRows) {
        return visibleRows * pitchY + gap;
    }

    /** Number of whole rows that fit in a height, at least 1. */
    public int rowsThatFit(int height) {
        return Math.max(1, (height - gap) / pitchY);
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
        return cellAt(pixelX, pixelY, firstRow, visibleRows, 0);
    }

    /** Like the row-scrolled lookup, with {@code firstColumn} as the leftmost shown column. */
    public int cellAt(int pixelX, int pixelY, int firstRow, int visibleRows, int firstColumn) {
        int x = pixelX - left - gap;
        int y = pixelY - top - gap;
        if (x < 0 || y < 0) {
            return -1;
        }
        int visibleColumn = x / pitchX;
        int column = visibleColumn + firstColumn;
        int visibleRow = y / pitchY;
        int row = visibleRow + firstRow;
        if (visibleColumn >= visibleColumns || column >= columns || visibleRow >= visibleRows || row >= rows
            || x % pitchX >= cellWidth || y % pitchY >= cellHeight) {
            return -1;
        }
        return row * columns + column;
    }

    /** Left of a cell when {@code firstColumn} is the leftmost shown column. */
    public int cellLeft(int index, int firstColumn) {
        return left + gap + (index % columns - firstColumn) * pitchX;
    }
}
