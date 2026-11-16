package com.github.halfdeadgames.asciigdx.layer;

import com.badlogic.gdx.utils.Array;
import com.github.halfdeadgames.asciigdx.grid.TerminalView;

/**
 * The cell handles of one layer: the lasting handles, the frame handles, and the draw list the
 * renderer reads. It keeps the three in step and tells the renderer about changes.
 *
 * <p>Each cell with a handle has exactly one entry in the draw list, in the order the cell got its
 * first handle. A cell with only a lasting handle is drawn through that handle itself, so nothing
 * is copied and the output matches a cell that never had a frame handle. As soon as the cell also
 * has a frame handle, the entry becomes a hidden handle that is never given out; before each draw
 * the two handles are folded into it by {@link CellHandleCombine}. The entry goes back to the
 * lasting handle when the frame handle is released, and is dropped when no handle is left.
 *
 * <p>The renderer only learns of appended entries by itself; every replaced or removed entry is
 * followed by a rebind, so the renderer rebuilds its index.
 */
final class CellHandleSet {

    private final Layer layer;
    private final TerminalView grid;
    private final LayerRenderer renderer;
    private final IndexedHandles lasting = new IndexedHandles();
    private final IndexedHandles frame = new IndexedHandles();
    private final IndexedHandles drawn = new IndexedHandles();
    private boolean drawListChanged;
    private int syncedColumns;
    private int syncedRows;

    CellHandleSet(Layer layer, TerminalView grid, LayerRenderer renderer) {
        this.layer = layer;
        this.grid = grid;
        this.renderer = renderer;
        syncedColumns = grid.getColumns();
        syncedRows = grid.getRows();
    }

    Array<CellHandle> frameHandles() {
        return frame.list();
    }

    /** The list the renderer draws: one entry per cell that has a handle. */
    Array<CellHandle> drawnHandles() {
        return drawn.list();
    }

    int lastingCount() {
        return lasting.size();
    }

    int frameCount() {
        return frame.size();
    }

    /** Returns the lasting handle of a cell, creating it first if the cell has none. */
    CellHandle getOrCreateLasting(int column, int row) {
        CellHandle existing = lasting.at(column, row);
        if (existing != null) {
            return existing;
        }
        CellHandle created = new CellHandle(layer, column, row);
        lasting.add(created);
        if (frame.at(column, row) == null) {
            appendDrawn(created);
        }
        return created;
    }

    /** Returns the frame handle of a cell, creating it first if the cell has none. */
    CellHandle getOrCreateFrame(int column, int row) {
        CellHandle existing = frame.at(column, row);
        if (existing != null) {
            return existing;
        }
        CellHandle created = new CellHandle(layer, column, row, true);
        frame.add(created);
        CellHandle lastingHandle = lasting.at(column, row);
        if (lastingHandle == null) {
            appendDrawn(new CellHandle(layer, column, row));
            return created;
        }
        drawn.replace(lastingHandle, combinedEntryStartingFrom(lastingHandle));
        drawListChanged = true;
        rebindRendererIfChanged();
        return created;
    }

    /**
     * A hidden entry that holds what the renderer shows for the lasting handle now, so the first
     * combine only rewrites the cell if the frame handle changes something.
     */
    private CellHandle combinedEntryStartingFrom(CellHandle lastingHandle) {
        CellHandle entry = new CellHandle(layer, lastingHandle.getColumn(), lastingHandle.getRow());
        CellHandleCombine.combine(entry, lastingHandle, CellHandleCombine.NO_EFFECT);
        entry.setGlyphHidden(lastingHandle.isGlyphHidden());
        if (lastingHandle.areOverridesChanged()) {
            entry.markOverridesChanged();
        } else {
            entry.clearOverridesChanged();
        }
        return entry;
    }

    private void appendDrawn(CellHandle entry) {
        boolean wasEmpty = drawn.size() == 0;
        drawn.add(entry);
        if (wasEmpty) {
            rebindRenderer();
        }
    }

    /** Resets every lasting handle; frame handles are not touched. */
    void resetLasting() {
        Array<CellHandle> handles = lasting.list();
        for (int i = 0; i < handles.size; i++) {
            handles.get(i).reset();
        }
    }

