package com.github.halfdeadgames.asciigdx.zone;

import com.github.halfdeadgames.asciigdx.grid.TerminalView;
import com.github.halfdeadgames.asciigdx.render.Layer;
import com.github.halfdeadgames.asciigdx.shape.CellShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeCellVisitor;
import java.util.Objects;

/**
 * Runs a {@link CellEffect} on every cell of a {@link CellShape} that lies inside a layer's grid.
 *
 * <p>The zone holds no cell handles between calls: each {@link #apply} asks the layer for the handles it
 * needs, so {@link Layer#releaseIdleCellHandles()} is always safe in between. Applying allocates nothing
 * itself; the layer only allocates when it creates a cell's handle for the first time. The shape and the
 * effect can be swapped between calls with {@link #setShape} and {@link #setEffect}.
 *
 * <p>Not reentrant and not thread-safe: call it from the render thread, and never from inside an effect.
 */
public final class EffectZone {

    private final ZoneVisitor visitor = new ZoneVisitor();
    private CellShape shape;
    private CellEffect effect;

    /**
     * Creates a zone.
     *
     * @param shape cells the effect runs on
     * @param effect what to do to each cell
     */
    public EffectZone(CellShape shape, CellEffect effect) {
        this.shape = Objects.requireNonNull(shape, "shape");
        this.effect = Objects.requireNonNull(effect, "effect");
    }

    /** Returns the shape the effect runs on. */
    public CellShape getShape() {
        return shape;
    }

    /** Returns the effect. */
    public CellEffect getEffect() {
        return effect;
    }

    /** Changes the cells the effect runs on. */
    public void setShape(CellShape shape) {
        this.shape = Objects.requireNonNull(shape, "shape");
    }

    /** Changes what is done to each cell. */
    public void setEffect(CellEffect effect) {
        this.effect = Objects.requireNonNull(effect, "effect");
    }

    /**
     * Runs the effect once on every shape cell inside the layer's grid. Cells outside the grid are skipped.
     * Call {@link Layer#resetCellHandles()} once per frame before the first zone is applied.
     *
     * @param layer layer whose cells are affected
     * @param timeSeconds time handed to the effect, e.g. seconds since the effect started
     * @throws IllegalStateException if called from inside an effect, or if the layer was removed and the
     *     shape's bounds overlap its grid
     */
    public void apply(Layer layer, float timeSeconds) {
        if (visitor.isRunning()) {
            throw new IllegalStateException("EffectZone.apply is not reentrant");
        }
        TerminalView grid = layer.getGrid();
        if (!overlaps(grid)) {
            return;
        }
        visitor.start(layer, effect, timeSeconds, shape.getLeft(), shape.getTop());
        try {
            shape.forEachCell(visitor);
        } finally {
            visitor.finish();
        }
    }

    private boolean overlaps(TerminalView grid) {
        return shape.getRight() >= 0 && shape.getBottom() >= 0
                && shape.getLeft() < grid.getWidth() && shape.getTop() < grid.getHeight();
    }

    private static final class ZoneVisitor implements ShapeCellVisitor {

        private final ZoneCell zoneCell = new ZoneCell();
        private Layer layer;
        private CellEffect effect;
        private float timeSeconds;
        private int shapeLeft;
        private int shapeTop;
        private int gridWidth;
        private int gridHeight;

        boolean isRunning() {
            return layer != null;
        }

        void start(Layer targetLayer, CellEffect targetEffect, float time, int left, int top) {
            layer = targetLayer;
            effect = targetEffect;
            timeSeconds = time;
            shapeLeft = left;
            shapeTop = top;
            gridWidth = targetLayer.getGrid().getWidth();
            gridHeight = targetLayer.getGrid().getHeight();
            zoneCell.setCellSize(targetLayer.getGlyphSource().getCellWidth(),
                    targetLayer.getGlyphSource().getCellHeight());
        }

        void finish() {
            layer = null;
            effect = null;
        }

        @Override
        public void visit(int column, int row, float distance, float distanceFraction) {
            if (column < 0 || row < 0 || column >= gridWidth || row >= gridHeight) {
                return;
            }
            zoneCell.set(column, row, column - shapeLeft, row - shapeTop, distance, distanceFraction);
            effect.apply(layer.cell(column, row), zoneCell, timeSeconds);
        }
    }
}
