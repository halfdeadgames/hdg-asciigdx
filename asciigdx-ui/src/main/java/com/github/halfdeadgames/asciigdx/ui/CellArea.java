package com.github.halfdeadgames.asciigdx.ui;

/**
 * A rectangle of surface cells, such as the region where windows may be dragged (see
 * {@link UiRoot#setWindowDragBounds(CellArea)}). Coordinates are surface cells, y-down: column 0 is
 * the left edge and row 0 is the top edge of the surface. The rectangle is not checked against any
 * surface size, so it may reach past the surface edge.
 *
 * <p>The last two values are sizes in cells, not end coordinates. For example,
 * {@code new CellArea(0, 2, 80, 40)} covers columns 0 to 79 and rows 2 to 41.
 *
 * <p>Immutable and safe to share between threads.
 *
 * @param column column of the left cell; {@code 0} or more
 * @param row row of the top cell; {@code 0} or more
 * @param columnCount width in cells; {@code 1} or more
 * @param rowCount height in cells; {@code 1} or more
 */
public record CellArea(int column, int row, int columnCount, int rowCount) {

    /**
     * Creates an area after checking every field.
     *
     * @param column column of the left cell; {@code 0} or more
     * @param row row of the top cell; {@code 0} or more
     * @param columnCount width in cells; {@code 1} or more
     * @param rowCount height in cells; {@code 1} or more
     * @throws IllegalArgumentException if {@code column} or {@code row} is negative, or if
     *     {@code columnCount} or {@code rowCount} is less than 1
     */
    public CellArea {
        if (column < 0) {
            throw new IllegalArgumentException("column must be 0 or more, was " + column);
        }
        if (row < 0) {
            throw new IllegalArgumentException("row must be 0 or more, was " + row);
        }
        if (columnCount < 1) {
            throw new IllegalArgumentException("columnCount must be 1 or more, was " + columnCount);
        }
        if (rowCount < 1) {
            throw new IllegalArgumentException("rowCount must be 1 or more, was " + rowCount);
        }
    }
}