    /**
     * Releases every idle lasting handle, then rebinds the renderer once if the draw list changed.
     */
    void releaseIdleLasting() {
        for (int i = lasting.size() - 1; i >= 0; i--) {
            if (lasting.get(i).isIdle()) {
                releaseLastingAt(i);
            }
        }
        rebindRendererIfChanged();
    }

    /**
     * Settles, removes, and detaches one lasting handle. The caller must then call
     * {@link #rebindRendererIfChanged()}. A frame handle on the same cell keeps the cell drawn.
     */
    void releaseLastingAt(int index) {
        CellHandle released = lasting.get(index);
        lasting.removeAt(index);
        if (frame.at(released.getColumn(), released.getRow()) == null) {
            dropDrawnEntry(released);
        }
        released.detach();
    }

    /** Removes and detaches one frame handle; see {@link #releaseLastingAt}. */
    void releaseFrameAt(int index) {
        CellHandle released = frame.get(index);
        frame.removeAt(index);
        CellHandle entry = drawn.at(released.getColumn(), released.getRow());
        CellHandle lastingHandle = lasting.at(released.getColumn(), released.getRow());
        if (lastingHandle == null) {
            dropDrawnEntry(entry);
        } else {
            handEntryBackTo(lastingHandle, entry);
        }
        entry.detach();
        released.detach();
    }

    private void dropDrawnEntry(CellHandle entry) {
        // Detached first: the renderer still indexes this entry, and a rewrite must not use its
        // overrides.
        entry.detach();
        settleRenderer(entry);
        drawn.remove(entry);
        drawListChanged = true;
    }

    private void handEntryBackTo(CellHandle lastingHandle, CellHandle entry) {
        lastingHandle.setGlyphHidden(entry.isGlyphHidden());
        if (entry.areOverridesChanged() || entry.hasOverrides()) {
            lastingHandle.markOverridesChanged();
        }
        drawn.replace(entry, lastingHandle);
        drawListChanged = true;
    }

    /**
     * Sends the renderer any change this entry has not synced yet, since a dropped entry gets no
     * later sync: it shows the glyph again and rewrites the cell without the entry's overrides.
     */
    private void settleRenderer(CellHandle entry) {
        if (isOutsideGrid(entry)) {
            // The grid shrank since the last draw; the renderer's rebuild already forgets this
            // cell.
            return;
        }
        if (entry.isGlyphHidden()) {
            renderer.setCellGlyphHidden(entry.getColumn(), entry.getRow(), false);
            entry.setGlyphHidden(false);
        }
        if (entry.areOverridesChanged() || entry.hasOverrides()) {
            renderer.refreshCell(entry.getColumn(), entry.getRow());
            entry.clearOverridesChanged();
        }
    }

    private boolean isOutsideGrid(CellHandle cell) {
        return cell.getColumn() >= grid.getColumns() || cell.getRow() >= grid.getRows();
    }

    void rebindRendererIfChanged() {
        if (drawListChanged) {
            rebindRenderer();
        }
    }

    private void rebindRenderer() {
        renderer.setCellHandles(drawn.list());
        drawListChanged = false;
    }

    /**
     * Brings the renderer in line with the handles before drawing: drops handles whose cell left
     * the grid, folds frame handles into their entries, then hides or shows the glyph of each entry
     * whose lifted state changed and rewrites the cells whose overrides changed.
     */
    void syncBeforeDraw() {
        dropHandlesOutsideResizedGrid();
        if (drawn.size() == 0) {
            return;
        }
        combineFrameHandles();
        applyLiftedFlags();
        refreshChangedOverrides();
    }

    /*
     * A handle is only dropped when its cell left the grid, so there is never a surviving cell to
     * rewrite; the renderer's rebuild on the size change drops it from its index.
     */
    private void dropHandlesOutsideResizedGrid() {
        if (grid.getColumns() == syncedColumns && grid.getRows() == syncedRows) {
            return;
        }
        syncedColumns = grid.getColumns();
        syncedRows = grid.getRows();
        dropOutsideGridAndReindex();
        forgetHiddenFlags();
    }

