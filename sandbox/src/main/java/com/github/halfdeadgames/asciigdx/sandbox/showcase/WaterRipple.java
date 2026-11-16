package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.github.halfdeadgames.asciigdx.layer.CellHandle;
import com.github.halfdeadgames.asciigdx.layer.Layer;

/**
 * Swells the water glyphs near a splash point as rings pass. Only the scale of lifted cells is
 * touched, so other effects can use the same handles for alpha.
 */
public final class WaterRipple {

    private final RippleRings rings;
    private final float maxScaleBump;
    private final CellHandle[] cells;
    private final float[] distancesCells;
    private boolean atRest = true;

    /**
     * Cell coordinates are in cell units from the grid's top-left corner, so the center of cell
     * (c, r) is (c + 0.5, r + 0.5). Handles are made for the cells whose centers lie within the
     * half extents of the splash point.
     *
     * @param rowHeightPerColumnWidth cell height divided by cell width, so rings look round on
     *        screen
     */
    public WaterRipple(Layer waterLayer, float splashColumn, float splashRow,
            float rowHeightPerColumnWidth,
            float halfWidthCells, float halfHeightCells, RippleRings rings, float maxScaleBump) {
        if (waterLayer == null || rings == null) {
            throw new IllegalArgumentException("waterLayer and rings must not be null");
        }
        requireFinitePositive(rowHeightPerColumnWidth, "rowHeightPerColumnWidth");
        requireFinitePositive(halfWidthCells, "halfWidthCells");
        requireFinitePositive(halfHeightCells, "halfHeightCells");
        requireFinitePositive(maxScaleBump, "maxScaleBump");
        this.rings = rings;
        this.maxScaleBump = maxScaleBump;
        int firstColumn = clampedFirstCell(splashColumn - halfWidthCells);
        int lastColumn =
                clampedLastCell(splashColumn + halfWidthCells, waterLayer.getGrid().getColumns());
        int firstRow = clampedFirstCell(splashRow - halfHeightCells);
        int lastRow = clampedLastCell(splashRow + halfHeightCells, waterLayer.getGrid().getRows());
        int columnCount = Math.max(0, lastColumn - firstColumn + 1);
        int rowCount = Math.max(0, lastRow - firstRow + 1);
        cells = new CellHandle[columnCount * rowCount];
        distancesCells = new float[cells.length];
        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                int index = row * columnCount + column;
                cells[index] = waterLayer.obtainCellHandle(firstColumn + column, firstRow + row);
                distancesCells[index] = distanceCells(
                        firstColumn + column + 0.5f - splashColumn,
                        (firstRow + row + 0.5f - splashRow) * rowHeightPerColumnWidth);
            }
        }
    }

    private static void requireFinitePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0f) {
            throw new IllegalArgumentException(name + " must be finite and > 0: " + value);
        }
    }

    /** A cell is covered when its center, index + 0.5, is at or past the edge. */
    private static int clampedFirstCell(float edge) {
        return Math.max(0, (int) Math.ceil(edge - 0.5f));
    }

    private static int clampedLastCell(float edge, int gridSize) {
        return Math.min(gridSize - 1, (int) Math.floor(edge - 0.5f));
    }

    private static float distanceCells(float columnDelta, float rowDelta) {
        return (float) Math.sqrt(columnDelta * columnDelta + rowDelta * rowDelta);
    }

    public int handleCount() {
        return cells.length;
    }

    public void update(float timeSeconds) {
        if (!rings.isActiveAt(timeSeconds)) {
            settleIfLifted();
            return;
        }
        atRest = false;
        for (int index = 0; index < cells.length; index++) {
            cells[index].setScale(
                    1f + maxScaleBump * rings.liftAt(distancesCells[index], timeSeconds));
        }
    }

    private void settleIfLifted() {
        if (atRest) {
            return;
        }
        atRest = true;
        for (int index = 0; index < cells.length; index++) {
            cells[index].setScale(1f);
        }
    }
}
