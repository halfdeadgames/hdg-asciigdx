package com.github.halfdeadgames.asciigdx.shape;

/**
 * A set of grid cells. Shapes are never empty, so the bounds are always valid. Coordinates may be
 * negative or outside any grid; consumers clip. {@link ShapeCellVisitor} describes the distance
 * units.
 */
public interface CellShape {

    /**
     * Visits every cell exactly once, row by row from top to bottom and left to right within a row.
     * Allocates nothing.
     *
     * @param visitor receives each cell
     */
    void forEachCell(ShapeCellVisitor visitor);

    /**
     * Returns the left edge of the bounds.
     *
     * @return the smallest column of any visited cell (inclusive)
     */
    int getLeft();

    /**
     * Returns the top edge of the bounds.
     *
     * @return the smallest row of any visited cell (inclusive)
     */
    int getTop();

    /**
     * Returns the right edge of the bounds.
     *
     * @return the largest column of any visited cell (inclusive)
     */
    int getRight();

    /**
     * Returns the bottom edge of the bounds.
     *
     * @return the largest row of any visited cell (inclusive)
     */
    int getBottom();
}
