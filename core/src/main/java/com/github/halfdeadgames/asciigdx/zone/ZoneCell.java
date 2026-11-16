package com.github.halfdeadgames.asciigdx.zone;

/**
 * Describes the cell an effect is currently running on: where it is in the grid, where it is inside the
 * zone's shape, and how far it is from the shape's center.
 *
 * <p>The same instance is reused for every cell of every apply. Read it inside the effect call and do not
 * keep it.
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

    void set(int column, int row, int zoneColumn, int zoneRow, float distance, float distanceFraction) {
        this.column = column;
        this.row = row;
        this.zoneColumn = zoneColumn;
        this.zoneRow = zoneRow;
        this.distance = distance;
        this.distanceFraction = distanceFraction;
    }

    /** The cell's column in the layer's grid, or in the map when the layer is a map window. */
    public int getColumn() {
        return column;
    }

    /** The cell's row in the layer's grid, or in the map when the layer is a map window. */
    public int getRow() {
        return row;
    }

    /** The cell's column counted from the left edge of the shape's bounds. */
    public int getZoneColumn() {
        return zoneColumn;
    }

    /** The cell's row counted from the top edge of the shape's bounds. */
    public int getZoneRow() {
        return zoneRow;
    }

    /**
     * Distance from the shape's center to the cell's center, in column widths, with rows scaled by the
     * shape's aspect.
     */
    public float getDistance() {
        return distance;
    }

    /** {@link #getDistance()} divided by the largest distance in the shape, from 0 to 1. */
    public float getDistanceFraction() {
        return distanceFraction;
    }

    /**
     * Width of one cell in virtual pixels. Multiply a horizontal amount in cell widths by it to get a
     * {@link com.github.halfdeadgames.asciigdx.layer.CellHandle} x offset.
     */
    public float getCellWidth() {
        return cellWidth;
    }

    /**
     * Height of one cell in virtual pixels. Multiply a vertical amount in cell heights by it to get a
     * {@link com.github.halfdeadgames.asciigdx.layer.CellHandle} y offset.
     */
    public float getCellHeight() {
        return cellHeight;
    }
}
