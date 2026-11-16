package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.github.halfdeadgames.asciigdx.grid.TerminalGrid;

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
        if (!Float.isFinite(rowHeightPerColumnWidth) || rowHeightPerColumnWidth <= 0f) {
            throw new IllegalArgumentException(
                    "rowHeightPerColumnWidth must be finite and > 0: " + rowHeightPerColumnWidth);
        }
        this.dive = dive;
        this.waterGrid = waterGrid;
        this.glyphColor = glyphColor;
        this.backgroundColor = backgroundColor;
        int maxCells =
                boundingCellCount(UnderwaterDive.seeThroughRadiusCells(), rowHeightPerColumnWidth);
        columns = new int[maxCells];
        rows = new int[maxCells];
        distancesCells = new float[maxCells];
        collectCellsNear(splashColumn, splashRow, rowHeightPerColumnWidth);
    }

    private static int boundingCellCount(float radiusCells, float rowHeightPerColumnWidth) {
        int columnSpan = (int) Math.ceil(2f * radiusCells) + 2;
        int rowSpan = (int) Math.ceil(2f * radiusCells / rowHeightPerColumnWidth) + 2;
        return columnSpan * rowSpan;
    }

    private void collectCellsNear(float splashColumn, float splashRow,
            float rowHeightPerColumnWidth) {
        float radius = UnderwaterDive.seeThroughRadiusCells();
        int firstColumn = Math.max(0, (int) Math.floor(splashColumn - radius));
        int lastColumn =
                Math.min(waterGrid.getColumns() - 1, (int) Math.ceil(splashColumn + radius));
        int firstRow = Math.max(0, (int) Math.floor(splashRow - radius / rowHeightPerColumnWidth));
        int lastRow = Math.min(waterGrid.getRows() - 1,
                (int) Math.ceil(splashRow + radius / rowHeightPerColumnWidth));
        for (int row = firstRow; row <= lastRow; row++) {
            for (int column = firstColumn; column <= lastColumn; column++) {
                float columnDelta = column + 0.5f - splashColumn;
                float rowDelta = (row + 0.5f - splashRow) * rowHeightPerColumnWidth;
                float distance = (float) Math.sqrt(columnDelta * columnDelta + rowDelta * rowDelta);
                if (distance < radius) {
                    addCell(column, row, distance);
                }
            }
        }
    }

    private void addCell(int column, int row, float distanceCells) {
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
