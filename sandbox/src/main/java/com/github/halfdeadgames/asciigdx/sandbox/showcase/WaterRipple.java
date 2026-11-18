package com.github.halfdeadgames.asciigdx.sandbox.showcase;

import com.github.halfdeadgames.asciigdx.layer.CellHandle;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.shape.RectangleShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeFill;
import com.github.halfdeadgames.asciigdx.zone.CellEffect;
import com.github.halfdeadgames.asciigdx.zone.EffectZone;
import com.github.halfdeadgames.asciigdx.zone.ZoneCell;

/**
 * Swells the water glyphs near a splash point as rings pass. The swell is written to frame handles
 * through an {@link EffectZone}, so the game must call {@code beginCellEffects} every frame before
 * {@link #update}. Only the scale is touched, so other effects can use the same frame handles for
 * alpha.
 */
public final class WaterRipple {

    private final RippleRings rings;
    private final EffectZone zone;
    private final Layer waterLayer;

    /**
     * Cell coordinates are in cell units from the grid's top-left corner, so the center of cell
     * (c, r) is (c + 0.5, r + 0.5). The zone covers the cells whose centers lie within the half
     * extents of the splash point.
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
        this.waterLayer = waterLayer;
        this.rings = rings;
        int firstColumn = clampedFirstCell(splashColumn - halfWidthCells);
        int lastColumn =
                clampedLastCell(splashColumn + halfWidthCells, waterLayer.getGrid().getColumns());
        int firstRow = clampedFirstCell(splashRow - halfHeightCells);
        int lastRow = clampedLastCell(splashRow + halfHeightCells, waterLayer.getGrid().getRows());
        int columnCount = lastColumn - firstColumn + 1;
        int rowCount = lastRow - firstRow + 1;
        if (columnCount < 1 || rowCount < 1) {
            zone = null;
            return;
        }
        RectangleShape coveredCells = RectangleShape.forCellAspect(
                firstColumn, firstRow, columnCount, rowCount, ShapeFill.FILLED,
                rowHeightPerColumnWidth);
        zone = new EffectZone(coveredCells, new RippleSwell(
                rings, maxScaleBump, splashColumn, splashRow, rowHeightPerColumnWidth));
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

    public void update(float timeSeconds) {
        if (zone == null || !rings.isActiveAt(timeSeconds)) {
            return;
        }
        zone.apply(waterLayer, timeSeconds);
    }

    private static final class RippleSwell implements CellEffect {

        private final RippleRings rings;
        private final float maxScaleBump;
        private final float splashColumn;
        private final float splashRow;
        private final float rowHeightPerColumnWidth;

        RippleSwell(RippleRings rings, float maxScaleBump, float splashColumn, float splashRow,
                float rowHeightPerColumnWidth) {
            this.rings = rings;
            this.maxScaleBump = maxScaleBump;
            this.splashColumn = splashColumn;
            this.splashRow = splashRow;
            this.rowHeightPerColumnWidth = rowHeightPerColumnWidth;
        }

        @Override
        public void apply(CellHandle cell, ZoneCell where, float timeSeconds) {
            float columnDelta = where.getColumn() + 0.5f - splashColumn;
            float rowDelta = (where.getRow() + 0.5f - splashRow) * rowHeightPerColumnWidth;
            float distanceCells =
                    (float) Math.sqrt(columnDelta * columnDelta + rowDelta * rowDelta);
            cell.scaleBy(1f + maxScaleBump * rings.liftAt(distanceCells, timeSeconds));
        }
    }
}
