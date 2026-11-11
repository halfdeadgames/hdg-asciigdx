package com.github.halfdeadgames.asciigdx.workbench.paint;

import com.github.halfdeadgames.asciigdx.workbench.ui.GlyphSize;

/**
 * Pixel layout of a grid of equal rectangular slots separated by gaps, in window pixels with the
 * origin at the top-left and y pointing down. The panel starts with a gap, so it is
 * {@code columns * (slotWidth + gap) + gap} pixels wide and {@code rows * (slotHeight + gap) + gap}
 * pixels high.
 */
final class SheetGridLayout {

    private final int left;
    private final int top;
    private final int slotWidth;
    private final int slotHeight;
    private final int pitchX;
    private final int pitchY;
    private final int gap;
    private final int columns;
    private final int visibleColumns;
    private final int rows;

    SheetGridLayout(int left, int top, GlyphSize slotSize, int gap, int columns, int rows) {
        this(left, top, slotSize, gap, columns, columns, rows);
    }

    /**
     * Slots are numbered across all {@code columns}, but only the first {@code visibleColumns} take
     * room in the panel and answer {@link #slotAt}.
     */
    SheetGridLayout(int left, int top, GlyphSize slotSize, int gap, int columns,
                    int visibleColumns, int rows) {
        this.visibleColumns = Math.max(1, Math.min(columns, visibleColumns));
        this.left = left;
        this.top = top;
        this.slotWidth = slotSize.width();
        this.slotHeight = slotSize.height();
        this.gap = gap;
        this.pitchX = slotWidth + gap;
        this.pitchY = slotHeight + gap;
        this.columns = columns;
        this.rows = rows;
    }

    int panelWidth() {
        return visibleColumns * pitchX + gap;
    }

    int panelHeight() {
        return rows * pitchY + gap;
    }

    int left() {
        return left;
    }

    int top() {
        return top;
    }

    int slotWidth() {
        return slotWidth;
    }

    int slotHeight() {
        return slotHeight;
    }

    int slotLeft(int index) {
        return left + gap + (index % columns) * pitchX;
    }

    int slotTop(int index) {
        return top + gap + (index / columns) * pitchY;
    }

    /** Top of a slot when the panel is scrolled so that {@code firstRow} is the first visible row. */
    int slotTop(int index, int firstRow) {
        return top + gap + (index / columns - firstRow) * pitchY;
    }

    /** Height of a panel that shows the given number of rows. */
    int heightForRows(int visibleRows) {
        return visibleRows * pitchY + gap;
    }

    /** Number of whole rows that fit in a height, at least 1. */
    int rowsThatFit(int height) {
        return Math.max(1, (height - gap) / pitchY);
    }

    /** Returns the slot under a pixel, or -1 if the pixel is in a gap or outside the panel. */
    int slotAt(int pixelX, int pixelY) {
        return slotAt(pixelX, pixelY, 0, rows);
    }

    /**
     * Returns the slot under a pixel when the panel is scrolled to {@code firstRow} and shows
     * {@code visibleRows} rows, or -1 for gaps, hidden rows, and pixels outside the panel.
     */
    int slotAt(int pixelX, int pixelY, int firstRow, int visibleRows) {
        return slotAt(pixelX, pixelY, firstRow, visibleRows, 0);
    }

    /** Like the row-scrolled lookup, with {@code firstColumn} as the leftmost shown column. */
    int slotAt(int pixelX, int pixelY, int firstRow, int visibleRows, int firstColumn) {
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
            || x % pitchX >= slotWidth || y % pitchY >= slotHeight) {
            return -1;
        }
        return row * columns + column;
    }

    /** Left of a slot when {@code firstColumn} is the leftmost shown column. */
    int slotLeft(int index, int firstColumn) {
        return left + gap + (index % columns - firstColumn) * pitchX;
    }
}
