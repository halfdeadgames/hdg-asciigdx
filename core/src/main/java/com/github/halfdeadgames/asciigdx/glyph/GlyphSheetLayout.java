package com.github.halfdeadgames.asciigdx.glyph;

/**
 * Pixel layout of a glyph sheet image: a grid of equally sized cells, called slots,
 * with an optional outer margin and spacing between cells.
 *
 * <p>Coordinates are in image space: the origin is the top-left pixel and {@code y}
 * grows downward. Slots are numbered in reading order, left to right, then top to bottom.
 *
 * <p>Immutable and free of libGDX types.
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
    public GlyphSheetLayout(int cellWidth, int cellHeight, int columns, int rows, int margin, int spacing) {
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
                "Margin and spacing must not be negative, was margin=" + margin + ", spacing=" + spacing);
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
        Math.multiplyExact(columns, rows);
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
    public static GlyphSheetLayout grid(int cellWidth, int cellHeight, int columns, int rows) {
        return new GlyphSheetLayout(cellWidth, cellHeight, columns, rows, 0, 0);
    }

    /** Returns the width of one cell in pixels. */
    public int getCellWidth() {
        return cellWidth;
    }

    /** Returns the height of one cell in pixels. */
    public int getCellHeight() {
        return cellHeight;
    }

    /** Returns the number of cells per row. */
    public int getColumns() {
        return columns;
    }

    /** Returns the number of cell rows. */
    public int getRows() {
        return rows;
    }

    /** Returns the border around the whole grid in pixels. */
    public int getMargin() {
        return margin;
    }

    /** Returns the gap between neighboring cells in pixels. */
    public int getSpacing() {
        return spacing;
    }

    /** Returns the number of slots, {@code columns * rows}. */
    public int slotCount() {
        return columns * rows;
    }

    /**
     * Returns the x pixel of the left edge of a slot.
     *
     * @param slot slot index in reading order
     * @return x in image space
     * @throws IndexOutOfBoundsException if the slot is outside {@code [0, slotCount())}
     */
    public int slotX(int slot) {
        checkSlot(slot);
        return margin + (slot % columns) * (cellWidth + spacing);
    }

    /**
     * Returns the y pixel of the top edge of a slot.
     *
     * @param slot slot index in reading order
     * @return y in image space, growing downward
     * @throws IndexOutOfBoundsException if the slot is outside {@code [0, slotCount())}
     */
    public int slotY(int slot) {
        checkSlot(slot);
        return margin + (slot / columns) * (cellHeight + spacing);
    }

    /** Returns the smallest image width in pixels that holds every slot. */
    public int requiredImageWidth() {
        return extent(cellWidth, columns, margin, spacing);
    }

    /** Returns the smallest image height in pixels that holds every slot. */
    public int requiredImageHeight() {
        return extent(cellHeight, rows, margin, spacing);
    }

    private void checkSlot(int slot) {
        if (slot < 0 || slot >= slotCount()) {
            throw new IndexOutOfBoundsException(
                "Slot " + slot + " is outside [0, " + slotCount() + ")");
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
