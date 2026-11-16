package com.github.halfdeadgames.asciigdx.zone;

/**
 * Describes the cell an effect is currently running on: where it is in the grid, where it is inside
 * the zone's shape, and how far it is from the shape's center.
 *
 * <p>The same instance is reused for every cell of every apply. Read it inside the effect call and
 * do not keep it.
 */
public final class ZoneCell {

    private int column;
    private int row;
    private int zoneColumn;
    private int zoneRow;
    private float distance;
    private float distanceFraction;
    private float cellWidth;
    private float cellHeight;

    ZoneCell() {
    }

    void setCellSize(float cellWidth, float cellHeight) {
        this.cellWidth = cellWidth;
        this.cellHeight = cellHeight;
    }

    void set(int column, int row, int zoneColumn, int zoneRow, float distance,
            float distanceFraction) {
        this.column = column;
        this.row = row;
        this.zoneColumn = zoneColumn;
        this.zoneRow = zoneRow;
        this.distance = distance;
        this.distanceFraction = distanceFraction;
    }

    /**
     * Returns the cell's column in the layer's grid, or in the map when the layer is a map window.
     *
     * @return the column, counted from 0 at the left
     */
    public int getColumn() {
        return column;
    }

    /**
     * Returns the cell's row in the layer's grid, or in the map when the layer is a map window.
     *
     * @return the row, counted from 0 at the top
     */
    public int getRow() {
        return row;
    }

    /**
     * Returns the cell's column counted from the left edge of the shape's bounds.
     *
     * @return the column offset inside the shape's bounds; 0 at the left edge
     */
    public int getZoneColumn() {
        return zoneColumn;
    }

    /**
     * Returns the cell's row counted from the top edge of the shape's bounds.
     *
     * @return the row offset inside the shape's bounds; 0 at the top edge
     */
    public int getZoneRow() {
        return zoneRow;
    }

    /**
     * Returns the distance from the shape's center to the cell's center, with rows scaled by the
     * shape's aspect.
     *
     * @return the distance in column widths; not negative
     */
    public float getDistance() {
        return distance;
    }

    /**
     * Returns {@link #getDistance()} divided by the largest distance in the shape.
     *
     * @return the relative distance, from 0 at the center to 1 at the farthest cell; 0 when the
     *     largest distance is 0, as in a one-cell shape
     */
    public float getDistanceFraction() {
        return distanceFraction;
    }

    /**
     * Returns the width of one cell. Multiply a horizontal amount in cell widths by it to get a
     * {@link com.github.halfdeadgames.asciigdx.layer.CellHandle} x offset.
     *
     * @return the cell width in virtual pixels
     */
    public float getCellWidth() {
        return cellWidth;
    }

    /**
     * Returns the height of one cell. Multiply a vertical amount in cell heights by it to get a
     * {@link com.github.halfdeadgames.asciigdx.layer.CellHandle} y offset.
     *
     * @return the cell height in virtual pixels
     */
    public float getCellHeight() {
        return cellHeight;
    }
}
