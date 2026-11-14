package com.github.halfdeadgames.asciigdx.shape;

/**
 * Receives each cell of a {@link CellShape}.
 *
 * <p>Distances are measured in column widths between the shape's center and the center of the cell.
 * Cell (column, row) has its center at (column + 0.5, row + 0.5). The row difference is multiplied by the
 * shape's {@code rowHeightPerColumnWidth} (cell height divided by cell width: 1 for square cells, 2 for
 * tall 1:2 cells), so equal distances look equal on screen.
 */
@FunctionalInterface
public interface ShapeCellVisitor {

    /**
     * Called once per cell.
     *
     * @param column           cell column, possibly negative
     * @param row              cell row, possibly negative
     * @param distance         distance from the shape's center to the cell's center, in column widths
     * @param distanceFraction {@code distance} divided by the largest distance of any cell in the shape,
     *                         in [0, 1]; 0 when that largest distance is 0 (never NaN)
     */
    void visit(int column, int row, float distance, float distanceFraction);
}
