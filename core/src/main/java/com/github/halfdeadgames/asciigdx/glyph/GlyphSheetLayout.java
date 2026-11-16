package com.github.halfdeadgames.asciigdx.glyph;

/**
 * Pixel layout of a glyph sheet image: a grid of equally sized cells, called slots,
 * with an optional outer margin and spacing between cells.
 *
 * <p>Coordinates are in image space: the origin is the top-left pixel and {@code y}
 * grows downward. Slots are numbered in reading order, left to right, then top to bottom.
 *
 * <p>Slot 0 is the top-left cell. Slot {@code columns} is the first cell of the second row.
 *
 * <p>Immutable, free of libGDX types, and safe to share between threads.
 */
public final class GlyphSheetLayout {

    private final int cellWidth;
    private final int cellHeight;
    private final int columns;
    private final int rows;
    private final int margin;
    private final int spacing;

    /**
     * Creates a layout.
     *
     * @param cellWidth width of one cell in pixels, must be positive
     * @param cellHeight height of one cell in pixels, must be positive
     * @param columns number of cells per row, must be positive
     * @param rows number of cell rows, must be positive
     * @param margin border around the whole grid in pixels, must not be negative
     * @param spacing gap between neighboring cells in pixels, must not be negative
     * @throws IllegalArgumentException if a size or count is not positive, margin or
     *         spacing is negative, or the resulting image size does not fit in an int
     */
    public GlyphSheetLayout(int cellWidth, int cellHeight, int columns, int rows, int margin,
            int spacing) {
        if (cellWidth <= 0 || cellHeight <= 0) {
            throw new IllegalArgumentException(
                    "Cell size must be positive, was " + cellWidth + "x" + cellHeight);
        }
        if (columns <= 0 || rows <= 0) {
            throw new IllegalArgumentException(
                    "Column and row counts must be positive, was " + columns + "x" + rows);
        }
        if (margin < 0 || spacing < 0) {
            throw new IllegalArgumentException(
                    "Margin and spacing must not be negative, was margin=" + margin + ", spacing="
                            + spacing);
        }
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
        this.columns = columns;
        this.rows = rows;
        this.margin = margin;
        this.spacing = spacing;
        // Fail fast on overflow and on slot counts that do not fit in an int.
        extent(cellWidth, columns, margin, spacing);
        extent(cellHeight, rows, margin, spacing);
        checkSlotCountFits(columns, rows);
    }

    /**
     * Creates a layout with no margin and no spacing.
     *
     * @param cellWidth width of one cell in pixels, must be positive
     * @param cellHeight height of one cell in pixels, must be positive
     * @param columns number of cells per row, must be positive
     * @param rows number of cell rows, must be positive
     * @return the layout
     * @throws IllegalArgumentException if a size or count is not positive
     */
    public static GlyphSheetLayout packed(int cellWidth, int cellHeight, int columns, int rows) {
        return new GlyphSheetLayout(cellWidth, cellHeight, columns, rows, 0, 0);
    }

    /**
     * Returns the width of one cell.
     *
     * @return the width in pixels; always positive
     */
    public int getCellWidth() {
        return cellWidth;
    }

    /**
     * Returns the height of one cell.
     *
     * @return the height in pixels; always positive
     */
    public int getCellHeight() {
        return cellHeight;
    }

    /**
     * Returns the number of cells per row.
     *
     * @return the column count; always positive
     */
    public int getColumns() {
        return columns;
    }

    /**
     * Returns the number of cell rows.
     *
     * @return the row count; always positive
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns the border around the whole grid. It is applied on all four sides.
     *
     * @return the margin in pixels; zero or more
     */
    public int getMargin() {
        return margin;
    }

    /**
     * Returns the gap between neighboring cells, both horizontally and vertically. There is no
     * gap between the margin and the first or last cell.
     *
     * @return the spacing in pixels; zero or more
     */
    public int getSpacing() {
        return spacing;
    }

    /**
     * Returns the number of slots, {@code columns * rows}.
     *
     * @return the slot count; always positive
     */
    public int getSlotCount() {
        return columns * rows;
    }

    /**
     * Returns the x pixel of the left edge of a slot. The slot spans {@link #getCellWidth()}
     * pixels to the right of it.
     *
     * @param slot slot index in reading order, from 0 at the top-left
     * @return x in image space, in pixels from the left edge of the image
     * @throws IndexOutOfBoundsException if the slot is outside {@code [0, getSlotCount())}
     */
    public int getSlotX(int slot) {
        checkSlot(slot);
        return margin + (slot % columns) * (cellWidth + spacing);
    }

    /**
     * Returns the y pixel of the top edge of a slot. The slot spans {@link #getCellHeight()}
     * pixels below it.
     *
     * @param slot slot index in reading order, from 0 at the top-left
     * @return y in image space, in pixels from the top edge of the image, growing downward
     * @throws IndexOutOfBoundsException if the slot is outside {@code [0, getSlotCount())}
     */
    public int getSlotY(int slot) {
        checkSlot(slot);
        return margin + (slot / columns) * (cellHeight + spacing);
    }

    /**
     * Returns the smallest image width that holds every slot, including margins and spacing.
     *
     * @return the width in pixels
     */
    public int getRequiredImageWidth() {
        return extent(cellWidth, columns, margin, spacing);
    }

    /**
     * Returns the smallest image height that holds every slot, including margins and spacing.
     *
     * @return the height in pixels
     */
    public int getRequiredImageHeight() {
        return extent(cellHeight, rows, margin, spacing);
    }

    private void checkSlot(int slot) {
        if (slot < 0 || slot >= getSlotCount()) {
            throw new IndexOutOfBoundsException(
                    "Slot " + slot + " is outside [0, " + getSlotCount() + ")");
        }
    }

    private static void checkSlotCountFits(int columns, int rows) {
        try {
            Math.multiplyExact(columns, rows);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Slot count does not fit in an int: " + columns + "x" + rows, e);
        }
    }

    private static int extent(int cellSize, int count, int margin, int spacing) {
        try {
            int cells = Math.multiplyExact(cellSize, count);
            int gaps = Math.multiplyExact(spacing, count - 1);
            return Math.addExact(Math.addExact(cells, gaps), Math.multiplyExact(2, margin));
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Sheet size does not fit in an int", e);
        }
    }
}