    private void dropOutsideGridAndReindex() {
        dropOutsideGridAndReindex(lasting);
        dropOutsideGridAndReindex(frame);
        dropOutsideGridAndReindex(drawn);
    }

    private void dropOutsideGridAndReindex(IndexedHandles handles) {
        handles.dropOutside(grid.getColumns(), grid.getRows());
        handles.rebuildIndex();
    }

    private void combineFrameHandles() {
        for (int i = 0; i < frame.size(); i++) {
            CellHandle frameHandle = frame.get(i);
            CellHandle entry = drawn.at(frameHandle.getColumn(), frameHandle.getRow());
            CellHandle lastingHandle = lasting.at(frameHandle.getColumn(), frameHandle.getRow());
            CellHandleCombine.combine(entry,
                    lastingHandle == null ? CellHandleCombine.NO_EFFECT : lastingHandle,
                    frameHandle);
        }
    }

    /** The renderer clears its hidden flags when it rebuilds, so every entry starts shown again. */
    void forgetHiddenFlags() {
        Array<CellHandle> entries = drawn.list();
        for (int i = 0; i < entries.size; i++) {
            entries.get(i).setGlyphHidden(false);
        }
    }

    private void applyLiftedFlags() {
        Array<CellHandle> entries = drawn.list();
        for (int i = 0; i < entries.size; i++) {
            CellHandle entry = entries.get(i);
            boolean lifted = entry.isLifted();
            if (lifted == entry.isGlyphHidden()) {
                continue;
            }
            renderer.setCellGlyphHidden(entry.getColumn(), entry.getRow(), lifted);
            entry.setGlyphHidden(lifted);
        }
    }

    private void refreshChangedOverrides() {
        Array<CellHandle> entries = drawn.list();
        for (int i = 0; i < entries.size; i++) {
            CellHandle entry = entries.get(i);
            if (!entry.areOverridesChanged()) {
                continue;
            }
            renderer.refreshCell(entry.getColumn(), entry.getRow());
            entry.clearOverridesChanged();
        }
    }

    /**
     * Clears hidden flags before a scroll, so none is left on a position that will hold other
     * content.
     */
    void showGlyphsAtOldPositions() {
        Array<CellHandle> entries = drawn.list();
        for (int i = 0; i < entries.size; i++) {
            showGlyphAtOldPosition(entries.get(i));
        }
    }

    private void showGlyphAtOldPosition(CellHandle entry) {
        if (!entry.isGlyphHidden()) {
            return;
        }
        if (!isInsideGrid(entry)) {
            // The grid shrank since the last draw; the renderer's rebuild already forgets this
            // flag.
            entry.setGlyphHidden(false);
            return;
        }
        renderer.setCellGlyphHidden(entry.getColumn(), entry.getRow(), false);
        entry.setGlyphHidden(false);
    }

    private boolean isInsideGrid(CellHandle cell) {
        return Guards.isCellInside(cell.getColumn(), cell.getRow(), grid.getColumns(),
                grid.getRows());
    }

    /**
     * Moves every handle with the scrolled content, drops those pushed out of the grid, and rebinds
     * the renderer. Call after the grid content moved.
     */
    void moveWithContent(int columns, int rows) {
        moveHiddenEntries(columns, rows);
        moveAll(lasting.list(), columns, rows);
        moveAll(frame.list(), columns, rows);
        dropOutsideGridAndReindex();
        rebindRenderer();
    }

    /**
     * Entries that are not a lasting handle are the hidden ones. They are found through the lasting
     * index, so this must run before the lasting handles move.
     */
    private void moveHiddenEntries(int columns, int rows) {
        Array<CellHandle> entries = drawn.list();
        for (int i = 0; i < entries.size; i++) {
            CellHandle entry = entries.get(i);
            if (lasting.at(entry.getColumn(), entry.getRow()) != entry) {
                entry.moveBy(columns, rows);
            }
        }
    }

    private static void moveAll(Array<CellHandle> handles, int columns, int rows) {
        for (int i = 0; i < handles.size; i++) {
            handles.get(i).moveBy(columns, rows);
        }
    }

    /** Detaches every handle, as when the layer is removed. */
    void detachAll() {
        lasting.detachAll();
        frame.detachAll();
        drawn.detachAll();
    }
}
