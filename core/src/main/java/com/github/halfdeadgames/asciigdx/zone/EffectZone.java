package com.github.halfdeadgames.asciigdx.zone;

import com.github.halfdeadgames.asciigdx.grid.TerminalView;
import com.github.halfdeadgames.asciigdx.layer.CellHandle;
import com.github.halfdeadgames.asciigdx.layer.Layer;
import com.github.halfdeadgames.asciigdx.layer.LayerStack;
import com.github.halfdeadgames.asciigdx.shape.CellShape;
import com.github.halfdeadgames.asciigdx.shape.ShapeCellVisitor;
import java.util.Objects;

/**
 * Runs a {@link CellEffect} on every cell of a {@link CellShape} that lies inside a layer's grid.
 *
 * <p>On a map-window layer the shape is read in map coordinates, not grid coordinates, so the zone stays
 * fixed to the world as the camera moves.
 *
 * <p>The zone writes to frame handles ({@link Layer#obtainFrameCellHandle}). The game must call
 * {@link LayerStack#beginCellEffects} once per frame before applying zones; that call resets the frame
 * handles and releases those left unused for {@link Layer#getFrameReleaseSeconds()}. Without it, the last written values stay on screen. Frame handles
 * combine with the layer's lasting handles, so the zone never clears handles the game holds from
 * {@link Layer#obtainCellHandle}.
 *
 * <p>The zone holds no handles between calls: each {@link #apply} asks the layer for the handles it
 * needs. Applying allocates nothing itself; the layer only allocates when it creates a cell's frame
 * handle for the first time. The shape and the effect can be swapped between calls with
 * {@link #setShape} and {@link #setEffect}.
 *
 * <p>Not reentrant and not thread-safe: call it from the render thread, and never from inside an effect.
 *
 * <p>Example:
 * <pre>{@code
 * EffectZone zone = new EffectZone(shape, CellEffects.bounce(0.5f, 1f, Phase.sync()));
 * // each frame, on the render thread:
 * stack.beginCellEffects(delta);
 * zone.apply(layer, time);
 * }</pre>
 */
public final class EffectZone {

    private final ZoneVisitor visitor = new ZoneVisitor();
    private CellShape shape;
    private CellEffect effect;

    /**
     * Creates a zone.
     *
     * @param shape cells the effect runs on; not null
     * @param effect what to do to each cell; not null
     * @throws NullPointerException if {@code shape} or {@code effect} is null
     */
    public EffectZone(CellShape shape, CellEffect effect) {
        this.shape = Objects.requireNonNull(shape, "shape");
        this.effect = Objects.requireNonNull(effect, "effect");
    }

    /**
     * Returns the shape the effect runs on.
     *
     * @return the current shape; never null
     */
    public CellShape getShape() {
        return shape;
    }

    /**
     * Returns the effect run on each cell.
     *
     * @return the current effect; never null
     */
    public CellEffect getEffect() {
        return effect;
    }

    /**
     * Changes the cells the effect runs on. Takes effect on the next {@link #apply}.
     *
     * @param shape the new shape; not null
     * @throws NullPointerException if {@code shape} is null
     */
    public void setShape(CellShape shape) {
        this.shape = Objects.requireNonNull(shape, "shape");
    }

    /**
     * Changes what is done to each cell. Takes effect on the next {@link #apply}.
     *
     * @param effect the new effect; not null
     * @throws NullPointerException if {@code effect} is null
     */
    public void setEffect(CellEffect effect) {
        this.effect = Objects.requireNonNull(effect, "effect");
    }

    /**
     * Runs the effect once on every shape cell inside the layer's grid, on each cell's frame handle
     * ({@link Layer#obtainFrameCellHandle}). Cells outside the grid are skipped. On a
     * {@linkplain Layer#hasMapWindow() map-window layer} the shape is in map coordinates: it stays on
     * its map cells while the window scrolls, and only cells inside the window are affected.
     *
     * <p>Call {@link LayerStack#beginCellEffects} once per frame before the first zone is applied.
     * Frame handles have no pivot of their own, so an effect must not call
     * {@link CellHandle#setPivot} or {@link CellHandle#setPivotKeepingPlacement}; both throw on a frame
     * handle.
     *
     * @param layer layer whose cells are affected; not null
     * @param timeSeconds time handed to the effect, in seconds, e.g. seconds since the effect started
     * @throws IllegalStateException if called from inside an effect, if the layer was removed and the
     *     shape's bounds overlap its grid, or if the effect sets a pivot on the handle it is given
     */
    public void apply(Layer layer, float timeSeconds) {
        if (visitor.isRunning()) {
            throw new IllegalStateException("EffectZone.apply is not reentrant");
        }
        TerminalView grid = layer.getGrid();
        boolean mapWindow = layer.hasMapWindow();
        int originColumn = mapWindow ? layer.getMapWindow().getOriginColumn() : 0;
        int originRow = mapWindow ? layer.getMapWindow().getOriginRow() : 0;
        if (!overlaps(grid, originColumn, originRow)) {
            return;
        }
        visitor.start(layer, effect, timeSeconds, shape.getLeft(), shape.getTop(), originColumn, originRow);
        try {
            shape.forEachCell(visitor);
        } finally {
            visitor.finish();
        }
    }

    /** Tests the shape's bounds against the rectangle of shape coordinates the grid shows. */
    private boolean overlaps(TerminalView grid, int originColumn, int originRow) {
        return shape.getRight() >= originColumn && shape.getBottom() >= originRow
                && shape.getLeft() < (long) originColumn + grid.getColumns()
                && shape.getTop() < (long) originRow + grid.getRows();
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
        private int originColumn;
        private int originRow;

        boolean isRunning() {
            return layer != null;
        }

        /** The origin is the map cell at grid (0, 0), or 0 when the layer is not a map window. */
        void start(Layer targetLayer, CellEffect targetEffect, float time, int left,
                int top, int mapOriginColumn, int mapOriginRow) {
            layer = targetLayer;
            effect = targetEffect;
            timeSeconds = time;
            shapeLeft = left;
            shapeTop = top;
            originColumn = mapOriginColumn;
            originRow = mapOriginRow;
            gridWidth = targetLayer.getGrid().getColumns();
            gridHeight = targetLayer.getGrid().getRows();
            zoneCell.setCellSize(targetLayer.getGlyphSource().getCellWidth(),
                    targetLayer.getGlyphSource().getCellHeight());
        }

        void finish() {
            layer = null;
            effect = null;
        }

        @Override
        public void visit(int column, int row, float distance, float distanceFraction) {
            int gridColumn = column - originColumn;
            int gridRow = row - originRow;
            if (gridColumn < 0 || gridRow < 0 || gridColumn >= gridWidth || gridRow >= gridHeight) {
                return;
            }
            zoneCell.set(column, row, column - shapeLeft, row - shapeTop, distance, distanceFraction);
            effect.apply(layer.obtainFrameCellHandle(gridColumn, gridRow), zoneCell, timeSeconds);
        }
    }
}
