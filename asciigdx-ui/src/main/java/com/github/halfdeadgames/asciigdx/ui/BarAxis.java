package com.github.halfdeadgames.asciigdx.ui;

/**
 * The cell geometry that {@link ProgressBar} and {@link Slider} share. A bar is a line of cells,
 * each one square-font glyph in one UI cell. Cell indexes count from the minimum end: from the
 * left for a horizontal bar, and from the bottom for a vertical bar.
 */
final class BarAxis {

    private final BarOrientation orientation;

    BarAxis(BarOrientation orientation) {
        this.orientation = orientation;
    }

    int widthFor(int cellCount) {
        return orientation == BarOrientation.HORIZONTAL ? cellCount : 1;
    }

    int heightFor(int cellCount) {
        return orientation == BarOrientation.HORIZONTAL ? 1 : cellCount;
    }

    int cellCountIn(int width, int height) {
        return orientation == BarOrientation.HORIZONTAL ? width : height;
    }

    int columnOf(int left, int index) {
        return orientation == BarOrientation.HORIZONTAL ? left + index : left;
    }

    int rowOf(int top, int index, int cellCount) {
        return orientation == BarOrientation.HORIZONTAL ? top : top + (cellCount - 1 - index);
    }

    /** Returns the index of the cell at a local position, clamped to the cells of the bar. */
    int indexAt(int column, int row, int cellCount) {
        int index = orientation == BarOrientation.HORIZONTAL ? column : cellCount - 1 - row;
        return Math.max(0, Math.min(cellCount - 1, index));
    }
}
