package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;
import com.github.halfdeadgames.asciigdx.shape.CellShape;
import com.github.halfdeadgames.asciigdx.shape.CircleShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeFill;

/**
 * Makes the water cells around the splash point translucent during the dive, then writes them back
 * opaque.
 */
public final class SeeThroughWater {

    private static final int OPAQUE_MASK = 0xFFFFFF00;
    private static final float MAX_ALPHA_BYTE = 255f;

    private final UnderwaterDive dive;
    private final TerminalGrid waterGrid;
    private final int glyphColor;
    private final int backgroundColor;
    private final int[] columns;
    private final int[] rows;
    private final float[] distancesCells;
    private int cellCount;
    private boolean cellsTranslucent;

    /**
     * Coordinates are in cell units from the grid's top-left corner, so the center of cell (c, r)
     * is (c + 0.5, r + 0.5).
     *
     * @param rowHeightPerColumnWidth cell height divided by cell width, so the area looks round on
     *        screen
     * @param glyphColor water glyph color; its alpha byte is replaced
     * @param backgroundColor water background color; its alpha byte is replaced
     */
    public SeeThroughWater(UnderwaterDive dive, TerminalGrid waterGrid, float splashColumn,
            float splashRow,
            float rowHeightPerColumnWidth, int glyphColor, int backgroundColor) {
        if (dive == null || waterGrid == null) {
            throw new IllegalArgumentException("dive and waterGrid must not be null");
        }
        this.dive = dive;
        this.waterGrid = waterGrid;
        this.glyphColor = glyphColor;
        this.backgroundColor = backgroundColor;
        CircleShape seeThroughArea = new CircleShape(splashColumn, splashRow,
                UnderwaterDive.seeThroughRadiusCells(), ShapeFill.FILLED,
                rowHeightPerColumnWidth);
        int maxCells = boundingCellCount(seeThroughArea);
        columns = new int[maxCells];
        rows = new int[maxCells];
        distancesCells = new float[maxCells];
        seeThroughArea.forEachCell(this::addCellIfOnGrid);
    }

    private static int boundingCellCount(CellShape shape) {
        int columnSpan = shape.getRight() - shape.getLeft() + 1;
        int rowSpan = shape.getBottom() - shape.getTop() + 1;
        return columnSpan * rowSpan;
    }

    private void addCellIfOnGrid(int column, int row, float distanceCells,
            float unusedDistanceFraction) {
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

    /** Call after the full water rewrite each frame, so the translucent cells win over it. */
    public void update(float timeSeconds) {
        boolean seeThrough = dive.seeThroughAt(timeSeconds) > 0f;
        if (!seeThrough && !cellsTranslucent) {
            return;
        }
        cellsTranslucent = seeThrough;
        writeCells(timeSeconds);
    }

    private void writeCells(float timeSeconds) {
        for (int index = 0; index < cellCount; index++) {
            int alphaByte = Math.round(
                    MAX_ALPHA_BYTE * dive.waterAlphaAt(distancesCells[index], timeSeconds));
            waterGrid.set(
                    columns[index], rows[index],
                    WaterPattern.codePointAt(columns[index], rows[index], timeSeconds),
                    (glyphColor & OPAQUE_MASK) | alphaByte,
                    (backgroundColor & OPAQUE_MASK) | alphaByte);
        }
    }
}
