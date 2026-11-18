package com.github.halfdeadgames.asciigdx.shape;

/**
 * How a {@link LineShape} turns the straight line between its two endpoint cells into cells.
 *
 * <p>Both styles always include both endpoint cells. Cells are chosen by walking from the upper
 * endpoint (the left one if both are in the same row), so the result does not depend on which
 * endpoint comes first. When the true line passes exactly through a cell corner, the step along the
 * shorter axis waits: a line at least as wide as it is tall stays in its row, and a steeper line
 * stays in its column. Mirroring a line left to right mirrors its cells. Flipping it across the
 * main diagonal (swapping columns and rows) flips its cells only for lines that run down and to the
 * right; for lines that run down and to the left, a tie may pick the other cell.
 *
 * <p>A corner gap means two consecutive cells that touch only at a corner.
 *
 * <p>More styles may be added in later versions; code that switches over the styles should handle
 * unknown ones.
 */
public enum LineStyle {

    /**
     * Visits exactly one cell per step along the longer axis (columns or rows), so a line has
     * {@code max(|dx|, |dy|) + 1} cells, where dx and dy are the column and row differences between
     * the endpoints. Each step picks the cell nearest to the true line and may move diagonally, so
     * the line can have corner gaps. Corner ties follow the rule in the {@link LineStyle} summary.
     * This is the thinnest style.
     */
    DIAGONAL_STEPS,

    /**
     * Never steps diagonally: every step moves exactly one cell horizontally or vertically, so a line
     * has {@code |dx| + |dy| + 1} cells, where dx and dy are the column and row differences between
     * the endpoints, and it has no corner gaps. Corner ties follow the rule in the
     * {@link LineStyle} summary: a line at least as wide as it is tall takes the cell in the same
     * row first and then steps down, and a steeper line takes the cell in the same column first and
     * then steps sideways. The line looks thicker than {@link #DIAGONAL_STEPS}.
     */
    NO_DIAGONAL_STEPS
}
