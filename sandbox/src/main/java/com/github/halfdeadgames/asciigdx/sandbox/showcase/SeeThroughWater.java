package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.github.halfdeadgames.asciigdx.grid.TerminalView;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.shape.CellShape;
import com.github.halfdeadgames.asciigdx.shape.CircleShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeFill;

/**
 * Fades the water cells around the splash point during the dive with frame cell handles, so each
 * cell's glyph and background fade together as one color. The water grid is never written; the
 * frame handles go back to opaque when the stack's cell effects begin the next frame.
 */
public final class SeeThroughWater {

    private final UnderwaterDive dive;
    private final Layer waterLayer;
    private final int[] columns;
    private final int[] rows;
    private final float[] distancesCells;
    private int cellCount;

    /**
     * Coordinates are in cell units from the grid's top-left corner, so the center of cell (c, r)
     * is (c + 0.5, r + 0.5).
     *
     * @param rowHeightPerColumnWidth cell height divided by cell width, so the area looks round on
     *        screen
     */
    public SeeThroughWater(UnderwaterDive dive, Layer waterLayer, float splashColumn,
            float splashRow, float rowHeightPerColumnWidth) {
        if (dive == null || waterLayer == null) {
            throw new IllegalArgumentException("dive and waterLayer must not be null");
        }
        this.dive = dive;
        this.waterLayer = waterLayer;
        CircleShape seeThroughArea = new CircleShape(splashColumn, splashRow,
                UnderwaterDive.seeThroughRadiusCells(), ShapeFill.FILLED,
                rowHeightPerColumnWidth);
        int maxCells = boundingCellCount(seeThroughArea);
        columns = new int[maxCells];
        rows = new int[maxCells];
        distancesCells = new float[maxCells];
        TerminalView waterGrid = waterLayer.getGrid();
        seeThroughArea.forEachCell((column, row, distanceCells, unusedDistanceFraction) ->
                addCellIfOnGrid(waterGrid, column, row, distanceCells));
    }

    private static int boundingCellCount(CellShape shape) {
        int columnSpan = shape.getRight() - shape.getLeft() + 1;
        int rowSpan = shape.getBottom() - shape.getTop() + 1;
        return columnSpan * rowSpan;
    }

    private void addCellIfOnGrid(TerminalView waterGrid, int column, int row,
            float distanceCells) {
        if (!waterGrid.contains(column, row)) {
            return;
        }
        columns[cellCount] = column;
        rows[cellCount] = row;
        distancesCells[cellCount] = distanceCells;
        cellCount++;
    }

    public int cellCount() {
        return cellCount;
    }

    /** Call every frame after the stack's cell effects begin. */
    public void update(float timeSeconds) {
        if (!(dive.seeThroughAt(timeSeconds) > 0f)) {
            return;
        }
        for (int index = 0; index < cellCount; index++) {
            waterLayer.obtainFrameCellHandle(columns[index], rows[index])
                    .setCellAlpha(dive.waterAlphaAt(distancesCells[index], timeSeconds));
        }
    }
}
