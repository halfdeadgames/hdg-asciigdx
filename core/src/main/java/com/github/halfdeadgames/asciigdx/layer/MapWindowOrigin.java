package com.github.halfdeadgames.asciigdx.layer;

/**
 * Turns a camera position in virtual pixels into the whole-cell map origin shown at grid cell
 * (0,0). Each layer uses its own cell size, so layers with smaller cells get larger origin numbers
 * for the same camera. Only the math lives here; the caller shifts grid contents by the reported
 * difference.
 */
final class MapWindowOrigin {

    /**
     * Largest origin magnitude in cells. A quarter of the int range keeps shifts (a difference of
     * two origins), handle moves, and {@code origin + grid size} from overflowing int.
     */
    static final int MAX_ORIGIN = Integer.MAX_VALUE / 4;

    private final int marginColumns;
    private final int marginRows;

    private int originColumn;
    private int originRow;
    private int shiftColumns;
    private int shiftRows;

    /**
     * Starts as if the camera were at (0,0).
     *
     * @param marginColumns extra cells kept before the visible left edge
     * @param marginRows extra cells kept before the visible top edge
     */
    MapWindowOrigin(int marginColumns, int marginRows) {
        if (marginColumns < 0 || marginRows < 0) {
            throw new IllegalArgumentException(
                    "Margins must not be negative: " + marginColumns + ", " + marginRows);
        }
        this.marginColumns = marginColumns;
        this.marginRows = marginRows;
        this.originColumn = clampOrigin(-(long) marginColumns);
        this.originRow = clampOrigin(-(long) marginRows);
    }

    /**
     * Returns the map cell index at the window edge for one axis.
     *
     * @param cameraPixels camera position on the axis, in pixels
     * @param cellSize cell size on the axis, in pixels; must be finite and positive
     * @param margin cells kept before the visible edge
     * @return the origin, clamped to {@code +/-MAX_ORIGIN}; a NaN camera counts as 0
     */
    static int originFor(float cameraPixels, float cellSize, int margin) {
        if (!Float.isFinite(cellSize) || cellSize <= 0f) {
            throw new IllegalArgumentException(
                    "Cell size must be finite and positive: " + cellSize);
        }
        if (Float.isNaN(cameraPixels)) {
            return clampOrigin(-(long) margin);
        }
        double cells = Math.floor((double) cameraPixels / cellSize);
        long boundedCells = (long) Math.max(-MAX_ORIGIN, Math.min(MAX_ORIGIN, cells));
        return clampOrigin(boundedCells - margin);
    }

    private static int clampOrigin(long origin) {
        return (int) Math.max(-MAX_ORIGIN, Math.min(MAX_ORIGIN, origin));
    }

    /**
     * Moves the window to the camera and records the change from the previous origin as the shift.
     */
    void follow(float cameraX, float cameraY, float cellWidth, float cellHeight) {
        int newColumn = originFor(cameraX, cellWidth, marginColumns);
        int newRow = originFor(cameraY, cellHeight, marginRows);
        // Both origins are within +/-MAX_ORIGIN, so the difference cannot wrap.
        shiftColumns = newColumn - originColumn;
        shiftRows = newRow - originRow;
        originColumn = newColumn;
        originRow = newRow;
    }

    int getOriginColumn() {
        return originColumn;
    }

    int getOriginRow() {
        return originRow;
    }

    /** Columns the origin moved in the latest {@link #follow} call. */
    int getShiftColumns() {
        return shiftColumns;
    }

    /** Rows the origin moved in the latest {@link #follow} call. */
    int getShiftRows() {
        return shiftRows;
    }

    boolean hasShifted() {
        return shiftColumns != 0 || shiftRows != 0;
    }

    int getMarginColumns() {
        return marginColumns;
    }

    int getMarginRows() {
        return marginRows;
    }
}
