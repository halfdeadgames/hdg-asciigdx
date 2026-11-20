package com.github.halfdeadgames.asciigdx.ui;

/**
 * The cell geometry that {@link ProgressBar} and {@link Slider} share. A bar is a line of cells,
 * each one glyph: one UI cell for the square font, and two UI rows high for the tall font. Cell
 * indexes count from the minimum end: from the left for a horizontal bar, and from the bottom for
 * a vertical bar.
 */
final class BarAxis {

    private static final int SQUARE_CELL_ROWS = 1;
    private static final int TALL_CELL_ROWS = 2;

    private final BarOrientation orientation;
    private final int rowsPerCell;

    BarAxis(BarOrientation orientation, UiFont font) {
        this.orientation = orientation;
        this.rowsPerCell = font == UiFont.TALL ? TALL_CELL_ROWS : SQUARE_CELL_ROWS;
    }

    int getRowsPerCell() {
        return rowsPerCell;
    }

    int widthFor(int cellCount) {
        return orientation == BarOrientation.HORIZONTAL ? cellCount : 1;
    }

    int heightFor(int cellCount) {
        return orientation == BarOrientation.HORIZONTAL ? rowsPerCell : cellCount * rowsPerCell;
    }

    int cellCountIn(int width, int height) {
        return orientation == BarOrientation.HORIZONTAL ? width : height / rowsPerCell;
    }

    int columnOf(int left, int index) {
        return orientation == BarOrientation.HORIZONTAL ? left + index : left;
    }

    int rowOf(int top, int index, int cellCount) {
        return orientation == BarOrientation.HORIZONTAL
                ? top
                : top + (cellCount - 1 - index) * rowsPerCell;
    }

    /** Returns the index of the cell at a local position, clamped to the cells of the bar. */
    int indexAt(int column, int row, int cellCount) {
        int index = orientation == BarOrientation.HORIZONTAL
                ? column
                : cellCount - 1 - Math.floorDiv(row, rowsPerCell);
        return Math.max(0, Math.min(cellCount - 1, index));
    }
}
